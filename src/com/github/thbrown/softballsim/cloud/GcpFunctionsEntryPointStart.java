package com.github.thbrown.softballsim.cloud;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.SoftballSim;
import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.datasource.gcpfunctions.DataSourceGcpFunctions;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;

/**
 * This class enables the ability for optimizers to be run via a GCP function.
 * 
 * It's a high level glue class to attach gcp and the sotball-sim application.
 * 
 * TODO: Error handling, fix null pointer on bad json data supplied TODO: Handle boolean args better
 */
public class GcpFunctionsEntryPointStart implements HttpFunction {

  private OutputStream outputStream;
  // The '/tmp' directory is the only writable directory for gcp functions
  private static final String DATA_FILE_LOCATION = "/tmp/data.json";

  @Override
  public void service(HttpRequest request, HttpResponse response) throws Exception {

    try {
      // Setup
      Gson gson = GsonAccessor.getInstance().getCustom();

      // Extract body
      byte[] jsonBodyBytes = request.getInputStream().readAllBytes();
      String jsonBody = new String(jsonBodyBytes, StandardCharsets.UTF_8);

      // Parse JSON to map
      Arguments map = gson.fromJson(jsonBody, Arguments.class);

      // Some error checking for the id
      String id = map.get(DataSourceGcpFunctions.ID);
      if (id == null) {
        send400Error(response, "Required json field 'I' (id) was not specified in the body");
        return;
      }
      int length = id.length();
      if (length < 15 || length > 63) {
        send400Error(response,
            "Please provide an Id (-I) that is longer than 15 characters and shorter than 64 characters. Was " + length
                + " characters");
        return;
      }

      // Write the data to disk (this consumes memory resources)
      String data = map.get("data");
      if (data != null) {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(DATA_FILE_LOCATION)));
        writer.write(data);
        writer.close();
        map.remove("data");
      } else {
        throw new RuntimeException("Required json field 'data' was not specified in the body");
      }

      // We have a set of args that are used for all optimizations invoked from gcp functions
      List<String> args = new ArrayList<>();
      args.add("-P");
      args.add(DATA_FILE_LOCATION);
      args.add("-D");
      args.add(DataSourceEnum.GCP_FUNCTIONS.name());

      // Convert the remaining map entries to command line arguments
      for (String flag : map.keySet()) {
        String value = map.get(flag);
        if (Boolean.parseBoolean(value)) { // Flags without args
          args.add("-" + flag);
        } else {
          args.add("-" + flag);
          args.add(map.get(flag));
        }
      }

      // Run optimization with those command line arguments
      Logger.log("Arguments: " + args.toString());
      Result result = SoftballSim.mainInternal(args.toArray(new String[args.size()]));
      Logger.log(result);

      // Return the result as json
      response.setContentType("text/plain");
      response.getOutputStream().write(gson.toJson(result).getBytes());

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

  private void send400Error(HttpResponse response, String message) throws IOException {
    response.setContentType("text/plain");
    response.setStatusCode(400);
    response.getOutputStream().write(message.getBytes());
  }

}
