package com.github.thbrown.softballsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;

import com.github.thbrown.softballsim.datasource.DataSource;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.gson.BaseOptimizationDefinition;
import com.github.thbrown.softballsim.gson.OptimizationDefinitionDeserializer;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineupgen.LineupGenerator;
import com.github.thbrown.softballsim.lineupgen.LineupType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SoftballSim {
  // Config
  public static int DEFAULT_GAMES_TO_SIMULATE = 1000;
  public static int DEFAULT_INNINGS_PER_GAME = 7;
  public static int DEFAULT_START_INDEX = 0;
  public static int DEFAULT_UPDATE_FREQUENCY_MS = 5000;
  
  private static int TASK_BUFFER_SIZE = 1000;
  
  private static DataSource DATA_SOURCE = DataSource.FILE_SYSTEM;
  private static final String STATS_FILE_PATH = System.getProperty("user.dir") + File.separator + "stats";
  public static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors();
  
  public static void main(String[] args) {
    // Args
    validateArgs(args);
    try {
      DATA_SOURCE = DataSource.valueOf(args[0]);
    } catch (IllegalArgumentException e) {
      Logger.log("Invalid data source, must be 'NETWORK' or 'FILE_SYSTEM' but was '" + args[0] + "'");
      System.exit(0);
    }
        
    if(DATA_SOURCE == DataSource.FILE_SYSTEM) {
        int gamesToSimulate = args.length >= 3 ? Integer.parseInt(args[2]) : DEFAULT_GAMES_TO_SIMULATE;
        int inningsToSimulate = args.length >= 4 ? Integer.parseInt(args[3]) : DEFAULT_INNINGS_PER_GAME;
        int threads = args.length >= 4 ? Integer.parseInt(args[4]) : DEFAULT_THREADS;
        int startIndex = DEFAULT_START_INDEX;
        
        LineupGenerator generator = getLineupGenerator(args[1]);
        generator.readDataFromFile(STATS_FILE_PATH);
        
        ProgressTracker tracker = new ProgressTracker(generator.size(), DEFAULT_UPDATE_FREQUENCY_MS, startIndex, 0);
        
        OptimizationResult result = simulateLineups(generator, gamesToSimulate, inningsToSimulate, startIndex, tracker, null, null, threads);
    	
	    Logger.log(result.toString());
	    if(tracker.getLocalElapsedTimeMs() != tracker.getTotalElapsedTimeMs()) {
	      Logger.log("Local simulation time: " + tracker.getLocalElapsedTimeMs() + " milliseconds");
	    }

    } else if (DATA_SOURCE == DataSource.NETWORK) {
      try{
        String connectionIp = args.length >= 2 ? args[1] : "127.0.0.1";
        String optimizationId = args.length >= 3 ? args[2] : "0000000000";
        final boolean runCleanupScriptOnTerminate = args.length >= 4 ? Boolean.parseBoolean(args[3]) : false;
        
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
                if ("Linux".equals(operatingSystem) || "Mac OS X".equals(operatingSystem)) {
                  File tmpDir = new File("cleanup.sh");
                  boolean exists = tmpDir.exists();
                  if(!exists) {
                    System.out.println("Could not find cleanup.sh, skiping cleanup");
                    return;
                  }
                  pb = new ProcessBuilder("cleanup.sh");
                } else if ("Windows".equals(operatingSystem) || "Windows 10".equals(operatingSystem)) {
                  File tmpDir = new File("cleanup.bat");
                  boolean exists = tmpDir.exists();
                  if(!exists) {
                    System.out.println("Could not find cleanup.bat, skiping cleanup");
                    return;
                  }
                  pb = new ProcessBuilder("cleanup.bat");
                } else {
                  System.out.println("Skipping cleanup because OS is not supported: " + operatingSystem);
                  return;
                }
                pb.directory(new File("./"));
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
    	throw new IllegalArgumentException("Unrecognized data source: " + DATA_SOURCE);
    }
    
    // Java isn't exiting for some reason. Force close.
    //System.exit(0);
  }

  public static LineupGenerator getLineupGenerator(String lineupTypeString) {
    LineupType lineupType = null;
    try {
      lineupType = LineupType.valueOf(lineupTypeString.toUpperCase());
    } catch (IllegalArgumentException e) {
      try {
        // This is brittle, but should be fairly stable.
        int ordinal = Integer.parseInt(lineupTypeString);
        lineupType = LineupType.values()[ordinal-1];
      } catch (IndexOutOfBoundsException | NumberFormatException unused) {
        Logger.log(String.format("Invalid LineupType. Was \"%s\".", lineupTypeString));
        printAvailableLineupTypes();
        System.exit(1);
      }
    }

    LineupGenerator generator = lineupType.getLineupGenerator();
    return generator;
  }

  private static void validateArgs(String[] args) {
    if (args.length == 0) {
      Logger.log("Usage: java SoftballSim <DataSource> <LineupType> <GamesToSimulate default=10000> <InningsToSimulate default=7>");
      Logger.log("\tExpecting input files in " + STATS_FILE_PATH);
      printAvailableLineupTypes();
      System.exit(0);
    }
  }

  public static void printAvailableLineupTypes() {
    Logger.log("\tAvailable lineup generators:");
    LineupType[] lineupTypes = LineupType.values();
    for (int i = 0; i < lineupTypes.length; i++) {
      Logger.log(String.format("\t\t\"%s\" or \"%s\"", lineupTypes[i], i+1));
    }
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

    return new OptimizationResult(bestResult, histo, tracker.getTotalElapsedTimeMs());
  }
}
