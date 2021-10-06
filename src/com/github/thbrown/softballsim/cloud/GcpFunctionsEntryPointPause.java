package com.github.thbrown.softballsim.cloud;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import com.github.thbrown.softballsim.datasource.gcpfunctions.DataSourceGcpBuckets;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
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

      // Extract body
      byte[] jsonBodyBytes = request.getInputStream().readAllBytes();
      String jsonBody = new String(jsonBodyBytes, StandardCharsets.UTF_8);

      // Parse JSON to map
      MapWrapper map = gson.fromJson(jsonBody, MapWrapper.class);

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

      // Success, but no response needed
      response.setContentType("application/json");
      response.setStatusCode(200);
      String payload = CloudUtils.getResponseJson("SUCCESS", "Successfully sent pause request");
      response.getOutputStream().write(payload.getBytes());
    } catch (Exception e) {
      // Log stack
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      Logger.log(sw.toString());

      // Sent exception as plain text
      response.setContentType("test/plain");
      response.setStatusCode(400);
      BufferedWriter writer = response.getWriter();
      writer.write(e.toString());
    }
  }

}
