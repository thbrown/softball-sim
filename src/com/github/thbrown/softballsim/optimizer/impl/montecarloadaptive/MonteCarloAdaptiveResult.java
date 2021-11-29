package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.Set;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.StringUtils;

/**
 * 
 * SummaryStatistics are not serializable (https://bugs.openjdk.java.net/browse/JDK-8043747)
 */
public class MonteCarloAdaptiveResult extends Result {

  private final Set<Long> candidateLineups;
  private long simulationsRequired;

  public MonteCarloAdaptiveResult(BattingLineup lineup, double lineupScore, long countTotal, long countCompleted,
      long elapsedTimeMs, Set<Long> candidateLineups, long simulationsRequired, ResultStatusEnum status) {
    super(OptimizerEnum.MONTE_CARLO_ADAPTIVE, lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs, status);
    this.candidateLineups = candidateLineups;
    this.simulationsRequired = simulationsRequired;
  }

  public Set<Long> getCandidateLineups() {
    return candidateLineups;
  }

  @Override
  public String getHumanReadableDetails() {
    return "Avg simulations per lineup: "
        + StringUtils.formatDecimal(((double) simulationsRequired / (double) super.getCountCompleted()), 2);
  }
}
