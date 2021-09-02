package com.github.thbrown.softballsim;

import java.util.Optional;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;

/**
 * This class contains the output of an optimization. It's used for reporting to the end user as
 * well as for the resumption of a partially complete optimization. It may be a final result or it
 * may contain a partial result for an incomplete optimization.
 * 
 * This class is immutable as instances are shared between threads by
 * {@link com.github.thbrown.softballsim.datasource.ProgressTracker}
 * 
 * Optimizer implementations may need to store additional information, if so, implementers can
 * extend this class. Subclasses should be careful to maintain immutability.
 * 
 * Unused field needed in the serialized result.
 */
@SuppressWarnings("unused")
public class Result {
  private final OptimizerEnum optimizer;
  private final BattingLineup lineup;
  private final double lineupScore;
  private final long countTotal;
  private final long countCompleted;
  private final long elapsedTimeMs;
  private final ResultStatusEnum status;
  private final String statusMessage;

  public Result(OptimizerEnum optimizer, BattingLineup lineup, double lineupScore, long countTotal, long countCompleted,
      long elapsedTimeMs, ResultStatusEnum status) {
    this(optimizer, lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs, status, null);
  }

  public Result(OptimizerEnum optimizer, BattingLineup lineup, double lineupScore, long countTotal, long countCompleted,
      long elapsedTimeMs, ResultStatusEnum status, String statusMessage) {
    this.optimizer = optimizer;
    this.lineup = lineup;
    this.lineupScore = lineupScore;
    this.countTotal = countTotal;
    this.countCompleted = countCompleted;
    this.elapsedTimeMs = elapsedTimeMs;
    this.status = status;
    this.statusMessage = statusMessage;
  }

  /**
   * Copy an existing Result but provide an updated status and statusMessage
   */
  public Result(Result toCopy, ResultStatusEnum newStatus, String newStatusMessage) {
    this.optimizer = toCopy.optimizer;
    this.lineup = toCopy.lineup;
    this.lineupScore = toCopy.lineupScore;
    this.countTotal = toCopy.countTotal;
    this.countCompleted = toCopy.countCompleted;
    this.elapsedTimeMs = toCopy.elapsedTimeMs;
    this.status = newStatus;
    this.statusMessage = newStatusMessage;
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
