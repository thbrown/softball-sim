package com.github.thbrown.softballsim.cloud;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.datasource.gcpbuckets.DataSourceGcpBuckets;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;

/**
 * A GCP function that gets an intermediate result for an optimization run.
 * 
 * Intermediate result are stored in a cloud bucket on an interval while the *Start function is
 * running.
 */
public class GcpFunctionsEntryPointQuery implements HttpFunction {

  @Override
  public void service(HttpRequest request, HttpResponse response) throws Exception {
    try {
      // Setup
      Gson gson = GsonAccessor.getInstance().getCustom();
      final String PASSWORD_HASH = Optional.ofNullable(System.getenv("PASSWORD_HASH"))
          .orElseThrow(() -> new RuntimeException("PASSWORD_HASH is not set in the environment"));

      // Extract body
      byte[] jsonBodyBytes = request.getInputStream().readAllBytes();
      String jsonBody = new String(jsonBodyBytes, StandardCharsets.UTF_8);

      // Parse JSON to map
      MapWrapper map = gson.fromJson(jsonBody, MapWrapper.class);

      // Password checking
      String pwd = Optional.ofNullable(map.get(GcpFunctionsEntryPointStart.PASSWORD_KEY)).orElseThrow(() -> {
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
      map.remove(GcpFunctionsEntryPointStart.PASSWORD_KEY);

      // Some error checking for the id
      String id = map.get(CommandLineOptions.ID);
      Logger.log("ID " + id + " " + map);

      if (id == null) {
        CloudUtils.send400Error(response, "Missing required field '" + CommandLineOptions.ID + "' (Id)");
        return;
      }

      String contentString = CloudUtils.readBlob(id, DataSourceGcpBuckets.CACHED_RESULTS_BUCKET);
      CloudUtils.send200Success(response, contentString);
    } catch (Exception e) {
      // Log stack
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      Logger.log(sw.toString());

      // Send exceptions
      CloudUtils.send400Error(response, e.toString());
    }
  }

}
