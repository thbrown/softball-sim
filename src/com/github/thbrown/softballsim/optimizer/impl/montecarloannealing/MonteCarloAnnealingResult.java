package com.github.thbrown.softballsim.optimizer.impl.montecarloannealing;

import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;

public class MonteCarloAnnealingResult extends Result {

  public MonteCarloAnnealingResult(BattingLineup lineup, double lineupScore, long countTotal,
      long countCompleted, long elapsedTimeMs, ResultStatusEnum status) {
    super(OptimizerEnum.MONTE_CARLO_ANNEALING, lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs, status);
    // No additional behavior for now

  }
}
