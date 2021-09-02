package com.github.thbrown.softballsim.optimizer.impl.expectedvalue;

import java.util.Map;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloExhaustiveResult;

public class ExpectedValueResult extends MonteCarloExhaustiveResult {

  public ExpectedValueResult(BattingLineup lineup, double lineupScore, long countTotal, long countCompleted,
      long elapsedTimeMs, Map<Long, Long> histogram, double worstScore, ResultStatusEnum isFinalResult) {
    super(OptimizerEnum.EXPECTED_VALUE, lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs,
        histogram, worstScore, isFinalResult);
  }

}
