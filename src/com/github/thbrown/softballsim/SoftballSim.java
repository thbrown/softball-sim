package com.github.thbrown.softballsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;

import com.github.thbrown.softballsim.datasource.DataSource;
import com.github.thbrown.softballsim.datasource.NetworkProgressTracker;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.DummyAlternatingBattingLineup;
import com.github.thbrown.softballsim.lineup.DummyOrdinaryBattingLineup;
import com.github.thbrown.softballsim.lineup.OrdinaryBattingLineup;
import com.github.thbrown.softballsim.lineupgen.LineupGenerator;
import com.github.thbrown.softballsim.lineupgen.LineupType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class SoftballSim {
  // Config
  private static int DEFAULT_GAMES_TO_SIMULATE = 10000;
  private static int DEFAULT_INNINGS_PER_GAME = 6;
  private static int DEFAULT_START_INDEX = 0;
  private static int DEFAULT_UPDATE_FREQUENCY = 1000;
  
  private static int TASK_BUFFER_SIZE = 1000;
  private static final int THREADS_TO_USE = Runtime.getRuntime().availableProcessors() - 1;
  
  private static DataSource DATA_SOURCE = DataSource.FILE_SYSTEM;
  private static final String STATS_FILE_PATH = System.getProperty("user.dir") + File.separator + "stats";
  
  public static void main(String[] args) {
    // Args
    validateArgs(args);
    try {
      DATA_SOURCE = DataSource.valueOf(args[0]);
    } catch (IllegalArgumentException e) {
      System.out.println("Invalid data source, must be 'NETWORK' or 'FILE_SYSTEM' but was '" + args[0] + "'");
      System.exit(0);
    }
    
    if(DATA_SOURCE == DataSource.FILE_SYSTEM) {
        int gamesToSimulate = args.length >= 3 ? Integer.parseInt(args[3]) : DEFAULT_GAMES_TO_SIMULATE;
        int inningsToSimulate = args.length >= 4 ? Integer.parseInt(args[4]) : DEFAULT_INNINGS_PER_GAME;
        int startIndex = DEFAULT_START_INDEX;

        long startTime = System.currentTimeMillis();
        
        LineupGenerator generator = getLineupGenerator(args[1]);
        generator.readDataFromFile(STATS_FILE_PATH);
        
        ProgressTracker tracker = new ProgressTracker(generator.size(), DEFAULT_UPDATE_FREQUENCY, startIndex);
        
        OptimizationResult result = simulateLineups(generator, gamesToSimulate, inningsToSimulate, startIndex, tracker, null, null);
    	
	    System.out.println(result.toString());
    	System.out.println("Simulation took " + (System.currentTimeMillis() - startTime) + " milliseconds.");

    } else if (DATA_SOURCE == DataSource.NETWORK) {
      try{
        String ip = "127.0.0.1"; // TODO: pass this as an argument?
        int port = 6969;
        
        System.out.println("[Connecting to socket...]");
        Socket socket = new Socket(ip, port);

        Gson gson = new GsonBuilder().create();
        
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        try {
          // Send the start command to
          Map<String,String> readyCommand = new HashMap<>();
          readyCommand.put("command", "READY");
          
          String jsonReadyCommand = gson.toJson(readyCommand);
          out.println(jsonReadyCommand);
          System.out.println("SENT: \t\t" + jsonReadyCommand);
          
          OptimizationResult result = null;
          String data = null;
          while ((data = in.readLine()) != null) {
            System.out.println("RECEIVED: \t" + data);
            
            Type empMapType = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> map = gson.fromJson(data, empMapType);
            
            int gamesToSimulate = map.get("iterations") != null ? Integer.parseInt(map.get("iterations")) : DEFAULT_GAMES_TO_SIMULATE;
            int inningsToSimulate = map.get("innings") != null ? Integer.parseInt(map.get("innings")) : DEFAULT_INNINGS_PER_GAME;
            long startIndex = map.get("startIndex") != null ? Long.parseLong(map.get("startIndex")) : DEFAULT_START_INDEX;
            String lineupType = map.get("lineupType");
            
            // Account for initial conditions if specified
            Type histoType = new TypeToken<Map<Integer, Integer>>(){}.getType();
            Map<Long, Long> initialHisto = map.get("initialHisto") != null ? gson.fromJson(map.get("initialHisto"), histoType) : null;
            Type lineupObjectType = new TypeToken<Map<String, List<String>>>(){}.getType();
            Map<String,List<String>> initialLineup = gson.fromJson(map.get("initialLineup"), lineupObjectType);
            Double initialScore = map.get("initialScore") == null ? null : Double.parseDouble(map.get("initialScore"));
            Result initialResult = null;
            if(initialScore != null && initialLineup != null && initialHisto != null) {
              // Build a list of players
              if(lineupType.equals("0") || lineupType.equals("2")) {
                initialResult = new Result(initialScore, new DummyOrdinaryBattingLineup(initialLineup.get("GroupA")));
              } else if(lineupType.equals("1")) {
                initialResult = new Result(initialScore, new DummyAlternatingBattingLineup(initialLineup.get("GroupA"), initialLineup.get("GroupB")));
              } else {
                throw new RuntimeException("Unrecognized lineup type: " + lineupType);
              }

              System.out.println("Initial conditions were specified");
              System.out.println(initialResult);
              System.out.println(initialHisto);
            } else {
              initialHisto = null;
              initialResult = null;
            }
            
            long startTime = System.currentTimeMillis();
            
            LineupGenerator generator = getLineupGenerator(map.get("lineupType"));
            generator.readDataFromString(map.get("data"));
            
            ProgressTracker tracker = new NetworkProgressTracker(generator.size(), DEFAULT_UPDATE_FREQUENCY, startIndex, gson, out);
            
            result = simulateLineups(generator, gamesToSimulate, inningsToSimulate, startIndex, tracker, initialResult, initialHisto);
            
            System.out.println(result.toString());
            System.out.println("Simulation took " + (System.currentTimeMillis() - startTime) + " milliseconds.");
            break;
          }
          
          Map<String,Object> completeCommand = new HashMap<>();
          completeCommand.put("command", "COMPLETE");
          completeCommand.put("lineup", result.getLineup());
          completeCommand.put("score", result.getScore());
          completeCommand.put("histogram", result.getHistogram());
          String jsonCompleteCommand = gson.toJson(completeCommand);
          out.println(jsonCompleteCommand);
          System.out.println("SENT: \t\t" + jsonCompleteCommand);
            
        } catch (Exception e) {
          StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
          String exceptionAsString = sw.toString();
          
          Map<String,Object> completeCommand = new HashMap<>();
          completeCommand.put("command", "ERROR");
          completeCommand.put("message", e.getMessage());
          completeCommand.put("trace", exceptionAsString);
          String jsonCompleteCommand = gson.toJson(completeCommand);
          out.println(jsonCompleteCommand);
          System.out.println("SENT: \t\t" + jsonCompleteCommand); 
          throw e;
        } finally {
           socket.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        String shutdownCommand;
        String operatingSystem = System.getProperty("os.name");

        if ("Linux".equals(operatingSystem) || "Mac OS X".equals(operatingSystem)) {
            shutdownCommand = "shutdown -h now";
        }
        else if ("Windows".equals(operatingSystem) || "Windows 10".equals(operatingSystem)) {
            shutdownCommand = "shutdown.exe -s -t 0";
        }
        else {
            throw new RuntimeException("Unsupported operating system: " + operatingSystem);
        }

        //Runtime.getRuntime().exec(shutdownCommand);
        System.out.println("Instance would have shut down " + shutdownCommand);
        System.exit(0);
      }
    } else {
    	throw new IllegalArgumentException("Unrecognized data source: " + DATA_SOURCE);
    }
  }

  private static LineupGenerator getLineupGenerator(String lineupTypeString) {
    LineupType lineupType = null;
    try {
      lineupType = LineupType.valueOf(lineupTypeString.toUpperCase());
    } catch (IllegalArgumentException e) {
      try {
        // This is brittle, but should be fairly stable.
        int ordinal = Integer.parseInt(lineupTypeString);
        lineupType = LineupType.values()[ordinal];
      } catch (IndexOutOfBoundsException | NumberFormatException unused) {
        System.out.println(String.format("Invalid LineupType. Was \"%s\".", lineupTypeString));
        printAvailableLineupTypes();
        System.exit(1);
      }
    }

    LineupGenerator generator = lineupType.getLineupGenerator();
    return generator;
  }

  private static void validateArgs(String[] args) {
    if (args.length == 0) {
      System.out.println("Usage: java SoftballSim <DataSource> <LineupType> <GamesToSimulate default=10000> <InningsToSimulate default=7>");
      System.out.println("\tExpecting input files in " + STATS_FILE_PATH);
      printAvailableLineupTypes();
      System.exit(0);
    }
  }

  public static void printAvailableLineupTypes() {
    System.out.println("\tAvailable lineup generators:");
    LineupType[] lineupTypes = LineupType.values();
    for (int i = 0; i < lineupTypes.length; i++) {
      System.out.println(String.format("\t\t\"%s\" or \"%s\"", lineupTypes[i], i));
    }
  }
  
  private static OptimizationResult simulateLineups(LineupGenerator generator, int gamesToSimulate, int inningsPerGame, long startIndex, ProgressTracker tracker, Result initialResult, Map<Long, Long> initialHisto) {

    // Print the details before we start
    DecimalFormat formatter = new DecimalFormat("#,###");
    System.out.println("*********************************************************************");
    System.out.println("Possible lineups: \t\t" + formatter.format(generator.size()));
    System.out.println("Games to simulate per lineup: \t" + gamesToSimulate);
    System.out.println("Innings per game: \t\t" + inningsPerGame);
    System.out.println("Threads used: \t\t\t" + THREADS_TO_USE);
    System.out.println("*********************************************************************");
    
    ExecutorService executor = Executors.newFixedThreadPool(THREADS_TO_USE);
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
            ThreadPoolExecutor ex =(ThreadPoolExecutor)executor;
            //System.out.println("Adding task 2 " + ex.getQueue().size() + " " + ex.);
            counter++;
        }
    }
    
    return new OptimizationResult(bestResult, histo);
  }
}
