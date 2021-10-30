package com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.HistogramUtils;

public class MonteCarloExhaustiveResult extends Result {

  private final Map<Long, Long> histogram;
  private double worstScore;

  public MonteCarloExhaustiveResult(BattingLineup lineup, double lineupScore, long countTotal, long countCompleted,
      long elapsedTimeMs, Map<Long, Long> histogram, double worstScore, ResultStatusEnum status) {
    this(OptimizerEnum.MONTE_CARLO_EXHAUSTIVE, lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs,
        histogram, worstScore, status);
  }

  protected MonteCarloExhaustiveResult(OptimizerEnum optimizerEnum, BattingLineup lineup, double lineupScore,
      long countTotal, long countCompleted, long elapsedTimeMs, Map<Long, Long> histogram, double worstScore,
      ResultStatusEnum status) {
    super(optimizerEnum, lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs, status);
    this.histogram = histogram;
    this.worstScore = worstScore;
  }

  public Map<Long, Long> getHistogram() {
    return histogram;
  }

  public double getWorstScore() {
    return worstScore;
  }

  @Override
  public String getHumanReadableDetails() {
    StringBuilder sb = new StringBuilder();
    sb.append("\n");
    sb.append("Runs | # Lineups | Histogram" + System.lineSeparator());

    Map<String, Long> formattedHistogram = new LinkedHashMap<>();
    List<Long> sortedKeys = new ArrayList<>(histogram.keySet());
    Collections.sort(sortedKeys);
    for (Long key : sortedKeys) {
      String keyString = key.toString();
      formattedHistogram.put(
          keyString.substring(0, keyString.length() - 1) + "." + keyString.substring(keyString.length() - 1),
          histogram.get(key));
    }
    sb.append(HistogramUtils.buildHistogram(formattedHistogram, 23, "█"));
    return sb.toString();
  }
}
