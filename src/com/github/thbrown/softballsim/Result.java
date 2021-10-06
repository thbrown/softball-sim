package com.github.thbrown.softballsim;

import java.util.Optional;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.StringUtils;

/**
 * This class contains the output of an optimization. It's used for reporting to the end user as
 * well as for the resumption of a partially complete optimization. It may be a final result or it
 * may contain a partial result for an incomplete optimization.
 * 
 * Instance of this class are shared between threads by
 * {@link com.github.thbrown.softballsim.datasource.ProgressTracker}
 * 
 * Optimizer implementations may need to store additional information, if so, implementers can
 * extend this class. Subclasses should be careful to maintain immutability.
 * 
 * Unused fields are needed in the serialized result.
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
  private final Long estimatedTimeRemainingMs;

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
    this.estimatedTimeRemainingMs = null;
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
    this.estimatedTimeRemainingMs = toCopy.estimatedTimeRemainingMs;
  }

  /**
   * Copy an existing Result but provide timeRemainingMs
   */
  public Result(Result toCopy, Long estimatedTimeRemainingMs) {
    this.optimizer = toCopy.optimizer;
    this.lineup = toCopy.lineup;
    this.lineupScore = toCopy.lineupScore;
    this.countTotal = toCopy.countTotal;
    this.countCompleted = toCopy.countCompleted;
    this.elapsedTimeMs = toCopy.elapsedTimeMs;
    this.status = toCopy.status;
    this.statusMessage = toCopy.statusMessage;
    this.estimatedTimeRemainingMs = estimatedTimeRemainingMs;
  }

  @Override
  public String toString() {
    String percentage = StringUtils.formatDecimal((double) getCountCompleted() / (double) getCountTotal() * 100, 2);
    return "Optimal lineup: \n" + Optional.ofNullable(lineup).map(v -> v.toString()).orElse("null") + "\n"
        + "Lineup expected score: " + this.lineupScore + "\n" + getHumanReadableDetails() + "\n" + "Status: "
        + getStatus() + "\n" + "Progress: " + getCountCompleted() + "/" + getCountTotal() + " (" + percentage + "%)"
        + "\n" + "Elapsed time (ms): " + this.elapsedTimeMs + "\n" + "Estimated time remaining (ms): "
        + this.getEstimatedTimeRemainingMs();
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

  public ResultStatusEnum getStatus() {
    return status;
  }

  public Long getEstimatedTimeRemainingMs() {
    return estimatedTimeRemainingMs;
  }
}
