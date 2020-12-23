package com.github.thbrown.softballsim.optimizer.impl.expectedvalue;

import java.util.Map;

public class ExpectedValueArgumentParser {

  public final static String MAX_BATTERS = "b";
  public final static String INNINGS = "i";
  public final static String LOWEST_SCORE = "l";
  public final static String THREADS = "t";

  private final int batters;
  private final int innings;
  private final int threads;
  private final boolean lowestScore;

  public ExpectedValueArgumentParser(Map<String, String> args) {
    batters = Integer.parseInt(args.get(MAX_BATTERS));
    innings = Integer.parseInt(args.get(INNINGS));
    lowestScore = Boolean.parseBoolean(args.get(LOWEST_SCORE));
    threads = Integer.parseInt(args.get(THREADS));
  }

  public int getMaxBatters() {
    return batters;
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
