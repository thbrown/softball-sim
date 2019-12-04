package com.github.thbrown.softballsim.datasource;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import com.github.thbrown.softballsim.util.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DataSourceNetwork implements DataSource {

  // DataSource - NETWORK
  public final static String LOCAL_IP_ADDRESS = "L";
  public final static String OPTIMIZATION_ID = "K";
  public final static String CLEANUP_SCRIPT = "C";

  public final static String LOCAL_IP_ADDRESS_DEFAULT = "127.0.0.1";
  public final static String OPTIMIZATION_ID_DEFAULT = "0000000000";

  @Override
  public List<Option> getCommandLineOptions() {
    List<Option> options = new ArrayList<>();
    options.add(Option.builder(LOCAL_IP_ADDRESS)
        .longOpt("Local-ip-address")
        .desc(DataSourceEnum.NETWORK
            + ": The ip address the application should attempt to connect to in order to get the information required to run the optimization. Default: "
            + LOCAL_IP_ADDRESS_DEFAULT)
        .hasArg(true)
        .required(false)
        .build());
    options.add(Option.builder(OPTIMIZATION_ID)
        .longOpt("Optimization-id")
        .desc(
            DataSourceEnum.NETWORK + ": The id of the optimization information to be request from the server. Default: "
                + OPTIMIZATION_ID_DEFAULT)
        .hasArg(true)
        .required(false)
        .build());
    // Should this be common?
    options.add(Option.builder(CLEANUP_SCRIPT)
        .longOpt("Cleanup-script")
        .desc(DataSourceEnum.NETWORK
            + ": If this flag is provided, the appliction will attempt to invoke ./cleanup.sh (linux, osx) or ./cleanup.bat (windows) after the optimization completes.")
        .hasArg(false)
        .required(false)
        .build());
    return options;
  }

  @Override
  public void execute(CommandLine allCmd) {
    try {
      String connectionIp = allCmd.getOptionValue(LOCAL_IP_ADDRESS, LOCAL_IP_ADDRESS_DEFAULT);
      String optimizationId = allCmd.getOptionValue(OPTIMIZATION_ID, OPTIMIZATION_ID_DEFAULT);
      final boolean runCleanupScriptOnTerminate = allCmd.hasOption(CLEANUP_SCRIPT);

      // Register shutdown hook - invoke the cleanup script whenever this application dies. This
      // is intended to shutdown/delete the cloud instance this application runs after the simulation
      // finishes it's job or is exited prematurely.
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          System.out.println("Application Terminating ...");
          if (runCleanupScriptOnTerminate) {
            try {
              Logger.log("Attempting to run cleanup script");
              ProcessBuilder pb = null;
              String operatingSystem = System.getProperty("os.name");
              File cleanupScript = null;
              if ("Linux".equals(operatingSystem) || "Mac OS X".equals(operatingSystem)) {
                cleanupScript = new File("cleanup.sh");
                pb = new ProcessBuilder("/bin/sh", cleanupScript.getName());
              } else if ("Windows".equals(operatingSystem) || "Windows 10".equals(operatingSystem)) {
                cleanupScript = new File("cleanup.bat");
                pb = new ProcessBuilder(cleanupScript.getName());
              }

              boolean exists = cleanupScript.exists();
              if (!exists) {
                System.out.println("Could not find cleanup.sh, skipping cleanup");
                return;
              }

              pb.directory(cleanupScript.getAbsoluteFile().getParentFile());
              System.out.println(pb.command());
              Process p = pb.start();
              System.out.println("Cleanup script exited with status " + p.waitFor());

            } catch (Exception e) {
              Logger.log("Encountered error while running shutdown hook");
              e.printStackTrace();
            }
          } else {
            Logger.log("Skipping shutdown");
          }
        }
      });

      int port = 8414;

      Logger.log("[Connecting to " + connectionIp + ":" + port + "]");
      Socket socket = new Socket(connectionIp, port);

      final GsonBuilder gsonBuilder = new GsonBuilder();
      gsonBuilder.registerTypeAdapter(NetworkCommandArgumentDeserializer.class,
          new NetworkCommandArgumentDeserializer());
      Gson gson = gsonBuilder.create();

      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      try {
        // Send the start command to
        Map<String, String> readyCommand = new HashMap<>();
        readyCommand.put("command", "READY");
        readyCommand.put("optimizationId", optimizationId);

        String jsonReadyCommand = gson.toJson(readyCommand);
        out.println(jsonReadyCommand);
        Logger.log("SENT: \t\t" + jsonReadyCommand);

        String data = null;
        while ((data = in.readLine()) != null) {
          Logger.log("RECEIVED: \t" + data);
          NetworkCommandArgumentDeserializer parsedData = gson.fromJson(data, NetworkCommandArgumentDeserializer.class);
          // parsedData.runSimulation(gson, out);
          break;
        }
      } catch (Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();

        Map<String, Object> errorCommand = new HashMap<>();
        errorCommand.put("command", "ERROR");
        errorCommand.put("message", e.toString());
        errorCommand.put("trace", exceptionAsString);
        String jsonErrorCommand = gson.toJson(errorCommand);
        out.println(jsonErrorCommand);
        Logger.log("SENT: \t\t" + jsonErrorCommand);
        throw e;
      } finally {
        Thread.sleep(1000);
        socket.close();
      }
    } catch (Exception e) {
      Logger.log(e);
      e.printStackTrace();
    }
  }

}
