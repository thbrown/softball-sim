package com.github.thbrown.softballsim.optimizer.impl.sortbyaverage;

import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.Result;

public class SortByAverageResult extends Result {

  public SortByAverageResult(BattingLineup lineup, double lineupScore, long countTotal,
      long countCompleted, long elapsedTimeMs, ResultStatusEnum status) {
    super(OptimizerEnum.SORT_BY_AVERAGE, lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs, status);
  }

  public SortByAverageResult(int timeMs) {
    super(OptimizerEnum.SORT_BY_AVERAGE, timeMs);
  }

}
