package com.github.thbrown.softballsim;

import java.util.Optional;
import com.github.thbrown.softballsim.lineup.BattingLineup;

/**
 * This class contains the output of an optimization. It's used for reporting to the end user as
 * well as for the resumption of a partially complete optimization. It may be a final result or it
 * may contain a partial result for an incomplete optimization.
 * 
 * This class is immutable as instance are shared between threads by
 * {@link com.github.thbrown.softballsim.datasource.ProgressTracker}
 * 
 * Optimizer implementations may need to store additional information, if so, implementers can
 * extend this extend this class. Subclasses should be careful to maintain immutability.
 */
public class Result {
  private final BattingLineup lineup;
  private final double lineupScore;
  private final long countTotal;
  private final long countCompleted;
  private final long elapsedTimeMs;

  public Result(BattingLineup lineup, double lineupScore, long countTotal, long countCompleted, long elapsedTimeMs) {
    this.lineup = lineup;
    this.lineupScore = lineupScore;
    this.countTotal = countTotal;
    this.countCompleted = countCompleted;
    this.elapsedTimeMs = elapsedTimeMs;
  }

  @Override
  public String toString() {
    return "Optimal lineup: \n"
        + Optional.ofNullable(lineup).map(v -> v.toString()).orElse("null") + "\n"
        + "Lineup expected score: " + this.lineupScore + "\n"
        + getHumanReadableDetails() + "\n"
        + "Elapsed time (ms): " + this.elapsedTimeMs;
  }

  public double getLineupScore() {
    return lineupScore;
  }

  public long getCountTotal() {
    return countTotal;
  }

  public long getCountCompleted() {
    return countCompleted;
  }

  public long getElapsedTimeMs() {
    return elapsedTimeMs;
  }

  public BattingLineup getLineup() {
    return lineup;
  }

  public String getHumanReadableDetails() {
    return "";
  }
}
