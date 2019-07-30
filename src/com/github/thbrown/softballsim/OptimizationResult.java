package com.github.thbrown.softballsim;

import java.util.List;
import java.util.Map;

public class OptimizationResult {

  private Result bestResult;
  private Map<Long, Long> histogram; // TODO int, long?
  private long elapsedTimeMs;
  
  OptimizationResult(Result bestResult, Map<Long, Long> histogram, long elapsedTimeMs) {
    this.bestResult = bestResult;
    this.histogram = histogram;
    this.elapsedTimeMs = elapsedTimeMs;
  }
  
  public Map<String, List<String>> getLineup() {
    return this.bestResult.getLineup().toMap();
  }
  
  public double getScore() {
    return this.bestResult.getScore();
  }
  
  public Map<Long, Long> getHistogram() {
    return this.histogram;
  }
  
  public long getElapsedTimeMs() {
    return elapsedTimeMs;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Best lineup");
    sb.append(System.lineSeparator());
    sb.append(bestResult.getLineup());
    sb.append(System.lineSeparator());
    sb.append("Best lineup mean runs scored: " + bestResult.getScore());
    sb.append(System.lineSeparator());
    for(Long k : histogram.keySet()) {
      sb.append(k/10.0 + " - " + histogram.get(k));
      sb.append(System.lineSeparator());
    }
    sb.append("Total simulation time: " + elapsedTimeMs + " milliseconds");
    return sb.toString();
  }

  
}
