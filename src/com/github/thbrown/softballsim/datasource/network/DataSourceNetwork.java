package com.github.thbrown.softballsim.datasource.network;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.datasource.DataSource;
import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.Logger;

public class DataSourceNetwork implements DataSource {

  // DataSource - NETWORK
  public final static String IP_ADDRESS = "I";
  public final static String OPTIMIZATION_ID = "K";
  public final static String CLEANUP_SCRIPT = "C";

  public final static String LOCAL_IP_ADDRESS_DEFAULT = "127.0.0.1";
  public final static String OPTIMIZATION_ID_DEFAULT = "0000000000";

  @Override
  public List<Option> getCommandLineOptions() {
    List<Option> options = new ArrayList<>();
    options.add(Option.builder(IP_ADDRESS)
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
  public Result execute(String[] args, LineupTypeEnum lineupType, List<String> players, OptimizerEnum optimizer) {
    // Parse command line arguments, this time include arguments that apply only to DataSouceNetwork
    CommandLineOptions commandLineOptions = CommandLineOptions.getInstance();
    Options commonAndDataSourceOptions = commandLineOptions.getOptionsForFlags(DataSourceEnum.NETWORK, null);
    CommandLine commonAndDataSource = commandLineOptions.parse(commonAndDataSourceOptions, args, true);

    try {
      String connectionIp = commonAndDataSource.getOptionValue(IP_ADDRESS, LOCAL_IP_ADDRESS_DEFAULT);
      String optimizationId = commonAndDataSource.getOptionValue(OPTIMIZATION_ID, OPTIMIZATION_ID_DEFAULT);
      final boolean runCleanupScriptOnTerminate = commonAndDataSource.hasOption(CLEANUP_SCRIPT);

      // Setup shutdown behavior
      if (runCleanupScriptOnTerminate) {
        registerShutdownHook();
      }

      // Connect to the network (to get simulation data)
      int port = 8414;
      Logger.log("[Connecting to " + connectionIp + ":" + port + "]");
      Socket socket = new Socket(connectionIp, port);
      NetworkHelper network = new NetworkHelper(socket);

      try {
        // Send the ready command to the remote instance, it will send us data
        DataSourceNetworkCommandReady readyCommand = new DataSourceNetworkCommandReady(optimizationId);
        network.writeCommand(readyCommand);

        // Listen for the next command -- TODO: We don't need the loop here, we are only reading one
        // command
        DataSourceNetworkCommand command = null;
        while ((command = network.readCommand()) != null) {
          command.process(args, lineupType, players, optimizer, network);
          break;
        }
      } catch (Exception e) {
        // Something went wrong, send the details to the remote instance
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();

        DataSourceNetworkCommandError errorCommand = new DataSourceNetworkCommandError(e.toString(), exceptionAsString);
        network.writeCommand(errorCommand);
        throw e;
      } finally {
        Thread.sleep(1000);
        socket.close();
      }
    } catch (Exception e) {
      Logger.log(e);
      e.printStackTrace();
    }
    return null; // TODO: Make this return the result for completeness sake?
  }

  /**
   * Register shutdown hook - invoke the cleanup script whenever this application dies. This is
   * intended to shutdown/delete the cloud instance this application runs after the simulation
   * finishes it's job or is exited prematurely.
   */
  private void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        System.out.println("Application Terminating ...");
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
          Logger.error("Encountered error while running shutdown hook");
          e.printStackTrace();
        }
      }
    });
  }


}
