package com.github.thbrown.softballsim.cloud;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.github.thbrown.softballsim.datasource.gcpfunctions.DataSourceFunctionsGcpFunctions;
import com.github.thbrown.softballsim.datasource.gcpfunctions.DataSourceGcpFunctions;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.gson.Gson;

/**
 * A second GCP function that gets an intermediate result for an optimization run.
 * 
 * Intermediate result are stored in a cloud bucket whilt the *Start function is running.
 * 
 * TODO: This should maybe be in a different project? Or at least generate it's own, smaller jar.
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
    Arguments map = gson.fromJson(jsonBody, Arguments.class);

    // Some error checking for the id
    String id = map.get(DataSourceGcpFunctions.ID);
    Logger.log("ID " + id + " " + map);

    if (id == null) {
      send400Error(response, "Missing required filed 'I' (Id)");
      return;
    }

    Storage storage = StorageOptions.getDefaultInstance().getService();

    try {
      Logger.log("Getting bucket " + DataSourceFunctionsGcpFunctions.BUCKET_NAME + ":" + id);
      BlobId blobId = BlobId.of(DataSourceFunctionsGcpFunctions.BUCKET_NAME, id);
      byte[] content = storage.readAllBytes(blobId);
      String contentString = new String(content, DataSourceFunctionsGcpFunctions.ENCODING);
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
