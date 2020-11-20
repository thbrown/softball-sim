package com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive;

import java.util.Map;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;

public class MonteCarloExhaustiveResult extends Result {

  private final Map<Long, Long> histogram;
  private double worstScore;

  public MonteCarloExhaustiveResult(BattingLineup lineup, double lineupScore, long countTotal, long countCompleted,
      long elapsedTimeMs, Map<Long, Long> histogram, double worstScore) {
    super(OptimizerEnum.MONTE_CARLO_EXHAUSTIVE, lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs);
    this.histogram = histogram;
    this.worstScore = worstScore;
  }

  public Map<Long, Long> getHistogram() {
    return histogram;
  }

  public double getWorstScore() {
    return worstScore;
  }
}
