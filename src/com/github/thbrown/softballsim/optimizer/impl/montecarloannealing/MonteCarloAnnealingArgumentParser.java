package com.github.thbrown.softballsim.optimizer.impl.montecarloannealing;

import java.util.Map;

public class MonteCarloAnnealingArgumentParser {

  public final static String DURATION = "D";
  public final static String INNINGS = "I";
  public final static String LOWEST_SCORE = "L";

  private final int duration;
  private final int innings;
  private final boolean lowestScore;

  public MonteCarloAnnealingArgumentParser(Map<String, String> args) {
    duration = Integer.parseInt(args.get(DURATION));
    innings = Integer.parseInt(args.get(INNINGS));
    lowestScore = Boolean.parseBoolean(args.get(LOWEST_SCORE));
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
}
