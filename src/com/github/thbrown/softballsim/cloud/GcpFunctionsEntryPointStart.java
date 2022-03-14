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
import com.github.thbrown.softballsim.SoftballSim;
import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.datasource.gcpbuckets.DataSourceGcpBuckets;
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
 * Flags and data are supplied via the post body. Results (final and intermediate) are written to a
 * cloud storage bucket. This function will create compute instances to continue the work if this
 * function is unable to complete the job within it's configured timeout.
 */
public class GcpFunctionsEntryPointStart implements HttpFunction {

  // Allow the function to run for TIMEOUT_IN_MILLIS before transitioning to
  // preemptible compute instances
  private static final int TIMEOUT_IN_MILLIS = 15 * 1000;// 500 * 1000;
  private static final String ZONES =
      "us-central1-a,us-central1-b,us-central1-c,us-central1-f";

  private static final String DATA_KEY = "data";
  private static final String ID_KEY = "-" + DataSourceGcpBuckets.ID;
  private static final String DATA_SOURCE_KEY = "-" + CommandLineOptions.DATA_SOURCE;
  private static final String ESTIMATE_ONLY_KEY = "-" + CommandLineOptions.ESTIMATE_ONLY;
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
          Thread.sleep(3000); // Delay to prevent excessive guessing
        } catch (InterruptedException e) {
        }
        return new RuntimeException("Missing Password");
      });

      String pwdHash = StringUtils.calculateSha256AsHex(pwd.trim());
      if (!pwdHash.equals(PASSWORD_HASH)) {
        throw new RuntimeException("Invalid Password");
      }
      map.remove(PASSWORD_KEY);

      Logger.log("Map " + map);

      // Error checking
      String id = map.get(ID_KEY);
      if (id == null) {
        new RuntimeException("Required json field " + ID_KEY + " (id) was not specified in the request body");
      }
      int length = id.length();
      if (length < 15 || length > 63) {
        new RuntimeException(
            "Please provide an Id (-I) that is longer than 15 characters and shorter than 64 characters. Was " + length
                + " characters");
      }

      String data = map.get(DATA_KEY);
      map.remove(DATA_KEY); // We don't want to pass the stats data as an arg to the optimizer program
      if (data == null) {
        throw new RuntimeException("Required json field " + DATA_KEY + " was not specified in the request body");
      }

      // Write the stats data to a cloud storage bucket (application will look for it
      // there)
      Logger.log(id + " uploading stats");
      CloudUtils.upsertBlob(data, id, DataSourceGcpBuckets.STATS_DATA_BUCKET);

      // Delete flags (un-pause if paused)
      CloudUtils.deleteBlob(id, DataSourceGcpBuckets.CONTROL_FLAGS_BUCKET);

      // Set the data source appropriately (GCP_BUCKETS)
      map.put(DATA_SOURCE_KEY, DataSourceEnum.GCP_BUCKETS.name());

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

      // Establish an application level timeout (before function infrastructure
      // timeout) So we have time to transition to GCP compute to finish the job, if
      // it hasn't completed by the time the timeout is hit
      new Thread(() -> {
        try {
          Thread.sleep(TIMEOUT_IN_MILLIS);

          // We don't want the optimization starting from scratch on compute engine
          MapWrapper shallowCopy = new MapWrapper(map);
          shallowCopy.remove("-" + CommandLineOptions.FORCE);

          // Get flags/values as string so we can pass it to the compute function
          String stringArguments = argMapToShellString(shallowCopy);

          // If this is an estimate only, we couldn't get an estimate in time, return null
          if (map.get(ESTIMATE_ONLY_KEY) != null && !map.get(ESTIMATE_ONLY_KEY).equalsIgnoreCase("false")) {
            Logger.log(id + " estimate could not be determined");
            CloudUtils.send400Error(response, "application timeout too long for estimate to complete");
            return;
          }

          // Start the job on a compute instance
          JsonObject jsonObject = new JsonObject();
          jsonObject.add(GcpFunctionsEntryPointCompute.ARGS_KEY, new JsonPrimitive(stringArguments));
          jsonObject.add(GcpFunctionsEntryPointCompute.ID_KEY, new JsonPrimitive(map.get(ID_KEY)));
          jsonObject.add(GcpFunctionsEntryPointCompute.ZONES_KEY, new JsonPrimitive(ZONES));
          jsonObject.add(PASSWORD_KEY, new JsonPrimitive(pwd));
          String jsonPayload = gson.toJson(jsonObject);

          // TODO: can't we just invoke this directly?
          Logger.log(id + " timeout exceeded, sending compute request " + jsonPayload);
          int status = this.sendPost(COMPUTE_FUNCTION_ENDPOINT, jsonPayload);
          Logger.log(id + " compute request status: " + status);

          // Send success message as json
          String jsonPayloadTwo = CloudUtils.getResponseJson("SUCCESS", "job transferred to compute");
          response.setContentType("application/json");
          response.setStatusCode(200);
          response.getOutputStream().write(jsonPayloadTwo.getBytes());
          response.getOutputStream().flush();
        } catch (Exception e) {
          Logger.log(id + " forcing function exit 3 " + e.toString());
          try {
            CloudUtils.send400Error(response, e.toString());
          } catch (IOException e1) {
            Logger.log(id + " problem sending 400 " + e1);
          }

          // Log stack
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          Logger.log(sw.toString());

          // Exit the whole program, not just this thread
          System.exit(1);
        } finally {
          // End this function's execution
          Logger.log(id + " forcing function exit");
          System.exit(0);
        }
      }).start();

      // Run optimization with those command line arguments
      Logger.log(id + " arguments: " + args.toString());
      Result result = SoftballSim.mainInternal(args.toArray(new String[args.size()]));
      Logger.log(id + " run complete " + result);

      // Return the result
      Logger.log(id + " returning");
      response.setContentType("application/json");
      response.setStatusCode(200);
      response.getOutputStream().write(gson.toJson(result).getBytes());
      response.getOutputStream().flush();
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
      System.exit(0);
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

  public static String argMapToShellString(MapWrapper s) {
    StringBuilder builder = new StringBuilder();
    // TODO: Names can't contains commas
    for (String key : s.keySet()) {
      String value = s.get(key);
      if (value.equalsIgnoreCase("true")) {
        // Boolean flag set to true, just add the flag
        builder.append(key + " ");
      } else if (value.equalsIgnoreCase("false")) {
        // Boolean flag set to false, don't add any args
      } else {
        // Flag w/ a value, add both flag and value to args
        builder.append(key + " ");
        builder.append("\"" + BashEscape.SHELL_ESCAPE.escape(s.get(key)) + "\" ");
      }
    }
    return builder.toString();
  }

}
