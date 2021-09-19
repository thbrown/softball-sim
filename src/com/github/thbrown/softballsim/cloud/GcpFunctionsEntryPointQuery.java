package com.github.thbrown.softballsim.cloud;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.github.thbrown.softballsim.datasource.gcpfunctions.DataSourceGcpBuckets;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.*;
import com.google.common.collect.ImmutableList;

/**
 * A second GCP function that gets an intermediate result for an optimization
 * run.
 * 
 * Intermediate result are stored in a cloud bucket while the *Start function is
 * running.
 */
public class GcpFunctionsEntryPointQuery implements HttpFunction {

  @Override
  public void service(HttpRequest request, HttpResponse response) throws Exception {

    // Setup
    Gson gson = GsonAccessor.getInstance().getCustom();

    // Extract body
    byte[] jsonBodyBytes = request.getInputStream().readAllBytes();
    String jsonBody = new String(jsonBodyBytes, StandardCharsets.UTF_8);

    // Parse JSON to map
    MapWrapper map = gson.fromJson(jsonBody, MapWrapper.class);

    // Some error checking for the id
    String id = map.get(DataSourceGcpBuckets.ID);
    Logger.log("ID " + id + " " + map);

    if (id == null) {
      send400Error(response, "Missing required field 'I' (Id)");
      return;
    }

    try {
      String contentString = CloudUtils.readBlob(id, DataSourceGcpBuckets.CACHED_RESULTS_BUCKET);
      response.setContentType("application/json");
      response.setStatusCode(200);
      response.getOutputStream().write(contentString.getBytes());
    } catch (Exception e) {
      send400Error(response, e.toString());
    }
  }

  private void send400Error(HttpResponse response, String message) throws IOException {
    response.setContentType("text/plain");
    response.setStatusCode(400);
    response.getOutputStream().write(message.getBytes());
  }

}
