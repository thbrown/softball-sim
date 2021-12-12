package com.github.thbrown.softballsim.optimizer.impl.expectedvalue;

import java.util.Map;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloExhaustiveResult;

public class ExpectedValueResult extends MonteCarloExhaustiveResult {

  public ExpectedValueResult(BattingLineup lineup, double lineupScore, long countTotal, long countCompleted,
      long elapsedTimeMs, Map<Long, Long> histogram, ResultStatusEnum isFinalResult,
      BattingLineup oppositeOfOptimalLineup, double oppositeOfOptimalScore) {
    super(OptimizerEnum.EXPECTED_VALUE, lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs,
        histogram, isFinalResult, oppositeOfOptimalLineup, oppositeOfOptimalScore);
  }

}
