package com.github.thbrown.softballsim.commands;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import com.github.thbrown.softballsim.helpers.ProcessHooks;

public class CompleteOptimizationCommand extends BaseOptimizationCommand {

  private Map<String, String> histogram;
  private double score;
  private long total;
  private Map<String, List<String>> lineup;
  private long complete;
  private long elapsedTimeMs;

  @Override
  public boolean process(ProcessHooks ps, PrintWriter out) throws Exception {
    return ps.onComplete(this, out);
  }

  public long getElapsedTimeMs() {
    return elapsedTimeMs;
  }

  public Map<String, String> getHistogram() {
    return histogram;
  }

  public double getScore() {
    return score;
  }

  public long getTotal() {
    return total;
  }

  public Map<String, List<String>> getLineup() {
    return lineup;
  }

  public long getComplete() {
    return complete;
  }

}
