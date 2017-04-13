package com.github.thbrown.softballsim;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineupgen.LineupGenerator;
import com.github.thbrown.softballsim.lineupgen.LineupType;

public class SoftballSim {
  // Config -- TODO convert these to flags w/ defaults
  public static final int INNINGS_PER_GAME = 6;
  public static final int GAMES_TO_SIMULATE = 10000;
  public static final boolean VERBOSE = false;
  public static final int THREADS_TO_USE = 5;
  public static final int NAME_PADDING = 24; // Just for formatting verbose output
  public static final String STATS_FILE_PATH =
      System.getProperty("user.dir") + File.separator + "stats";

  public static void main(String[] args) {

    // Args
    validateArgs(args);
    LineupGenerator generator = getLineupGenerator(args[0]);
    generator.readDataFromFile(STATS_FILE_PATH);

    System.out.println("*********************************************************************");
    System.out.println("Games simulated per lineup: " + GAMES_TO_SIMULATE);
    System.out.println("Innings per game: " + INNINGS_PER_GAME);
    System.out.println("*********************************************************************");

    
    // Build a list of simulations each with one lineup
    List<Simulation> simulations = new ArrayList<>();
    BattingLineup lineup;
    while ((lineup = generator.getNextLineup()) != null) {
      Simulation s = new Simulation(lineup, GAMES_TO_SIMULATE);
      simulations.add(s);
    }

    // Run all the simulations using the specified number of threads
    ExecutorService executor = Executors.newFixedThreadPool(THREADS_TO_USE);
    List<Future<Double>> results = null;
    try {
      results = executor.invokeAll(simulations);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Interate over all the results, choosing lineup with the highest score
    double bestResult = 0;
    BattingLineup bestLineup = null;
    int index = 0;
    long startTime = System.currentTimeMillis();
    try {
      for(Future<Double> result : results) {
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
      System.out.println("Usage: java " + getApplicationName() + " <LineupType>");
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
