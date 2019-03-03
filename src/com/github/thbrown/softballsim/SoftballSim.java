package com.github.thbrown.softballsim;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;

import com.github.thbrown.softballsim.datasource.DataSource;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineupgen.LineupGenerator;
import com.github.thbrown.softballsim.lineupgen.LineupType;

public class SoftballSim {
  // Config
  public static int GAMES_TO_SIMULATE = 1;
  public static int INNINGS_PER_GAME = 7;
  public static int START_INDEX = 0;
  public static int TASK_BUFFER_SIZE = 1000;
  
  public static DataSource DATA_SOURCE = DataSource.FILE_SYSTEM;
  public static final String STATS_FILE_PATH = System.getProperty("user.dir") + File.separator + "stats";

  public static boolean VERBOSE = false;
  public static final int THREADS_TO_USE = Runtime.getRuntime().availableProcessors() - 1;
  public static final int NAME_PADDING = 24; // Just for formatting verbose output

  public static void main(String[] args) {
    // Args
    validateArgs(args);
    GAMES_TO_SIMULATE = args.length >= 2 ? Integer.parseInt(args[2]) : GAMES_TO_SIMULATE;
    INNINGS_PER_GAME = args.length >= 3 ? Integer.parseInt(args[3]) : INNINGS_PER_GAME;
    
    if(DATA_SOURCE == DataSource.FILE_SYSTEM) {
    	long startTime = System.currentTimeMillis();
	    
	    LineupGenerator generator = getLineupGenerator(args[0]);
	    generator.readDataFromFile(STATS_FILE_PATH);
	    
	    // Print the details before we start
	    DecimalFormat formatter = new DecimalFormat("#,###");
	    System.out.println("*********************************************************************");
	    System.out.println("Possible lineups: \t\t" + formatter.format(generator.size()));
	    System.out.println("Games to simulate per lineup: \t" + GAMES_TO_SIMULATE);
	    System.out.println("Innings per game: \t\t" + INNINGS_PER_GAME);
	    System.out.println("Threads used: \t\t\t" + THREADS_TO_USE);
	    System.out.println("*********************************************************************");
	    
	    ExecutorService executor = Executors.newFixedThreadPool(THREADS_TO_USE);
	    Queue<Future<Result>> results = new LinkedList<>();
	    ProgressTracker tracker = new ProgressTracker(generator.size(), 100);
	    
	    // Queue up a few tasks to process (number of tasks is capped by TASK_BUFFER_SIZE)
	    long max = generator.size() - START_INDEX > TASK_BUFFER_SIZE ? TASK_BUFFER_SIZE + START_INDEX : generator.size();
	    for(long l = START_INDEX; l < max; l++) {
	      Simulation s = new Simulation(generator.getLineup(l), GAMES_TO_SIMULATE, tracker);
	      results.add(executor.submit(s));
	    }
	    
	    // Process results as they finish executing
	    Result bestResult = null;
	    Map<Integer, Integer> histo = new HashMap<>();
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
	        int key = (int)(result.getScore()*10);
	        if(histo.containsKey(key)) {
	          histo.put(key, histo.get(key)+1);
	        } else {
	          histo.put(key, 1);
	        }
	        
	        // Update the best lineup, if necessary
	        if (bestResult == null || result.getScore() > bestResult.getScore()) {
	          bestResult = result;
	        }
	      
	        // Add another task to the buffer if there are any left
	        BattingLineup lineup = generator.getLineup(counter);
	        if(lineup != null) {
		        Simulation s = new Simulation(lineup, GAMES_TO_SIMULATE, tracker);
		        results.add(executor.submit(s));
		        ThreadPoolExecutor ex=(ThreadPoolExecutor)executor;
		        //System.out.println("Adding task 2 " + ex.getQueue().size() + " " + ex.);
		        counter++;
	        }
	    }
	  
	    // Print the results
	    System.out.println();
	    System.out.println("Best lineup");
	    System.out.println(bestResult.getLineup());
	    System.out.println("Best lineup mean runs scored: " + bestResult.getScore());
	    System.out.println("Simulation took " + (System.currentTimeMillis() - startTime) + " milliseconds.");
	    for(Integer k : histo.keySet()) {
	      System.out.println(k/10.0 + " - " + histo.get(k));
	    }
	    System.exit(0);
    } else if (DATA_SOURCE == DataSource.NETWORK) {
    	// Listen for instructions on some port
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
      System.out.println("Usage: java SoftballSim <LineupType> <gamesToSimulate default=10000> <inningsToSimulate default=7>");
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
}
