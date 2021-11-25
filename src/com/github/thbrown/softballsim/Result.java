package com.github.thbrown.softballsim;

import java.util.List;
import java.util.Optional;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloExhaustiveResult;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.github.thbrown.softballsim.util.Logger;


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

  public static final String HUMAN_READABLE = "humanReadableDetails";
  public static final String FLAT_LINEUP = "flatLineup";

  private final OptimizerEnum optimizer;
  private final BattingLineup lineup;
  private final double lineupScore;
  private final long countTotal;
  private final long countCompleted;
  private final long elapsedTimeMs;
  private final ResultStatusEnum status;
  private final String statusMessage;
  private final Long estimatedTimeRemainingMs;

  // Make sure these match teh actual variable name used above!
  // TODO: test these are valid in unit test with this.getClass().getDeclaredFields()
  private final static String ESTIMATED_TIME_VARIABLE_NAME = "estimatedTimeRemainingMs";
  private final static String STATUS_VARIABLE_NAME = "status";
  private final static String STATUS_MESSAGE_VARIABLE_NAME = "statusMessage";

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
   * Copy an existing Result but provide timeRemainingMs. This uses Gson serialization/deserialization
   * to maintain subclass status (i.e. If you call this on a MonteCarloExaustiveResult, you will get
   * back a MonteCarloExaustiveResult)
   */
  public final Result copyWithNewEstimatedTimeRemainingMs(Long estimatedTimeRemainingMs) {
    Gson g = GsonAccessor.getInstance().getCustom();
    JsonObject obj = (JsonObject) g.toJsonTree(this);
    obj.addProperty(Result.ESTIMATED_TIME_VARIABLE_NAME, estimatedTimeRemainingMs);

    Result toReturn = g.fromJson(obj, this.getClass());
    if (this.getLineup() != null) {
      // Re-populate player's stats (these do not get serilized)
      toReturn.getLineup().populateStats(this.getLineup().asList());
    }
    return toReturn;
  }

  /**
   * Copy an existing Result but provide an updated status and statusMessages. This uses Gson
   * serialization/deserialization to maintain subclass status (i.e. If you call this on a
   * MonteCarloExaustiveResult, you will get back a MonteCarloExaustiveResult)
   */
  public final Result copyWithNewStatus(ResultStatusEnum newStatus, String newStatusMessage) {
    Gson g = GsonAccessor.getInstance().getCustom();
    JsonObject obj = (JsonObject) g.toJsonTree(this);

    obj.addProperty(Result.STATUS_VARIABLE_NAME, newStatus.name());
    obj.addProperty(Result.STATUS_MESSAGE_VARIABLE_NAME, newStatusMessage);

    Result toReturn = g.fromJson(obj, this.getClass());
    if (this.getLineup() != null) {
      // Re-populate player's stats (these do not get serilized)
      toReturn.getLineup().populateStats(this.getLineup().asList());
    }
    return toReturn;
  }

  @Override
  public final String toString() {
    String percentage = StringUtils.formatDecimal((double) getCountCompleted() / (double) getCountTotal() * 100, 2);
    return "Optimal lineup: \n"
        + Optional.ofNullable(lineup).map(v -> v.toString()).orElse("null") + "\n"
        + "Lineup expected score: " + this.lineupScore + "\n"
        + "Details: " + getHumanReadableDetails() + "\n"
        + "Status: " + getStatus() + "\n"
        + "Progress: " + getCountCompleted() + "/" + getCountTotal() + " (" + percentage + "%)" + "\n"
        + "Elapsed time (ms): " + this.elapsedTimeMs + "\n"
        + "Estimated time remaining (ms): " + this.getEstimatedTimeRemainingMs();
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

  public ResultStatusEnum getStatus() {
    return status;
  }

  public Long getEstimatedTimeRemainingMs() {
    return estimatedTimeRemainingMs;
  }

  // Derived fields, these are included in the serialized result by direction of
  // ResultDeserializerSerializer
  // They are ignored on deserialization of JSON data

  public String getHumanReadableDetails() {
    return "N/A";
  }

  public List<String> getFlatLineup() {
    return lineup == null ? null : lineup.asListOfIds();
  }
}
