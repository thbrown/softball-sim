package com.github.thbrown.softballsim.cloud;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.SoftballSim;
import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.datasource.gcpbuckets.DataSourceGcpBuckets;
import com.github.thbrown.softballsim.optimizer.EmptyResult;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * This class enables the ability for optimizers to be run via a GCP function.
 * 
 * Flags and data are supplied via the post body. Estimations are run
 * synchronously, normal runs are
 * run async and Results (final and intermediate) are written to a cloud storage
 * bucket.
 */
public class GcpFunctionsEntryPointStart implements HttpFunction {

  private static final String ZONES = "us-central1-a,us-central1-b,us-central1-c,us-central1-f";

  private static final String DATA_KEY = "data";
  private static final String NAME_KEY = "-" + DataSourceGcpBuckets.NAME;
  private static final String DATA_SOURCE_KEY = "-" + CommandLineOptions.DATA_SOURCE;
  private static final String ESTIMATE_ONLY_KEY = "-" + CommandLineOptions.ESTIMATE_ONLY;
  private static final String OPTIMIZER_KEY = "-" + CommandLineOptions.OPTIMIZER;
  public static final String PASSWORD_KEY = "PASSWORD";

  @Override
  public void service(HttpRequest request, HttpResponse response) throws Exception {

    try {
      // Setup required variables
      Gson gson = GsonAccessor.getInstance().getCustom();
      String COMPUTE_FUNCTION_ENDPOINT = Optional.ofNullable(System.getenv("COMPUTE_FUNCTION_ENDPOINT"))
          .orElseThrow(() -> new RuntimeException("COMPUTE_FUNCTION_ENDPOINT is not set in the environment"));
      final String PASSWORD_HASH = Optional.ofNullable(System.getenv("PASSWORD_HASH"))
          .orElseThrow(() -> new RuntimeException("PASSWORD_HASH is not set in the environment"));

      // Extract post body into a map for consumption
      byte[] jsonBodyBytes = Optional.ofNullable(request.getInputStream().readAllBytes())
          .orElseThrow(() -> new RuntimeException("Missing POST request body"));
      String jsonBody = new String(jsonBodyBytes, StandardCharsets.UTF_8);
      if (StringUtils.isBlank(jsonBody)) {
        throw new RuntimeException("Missing POST request body");
      }
      MapWrapper map = gson.fromJson(jsonBody, MapWrapper.class);

      // Password checking
      String pwd = Optional.ofNullable(map.get(PASSWORD_KEY)).orElseThrow(() -> {
        try {
          Thread.sleep(500); // Delay to prevent excessive guessing
        } catch (InterruptedException e) {
        }
        return new RuntimeException("Missing Password");
      });

      String pwdHash = StringUtils.calculateSha256AsHex(pwd.trim());
      if (!pwdHash.equals(PASSWORD_HASH)) {
        throw new RuntimeException("Invalid Password");
      }
      map.remove(PASSWORD_KEY);

      // Error checking
      String name = map.get(NAME_KEY);
      if (name == null) {
        new RuntimeException("Required json field " + NAME_KEY + " (name) was not specified in the request body");
      }
      int length = name.length();
      if (length < 15 || length > 63) {
        new RuntimeException(
            "Please provide an Name (-n) that is longer than 15 characters and shorter than 64 characters. Was "
                + length
                + " characters");
      }

      String data = map.get(DATA_KEY);
      map.remove(DATA_KEY); // We don't want to pass the stats data as an arg to the optimizer program
      if (data == null) {
        throw new RuntimeException("Required json field " + DATA_KEY + " was not specified in the request body");
      }

      // Write the stats data to a cloud storage bucket (application will look for it
      // there)
      Logger.log(name + " uploading stats");
      CloudUtils.upsertBlob(data, name, DataSourceGcpBuckets.STATS_DATA_BUCKET);

      // Delete flags (un-pause if paused)
      CloudUtils.deleteBlob(name, DataSourceGcpBuckets.CONTROL_FLAGS_BUCKET);

      // Set the data source appropriately (GCP_BUCKETS)
      map.put(DATA_SOURCE_KEY, DataSourceEnum.GCP_BUCKETS.name());

      // Logger.log("Map " + map);

      // Add all flags and values from the request to the command arguments
      List<String> args = new ArrayList<>();
      for (String flag : map.keySet()) {
        String value = map.get(flag);
        if (value.equalsIgnoreCase("true")) {
          // Boolean flag set to true, just add the flag
          args.add(flag);
        } else if (value.equalsIgnoreCase("false")) {
          // Boolean flag set to false, don't add any args
        } else {
          // Flag w/ a value, add both flag and value to args
          args.add(flag);
          args.add(value);
        }
      }

      // We don't want the optimization starting from scratch on compute engine
      MapWrapper shallowCopy = new MapWrapper(map);
      shallowCopy.remove("-" + CommandLineOptions.FORCE);

      // If this is an estimate only, run the optimization synchronously
      if (map.get(ESTIMATE_ONLY_KEY) != null && !map.get(ESTIMATE_ONLY_KEY).equalsIgnoreCase("false")) {
        // Run optimization with those command line arguments
        Logger.log(name + " arguments: " + args.toString());
        Result result = SoftballSim.mainInternal(args.toArray(new String[args.size()]));
        Logger.log(name + " run complete " + result);

        // Return the result
        Logger.log(name + " returning");
        response.setContentType("application/json");
        response.setStatusCode(200);
        response.getOutputStream().write(gson.toJson(result).getBytes());
        response.getOutputStream().flush();
        return;
      }

      // The results file serves as a lock to prevents multiple instances from running
      // the same
      // optimization
      // TODO: There is a race condition here between the read and the write, we can
      // solve that with gcp
      // "generation" and "pre-conditions"
      String resultString = CloudUtils.readBlob(name, DataSourceGcpBuckets.CACHED_RESULTS_BUCKET);
      Logger.log("Result blob is " + resultString);
      Result result = GsonAccessor.getInstance().getCustom().fromJson(resultString, Result.class);
      // Change the status to IN_PROGRESS if it's not already
      if (result == null) {
        // This happens if the optimization has never been started before
        Logger.log("No result, persisting empty result");
        Result emptyResult = new EmptyResult(OptimizerEnum.getEnumFromId(map.get(OPTIMIZER_KEY)),
            ResultStatusEnum.ALLOCATING_RESOURCES);
        String emptyResultString = GsonAccessor.getInstance().getCustom().toJson(emptyResult);
        CloudUtils.upsertBlob(emptyResultString, name, DataSourceGcpBuckets.CACHED_RESULTS_BUCKET);
      } else if (!result.getStatus().isActive()) {
        // This happens if the opt was paused or it error'ed out
        Logger.log("Existing result, changing status from " + result.getStatus() + " to ALLOCATING_RESOURCES");
        String updatedResultString = Result.copyWithNewStatusStringOnly(resultString,
            ResultStatusEnum.ALLOCATING_RESOURCES, null);
        CloudUtils.upsertBlob(updatedResultString, name, DataSourceGcpBuckets.CACHED_RESULTS_BUCKET);
      } else {
        // Don't start the optimization if it is already active based on its status
        // (IN_PROGRESS or
        // ALLOCATING_RESOURCES)
        CloudUtils.send200Warning(response, "This optimization is already active. Status: " + result.getStatus());
        return;
      }

      // Start the job on a compute instance
      JsonObject jsonObject = new JsonObject();
      jsonObject.add(GcpFunctionsEntryPointCompute.NAME_KEY, new JsonPrimitive(map.get(NAME_KEY)));
      jsonObject.add(GcpFunctionsEntryPointCompute.ZONES_KEY, new JsonPrimitive(ZONES));
      jsonObject.add(PASSWORD_KEY, new JsonPrimitive(pwd));
      // The remaining properties are params that should be passed right to the
      // application, there is no risk of key conflicts because all input args begin
      // with a "-"
      for (String key : shallowCopy.keySet()) {
        jsonObject.add(key, new JsonPrimitive(shallowCopy.get(key)));
      }

      String jsonPayload = gson.toJson(jsonObject);

      // TODO: can't we just invoke this directly via some google cloud api? That
      // might simplify
      Logger.log(name + " sending compute request " + jsonPayload);
      int status = this.sendPost(COMPUTE_FUNCTION_ENDPOINT, jsonPayload);
      Logger.log(name + " compute request status: " + status);

      // Send success message as json
      CloudUtils.send200Success(response, "job transferred to compute");
    } catch (Exception e) {
      // Log stack
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      Logger.log("Exception encountered during processing " + e.toString());
      Logger.log(sw.toString());

      // Send exceptions
      CloudUtils.send400Error(response, e.toString());
    } finally {
      // End the function execution, no need to wait for our async timeout
      // this also kills any optimizer threads that may still be running
      // Thread.sleep(2000); // Prevent truncated response body
      // System.exit(0);
    }
  }

  private int sendPost(String url, String body) {
    try {
      HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
      com.google.api.client.http.HttpRequest request;

      request = requestFactory.buildPostRequest(new GenericUrl(url),
          ByteArrayContent.fromString("application/json", body));

      request.getHeaders().setContentType("application/json");
      com.google.api.client.http.HttpResponse response = request.execute();
      return response.getStatusCode();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
