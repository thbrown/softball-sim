package com.github.thbrown.softballsim.optimizer.impl.montecarloannealing;

import java.util.Map;

public class MonteCarloAnnealingArgumentParser {

  public final static String DURATION = "D";
  public final static String INNINGS = "I";
  public final static String LOWEST_SCORE = "L";
  public final static String ALPHA = "A";
  public final static String THREADS = "T";

  private final int duration;
  private final int innings;
  private final boolean lowestScore;
  private final double alpha;
  private final int threads;

  public MonteCarloAnnealingArgumentParser(Map<String, String> args) {
    duration = Integer.parseInt(args.get(DURATION));
    innings = Integer.parseInt(args.get(INNINGS));
    alpha = Double.parseDouble(args.get(ALPHA));
    lowestScore = Boolean.parseBoolean(args.get(LOWEST_SCORE));

    String threadsString = args.get(THREADS);
    if (threadsString == null) {
      threads = Runtime.getRuntime().availableProcessors();
    } else {
      threads = Integer.parseInt(args.get(THREADS));
    }
  }

  public long getDuration() {
    return duration;
  }

  public int getInnings() {
    return innings;
  }

  public boolean isLowestScore() {
    return lowestScore;
  }

  public double getAlpha() {
    return alpha;
  }

  public int getThreads() {
    return threads;
  }
}
