package com.github.thbrown.softballsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.gson.BaseOptimizationDefinition;
import com.github.thbrown.softballsim.gson.OptimizationDefinitionDeserializer;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineupgen.LineupGenerator;
import com.github.thbrown.softballsim.lineupgen.LineupTypeEnum;
import com.github.thbrown.softballsim.util.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SoftballSim {
  // Config
  public static int DEFAULT_START_INDEX = 0;
  public static int DEFAULT_UPDATE_FREQUENCY_MS = 5000;
  
  private static int TASK_BUFFER_SIZE = 1000;
  
  private static final String STATS_FILE_PATH = System.getProperty("user.dir") + File.separator + "stats";
  
  public static void main(String[] args) throws ParseException {
	 
	// The valid command line flags change based on which optimizer and data source are supplied.
	CommandLineOptions commandLineOptions = CommandLineOptions.getInstance();
	CommandLineParser parser = new DefaultParser();
		
	// If there were no arguments supplied, show help with only the common options (i.e. no additional flags for optimizer, and the default flags for data source)
	Options availableOptions = commandLineOptions.getOptionsForFlags(DataSourceEnum.getEnumFromName(CommandLineOptions.SOURCE_DEFAULT), null);
	if(args.length == 0) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(CommandLineOptions.APPLICATION_NAME, CommandLineOptions.HELP_HEADER_1, availableOptions, CommandLineOptions.HELP_FOOTER);
		return;
	}
	
	// Some arguments have been supplied, parse only the common arguments for now
	Options commonOptions = commandLineOptions.getOptionsForFlags(null, null);
	String[] filterdArgs = commandLineOptions.filterArgsArray(args, commonOptions);
	CommandLine commonCmd = parser.parse(commonOptions, filterdArgs, true);
	
	String dataSourceString = commonCmd.getOptionValue(CommandLineOptions.DATA_SOURCE, CommandLineOptions.SOURCE_DEFAULT);
	DataSourceEnum dataSource = DataSourceEnum.getEnumFromName(dataSourceString);

	OptimizerEnum optimizer = null;
	if(commonCmd.hasOption(CommandLineOptions.OPTIMIZER)) {
		String optimizerString = commonCmd.getOptionValue(CommandLineOptions.OPTIMIZER);
		optimizer = OptimizerEnum.getEnumFromIdOrName(optimizerString);
	}
	
	// Help will show the valid flags based on what optimizer and data source are supplied.
	availableOptions = commandLineOptions.getOptionsForFlags(dataSource, optimizer);
	if(commonCmd.hasOption(CommandLineOptions.HELP)) {
		String helpHeader = (optimizer == null) ? CommandLineOptions.HELP_HEADER_1 : CommandLineOptions.HELP_HEADER_2 + optimizer;
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(CommandLineOptions.APPLICATION_NAME, helpHeader, availableOptions, CommandLineOptions.HELP_FOOTER);
		return;
	}
	
	// Manually enforce optimizer as a required flag. Other required flags should be specified in their options definition so their presents
	// is enforced during parse. Enforcing the optimizer flag here manually here lets us work with a null optimizer above.
	if(optimizer == null) {
		throw new MissingArgumentException("Optimizer (-o) is a required flag. Please specify one of the following options either as a name or as an ordinal. " + OptimizerEnum.getValuesAsString());
	}
	
	// Parse command line arguments - this time include the optimizer and dataSource specific flags
	CommandLine allCmd = parser.parse(availableOptions, args, false);
	     
    if(dataSource == DataSourceEnum.FILE_SYSTEM) {
        int gamesToSimulate = Integer.parseInt(allCmd.getOptionValue(CommandLineOptions.GAMES, CommandLineOptions.GAMES_DEFAULT));
        int inningsToSimulate = Integer.parseInt(allCmd.getOptionValue(CommandLineOptions.INNINGS, CommandLineOptions.INNINGS_DEFAULT));
        int threads = Integer.parseInt(allCmd.getOptionValue(CommandLineOptions.THREADS, CommandLineOptions.THREADS_DEFAULT));
        int startIndex = DEFAULT_START_INDEX;
        
	    	String lineupTypeString = allCmd.getOptionValue(CommandLineOptions.LINEUP_TYPE, CommandLineOptions.TYPE_LINEUP_DEFAULT);
	    	LineupTypeEnum lineupType = LineupTypeEnum.getEnumFromIdOrName(String.valueOf(lineupTypeString));
	    	
        LineupGenerator generator = lineupType.getLineupGenerator();
        generator.readDataFromFile(STATS_FILE_PATH);
        
        ProgressTracker tracker = new ProgressTracker(generator.size(), DEFAULT_UPDATE_FREQUENCY_MS, startIndex, 0);
        
        OptimizationResult result = simulateLineups(generator, gamesToSimulate, inningsToSimulate, startIndex, tracker, null, null, threads);
    	
	    Logger.log(result.toString());
	    if(tracker.getLocalElapsedTimeMs() != tracker.getTotalElapsedTimeMs()) {
	      Logger.log("Local simulation time: " + tracker.getLocalElapsedTimeMs() + " milliseconds");
	    }

    } else if (dataSource == DataSourceEnum.NETWORK) {
      try{
        String connectionIp = allCmd.getOptionValue(CommandLineOptions.LOCAL_IP_ADDRESS, CommandLineOptions.LOCAL_IP_ADDRESS_DEFAULT);
        String optimizationId = allCmd.getOptionValue(CommandLineOptions.OPTIMIZATION_ID, CommandLineOptions.OPTIMIZATION_ID_DEFAULT);
        final boolean runCleanupScriptOnTerminate = commonCmd.hasOption(CommandLineOptions.CLEANUP_SCRIPT);
        
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
                if(!exists) {
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
        gsonBuilder.registerTypeAdapter(BaseOptimizationDefinition.class, new OptimizationDefinitionDeserializer());
        Gson gson = gsonBuilder.create();
        
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        try {
          // Send the start command to
          Map<String,String> readyCommand = new HashMap<>();
          readyCommand.put("command", "READY");
          readyCommand.put("optimizationId", optimizationId);
          
          String jsonReadyCommand = gson.toJson(readyCommand);
          out.println(jsonReadyCommand);
          Logger.log("SENT: \t\t" + jsonReadyCommand);
                    
          String data = null;
          while ((data = in.readLine()) != null) {
            Logger.log("RECEIVED: \t" + data);
            BaseOptimizationDefinition parsedData = gson.fromJson(data, BaseOptimizationDefinition.class);
            parsedData.runSimulation(gson, out);
            break;
          }
        } catch (Exception e) {
          StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
          String exceptionAsString = sw.toString();
          
          Map<String,Object> errorCommand = new HashMap<>();
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
    } else {
    	  throw new IllegalArgumentException("Unrecognized data source: " + dataSource);
    }
    
    // Java isn't exiting for some reason. Force close.
    //System.exit(0);
  }
  
  public static OptimizationResult simulateLineups(LineupGenerator generator, int gamesToSimulate, int inningsPerGame, long startIndex, ProgressTracker tracker, Result initialResult, Map<Long, Long> initialHisto, int threads) {

    // Print the details before we start
    DecimalFormat formatter = new DecimalFormat("#,###");
    Logger.log("*********************************************************************");
    Logger.log("Possible lineups: \t\t" + formatter.format(generator.size()));
    Logger.log("Games to simulate per lineup: \t" + gamesToSimulate);
    Logger.log("Innings per game: \t\t" + inningsPerGame);
    Logger.log("Threads used: \t\t\t" + threads);
    Logger.log("*********************************************************************");
    
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    Queue<Future<Result>> results = new LinkedList<>();
    
    // Queue up a few tasks to process (number of tasks is capped by TASK_BUFFER_SIZE)
    long max = generator.size() - startIndex > TASK_BUFFER_SIZE ? TASK_BUFFER_SIZE + startIndex : generator.size();
    for(long l = startIndex; l < max; l++) {
      Simulation s = new Simulation(generator.getLineup(l), gamesToSimulate, inningsPerGame);
      results.add(executor.submit(s));
    }
    
    // Process results as they finish executing
    Result bestResult = initialResult != null ? initialResult : null;
    Map<Long, Long> histo = initialHisto == null ? new HashMap<>() : initialHisto;
    long counter = max;
    while(!results.isEmpty()) {
        // Wait for the result
        Result result = null;
        try {
            Future<Result> future = results.poll();
            if(future != null) {
                result = future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
      
        // Update histogram
        long key = (long)(result.getScore()*10);
        if(histo.containsKey(key)) {
          histo.put(key, histo.get(key)+1);
        } else {
          histo.put(key, 1L);
        }
        
        // Update the best lineup, if necessary
        if (bestResult == null || result.getScore() > bestResult.getScore()) {
          bestResult = result;
        }
        
        // Update the progress tracker
        tracker.markOperationAsComplete(bestResult, histo);
      
        // Add another task to the buffer if there are any left
        BattingLineup lineup = generator.getLineup(counter);
        if(lineup != null) {
            Simulation s = new Simulation(lineup, gamesToSimulate, inningsPerGame);
            results.add(executor.submit(s));
            //ThreadPoolExecutor ex =(ThreadPoolExecutor)executor;
            //Logger.log("Adding task 2 " + ex.getQueue().size() + " " + ex.);
            counter++;
        }
    }
    executor.shutdown();
    return new OptimizationResult(bestResult, histo, tracker.getTotalElapsedTimeMs());
  }
}
