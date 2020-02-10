package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.Set;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.util.StringUtils;

/**
 * 
 * 
 * SummaryStatistics are not serializable (https://bugs.openjdk.java.net/browse/JDK-8043747)
 *
 */
public class MonteCarloAdaptiveResult extends Result {

  private final Set<Long> candidateLineups;
  private long simulationsRequired;

  public MonteCarloAdaptiveResult(BattingLineup lineup, double lineupScore, long countTotal, long countCompleted,
      long elapsedTimeMs, Set<Long> candidateLineups, long simulationsRequired) {
    super(lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs);
    this.candidateLineups = candidateLineups;
    this.simulationsRequired = simulationsRequired;
  }

  public Set<Long> getCandidateLineups() {
    return candidateLineups;
  }

  public String toString() {
    String base = super.toString();
    return base + "\nAvg simulations per lineup: " + StringUtils.formatDecimal(((double)simulationsRequired/(double)super.getCountCompleted()),2);
  }
}
