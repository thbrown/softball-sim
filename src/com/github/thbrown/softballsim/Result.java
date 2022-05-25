package com.github.thbrown.softballsim;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import com.github.thbrown.softballsim.data.gson.helpers.DataPlayerLookupImpl;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloExhaustiveResult;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.thbrown.softballsim.util.Logger;


/**
 * This class contains the output of an optimization. It's used for reporting to the end user as
 * well as for the resumption of a partially complete optimization. It may be a final result or it
 * may contain a partial result for an incomplete optimization.
 * 
 * Instance of this class are shared between threads by
 * {@link com.github.thbrown.softballsim.datasource.ProgressTracker}
 * 
 * Optimizer implementations can store additional result information in the instance variable of the
 * implementing class. Subclasses should be careful to maintain immutability.
 * 
 * Unused fields are needed in the serialized result.
 */
@SuppressWarnings("unused")
public abstract class Result {

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

  // Make sure these match the actual variable name used above!
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
    this(optimizer, lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs, status, statusMessage, null);
  }

  public Result(OptimizerEnum optimizer, BattingLineup lineup, double lineupScore, long countTotal, long countCompleted,
      long elapsedTimeMs, ResultStatusEnum status, String statusMessage, Long estimatedTimeRemainingMs) {
    this.optimizer = optimizer;
    this.lineup = lineup;
    this.lineupScore = lineupScore;
    this.countTotal = countTotal;
    this.countCompleted = countCompleted;
    this.elapsedTimeMs = elapsedTimeMs;
    this.status = status;
    this.statusMessage = statusMessage;
    this.estimatedTimeRemainingMs = estimatedTimeRemainingMs;
  }

  public Result(OptimizerEnum optimizer, long duration) {
    this(optimizer, null, 0, 0, 0, 0, ResultStatusEnum.ESTIMATE, null, duration);
  }

  /**
   * Copy an existing Result but provide timeRemainingMs. This uses Gson serialization/deserialization
   * to maintain subclass status (i.e. If you call this on a MonteCarloExaustiveResult, you will get
   * back a MonteCarloExaustiveResult)
   */
  public final Result copyWithNewEstimatedTimeRemainingMs(Long estimatedTimeRemainingMs) {
    // Setup for serializing and deserializing BattingLineups (to make a copy)
    List<BattingLineup> battingLineups = getAllBattingLineupsInClassHierarchy();
    DataPlayerLookupImpl lookup = new DataPlayerLookupImpl(battingLineups);
    Gson g = GsonAccessor.getInstance().getCustomWithStatsLookup(lookup);

    // Perform the copy and alter estimatedTimeRemainingMs
    JsonObject obj = (JsonObject) g.toJsonTree(this);
    obj.addProperty(Result.ESTIMATED_TIME_VARIABLE_NAME, estimatedTimeRemainingMs);
    return g.fromJson(obj, this.getClass());
  }

  /**
   * Copy an existing Result but provide an updated status and statusMessages. This uses Gson
   * serialization/deserialization to maintain subclass identity (i.e. If you call this on a
   * MonteCarloExhaustiveResult, you will get back a MonteCarloExhaustiveResult)
   */
  public final Result copyWithNewStatus(ResultStatusEnum newStatus, String newStatusMessage) {
    // Setup for serializing and deserializing BattingLineups (to make a copy)
    List<BattingLineup> battingLineups = getAllBattingLineupsInClassHierarchy();
    DataPlayerLookupImpl lookup = new DataPlayerLookupImpl(battingLineups);
    Gson g = GsonAccessor.getInstance().getCustomWithStatsLookup(lookup);

    // Perform the copy and alter status and status message
    // Logger.log("Type " + this.getClass() + " " + newStatus);
    JsonObject obj = (JsonObject) g.toJsonTree(this);
    obj.addProperty(Result.STATUS_VARIABLE_NAME, newStatus.name());
    obj.addProperty(Result.STATUS_MESSAGE_VARIABLE_NAME, newStatusMessage);
    return g.fromJson(obj, this.getClass());
  }

  /**
   * Copy an existing Result but provide an updated status and statusMessages. This is a static method
   * that works on a result JSON string only, it never becomes a Result Java object.
   */
  public static String copyWithNewStatusStringOnly(String jsonResult, ResultStatusEnum newStatus,
      String newStatusMessage) {
    JsonObject obj = JsonParser.parseString(jsonResult).getAsJsonObject();
    obj.addProperty(Result.STATUS_VARIABLE_NAME, newStatus.name());
    obj.addProperty(Result.STATUS_MESSAGE_VARIABLE_NAME, newStatusMessage);
    return GsonAccessor.getInstance().getDefault().toJson(obj);
  }

  /**
   * @returns a list of all batting lineups stored as instance variables in this object
   */
  private List<BattingLineup> getAllBattingLineupsInClassHierarchy() {
    List<BattingLineup> battingLineups = new ArrayList<>();
    List<Field> fields = getAllFields(this.getClass());
    for (Field field : fields) {
      String name = field.getName();
      Object value;
      try {
        field.setAccessible(true);
        value = field.get(this);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
      // System.out.println(name + ": " + value.toString());
      if (value instanceof BattingLineup) {
        battingLineups.add((BattingLineup) value);
      }
    }
    return battingLineups;
  }

  // https://stackoverflow.com/questions/1042798/retrieving-the-inherited-attribute-names-values-using-java-reflection
  private static List<Field> getAllFields(Class<?> type) {
    List<Field> fields = new ArrayList<Field>();
    for (Class<?> c = type; c != null; c = c.getSuperclass()) {
      fields.addAll(Arrays.asList(c.getDeclaredFields()));
    }
    return fields;
  }

  @Override
  public final String toString() {
    final int INDENT = 3;
    String percentage = StringUtils.formatDecimal((double) getCountCompleted() / (double) getCountTotal() * 100, 2);
    return "Optimal lineup:\n"
        + Optional.ofNullable(lineup).map(v -> v.toString()).map(v -> StringUtils.indent(v, INDENT)).orElse("-") + "\n"
        + "Optimal lineup avg score: \n"
        + StringUtils.padLeft("", INDENT) + StringUtils.formatDecimal(this.lineupScore, 2) + " runs per game\n"
        + "Details:\n"
        + StringUtils.indent(getHumanReadableDetails(), INDENT) + "\n"
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

  public String getStatusMessage() {
    return statusMessage;
  }

  public Long getEstimatedTimeRemainingMs() {
    return estimatedTimeRemainingMs;
  }

  // Derived fields, these are included in the serialized result by direction of
  // ResultDeserializerSerializer
  // They are ignored on deserialization of JSON data

  public String getHumanReadableDetails() {
    StringBuilder sb = new StringBuilder();
    if (this.lineup != null && this.lineup.getDisplayInfo() != null) {
      sb.append("Lineup Info:\n");
      sb.append(StringUtils.indent(this.lineup.getDisplayInfo(), 1));
      sb.append("\n");
      sb.append("\n");
    }
    return sb.toString();
  }

  public List<String> getFlatLineup() {
    return lineup == null ? null : lineup.asListOfIds();
  }
}
