package com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive;

import java.util.Map;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;

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
}
