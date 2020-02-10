package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.Map;

public class MonteCarloAdaptiveArgumentParser {

  public final static String INNINGS = "i";
  public final static String LOWEST_SCORE = "l";
  public final static String THREADS = "t";
  public final static String ALPHA = "a";

  public final static String THREADS_DEFAULT_TOKEN = "$getLogicalCPUCores";

  private final int innings;
  private final boolean lowestScore;
  private final int threads;
  private final double alpha;

  public MonteCarloAdaptiveArgumentParser(Map<String, String> args) {
    innings = Integer.parseInt(args.get(INNINGS));
    lowestScore = Boolean.parseBoolean(args.get(LOWEST_SCORE));
    alpha = Double.parseDouble(args.get(ALPHA));

    String threadsString = args.get(THREADS);
    if (threadsString.equals(THREADS_DEFAULT_TOKEN)) {
      threads = Runtime.getRuntime().availableProcessors();
    } else {
      threads = Integer.parseInt(args.get(THREADS));
    }
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

  public double getAlpha() {
    return alpha;
  }

}
