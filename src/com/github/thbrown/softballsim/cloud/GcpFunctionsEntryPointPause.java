package com.github.thbrown.softballsim.cloud;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import com.github.thbrown.softballsim.datasource.gcpbuckets.DataSourceGcpBuckets;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;

/**
 * This class enables the ability for optimizers to be paused via a GCP function.
 */
public class GcpFunctionsEntryPointPause implements HttpFunction {

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
      String id = map.get(DataSourceGcpBuckets.ID);
      if (id == null) {
        CloudUtils.send400Error(response,
            "Required json field " + DataSourceGcpBuckets.ID + " was not specified in the body");
        return;
      }

      // We'll add flag to the control bucket to indicate that a currently running
      // optimization should stop
      CloudUtils.upsertBlob("HALT", id, DataSourceGcpBuckets.CONTROL_FLAGS_BUCKET);

      // Return success
      CloudUtils.send200Success(response, "Successfully sent pause request");
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
