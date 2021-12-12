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
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;

public class MonteCarloExhaustiveResult extends Result {

  private final Map<Long, Long> histogram;
  private final BattingLineup oppositeOfOptimalLineup;
  private final double oppositeOfOptimalScore;

  public MonteCarloExhaustiveResult(BattingLineup lineup, double lineupScore, long countTotal, long countCompleted,
      long elapsedTimeMs, Map<Long, Long> histogram, ResultStatusEnum status, BattingLineup oppositeOfOptimalLineup,
      double oppositeOfOptimalScore) {
    this(OptimizerEnum.MONTE_CARLO_EXHAUSTIVE, lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs,
        histogram, status, oppositeOfOptimalLineup, oppositeOfOptimalScore);
  }

  protected MonteCarloExhaustiveResult(OptimizerEnum optimizerEnum, BattingLineup lineup, double lineupScore,
      long countTotal, long countCompleted, long elapsedTimeMs, Map<Long, Long> histogram,
      ResultStatusEnum status, BattingLineup oppositeOfOptimalLineup, double oppositeOfOptimalScore) {
    super(optimizerEnum, lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs, status);
    this.histogram = histogram;
    this.oppositeOfOptimalLineup = oppositeOfOptimalLineup;
    this.oppositeOfOptimalScore = oppositeOfOptimalScore;
  }

  public Map<Long, Long> getHistogram() {
    return histogram;
  }

  public BattingLineup getOppositeOfOptimalLineup() {
    return this.oppositeOfOptimalLineup;
  }

  public double getOppositeOfOptimalScore() {
    return this.oppositeOfOptimalScore;
  }

  @Override
  public String getHumanReadableDetails() {
    final int INDENT = 1;
    StringBuilder sb = new StringBuilder(super.getHumanReadableDetails());

    sb.append("Histogram:\n");
    sb.append(StringUtils.padLeft("", INDENT) + "Runs | # Lineups | Histogram" + System.lineSeparator());
    Map<String, Long> formattedHistogram = new LinkedHashMap<>();
    List<Long> sortedKeys = new ArrayList<>(histogram.keySet());
    Collections.sort(sortedKeys);
    for (Long key : sortedKeys) {
      String keyString = key.toString();
      formattedHistogram.put(
          keyString.substring(0, keyString.length() - 1) + "." + keyString.substring(keyString.length() - 1),
          histogram.get(key));
    }
    sb.append(StringUtils.indent(HistogramUtils.buildHistogram(formattedHistogram, 18, "█"), INDENT));
    sb.append("\n");
    sb.append("\n");

    if (this.oppositeOfOptimalLineup != null) {
      String worstOrBest = this.oppositeOfOptimalScore > this.getLineupScore() ? "Best" : "Worst";
      sb.append(worstOrBest + " lineup: \n");
      sb.append(StringUtils.indent(this.getOppositeOfOptimalLineup().toString(), INDENT));
      sb.append("\n");
      sb.append("\n");
      sb.append(worstOrBest + " lineup avg score:\n");
      sb.append(StringUtils.padLeft("", INDENT) + StringUtils.formatDecimal(this.oppositeOfOptimalScore, 2)
          + " runs per game\n");
    }
    return sb.toString();
  }
}
