package com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive;

import java.util.Map;

public class MonteCarloExhaustiveArgumentParser {

  public final static String GAMES = "G";
  public final static String INNINGS = "I";
  public final static String LOWEST_SCORE = "L";
  public final static String THREADS = "T";

  private final long games;
  private final int innings;
  private final boolean lowestScore;
  private final int threads;

  public MonteCarloExhaustiveArgumentParser(Map<String, String> args) {
    games = Long.parseLong(args.get(GAMES));
    innings = Integer.parseInt(args.get(INNINGS));
    lowestScore = Boolean.parseBoolean(args.get(LOWEST_SCORE));

    String threadsString = args.get(THREADS);
    if (threadsString == null) {
      threads = Runtime.getRuntime().availableProcessors();
    } else {
      threads = Integer.parseInt(args.get(THREADS));
    }
  }

  public long getGames() {
    return games;
  }

  public int getInnings() {
    return innings;
  }

  public boolean isLowestScore() {
    return lowestScore;
  }

  public int getThreads() {
    return threads;
  }

}
