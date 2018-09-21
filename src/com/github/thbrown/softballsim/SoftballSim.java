package com.github.thbrown.softballsim;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;

import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineupgen.LineupGenerator;
import com.github.thbrown.softballsim.lineupgen.LineupType;

public class SoftballSim {
  // Config -- TODO convert these to flags w/ defaults
  public static int GAMES_TO_SIMULATE = 10000000;
  public static int INNINGS_PER_GAME = 7;
  public static boolean VERBOSE = false;
  // Minus one so you can still do things while it's running.
  public static final int THREADS_TO_USE = Runtime.getRuntime().availableProcessors() - 1;
  public static final int NAME_PADDING = 24; // Just for formatting verbose output
  public static final String STATS_FILE_PATH =
      System.getProperty("user.dir") + File.separator + "stats";

  public static void main(String[] args) {
    // Args
    validateArgs(args);
    GAMES_TO_SIMULATE = args.length >= 2 ? Integer.parseInt(args[1]) : GAMES_TO_SIMULATE;
    INNINGS_PER_GAME = args.length >= 3 ? Integer.parseInt(args[2]) : INNINGS_PER_GAME;
    
    LineupGenerator generator = getLineupGenerator(args[0]);
    generator.readDataFromFile(STATS_FILE_PATH);
    
    // Build a list of simulations each with one lineup
    ProgressTracker tracker = new ProgressTracker();
    List<Simulation> simulations = new ArrayList<>();
    BattingLineup lineup;
    while ((lineup = generator.getNextLineup()) != null) {
      Simulation s = new Simulation(lineup, GAMES_TO_SIMULATE, tracker);
      simulations.add(s);
    }
    long numberOfLineupsToTest = simulations.size();
    tracker.init(numberOfLineupsToTest, 100);

    // Print the details before we start
    DecimalFormat formatter = new DecimalFormat("#,###");
    System.out.println("*********************************************************************");
    System.out.println("Possible lineups: \t\t" + formatter.format(numberOfLineupsToTest));
    System.out.println("Games to simulate per lineup: \t" + GAMES_TO_SIMULATE);
    System.out.println("Innings per game: \t\t" + INNINGS_PER_GAME);
    System.out.println("Threads used: \t\t\t" + THREADS_TO_USE);
    System.out.println("*********************************************************************");

    // Run all the simulations using the specified number of threads
    ExecutorService executor = Executors.newFixedThreadPool(THREADS_TO_USE);
    List<Future<Double>> results = new ArrayList<>(simulations.size());
    long startTime = System.currentTimeMillis();

    try {
      results = executor.invokeAll(simulations);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    
    Map<Integer, Integer> histo = new HashMap<>();

    // Iterate over all the results, choosing the lineup with the highest score
    double bestResult = 0;
    BattingLineup bestLineup = null;
    int index = 0;
    try {
      for(Future<Double> result : results) {
        // Update histogram
        int key = (int)(result.get()*10);
        if(histo.containsKey(key)) {
          histo.put(key, histo.get(key)+1);
        } else {
          histo.put(key, 1);
        }
        
        if (result.get() > bestResult) {
          bestResult = result.get();
          bestLineup = simulations.get(index).getLineup();
        }
        index++;
      }
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }

    System.out.println();
    System.out.println("Best lineup");
    System.out.println(bestLineup);
    System.out.println("Best lineup mean runs scored: " + bestResult);
    System.out.println("Simulation took " + (System.currentTimeMillis() - startTime) + " milliseconds.");
    for(Integer k : histo.keySet()) {
      System.out.println(k/10.0 + " - " + histo.get(k));
    }
    System.exit(0);
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
      System.out.println("Usage: java " + getApplicationName() + " <LineupType> <gamesToSimulate default=10000> <inningsToSimulate default=7>");
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

  public static String getApplicationName() {
    // TODO: get simple class name from
    // Thread.currentThread().getStackTrace()[2].getClassName());
    return "SoftballSim";
  }
}
