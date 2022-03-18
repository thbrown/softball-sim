package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.Set;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.StringUtils;

/**
 * SummaryStatistics are not serializable (https://bugs.openjdk.java.net/browse/JDK-8043747)
 */
public class MonteCarloAdaptiveResult extends Result {

  private final Set<Long> candidateLineups;
  private long simulationsRequired;
  private long comparisonsThatReachedSimLimit;

  public MonteCarloAdaptiveResult(BattingLineup lineup, double lineupScore, long countTotal, long countCompleted,
      long elapsedTimeMs, Set<Long> candidateLineups, ResultStatusEnum status, long simulationsRequired,
      long comparisonsThatReachedSimLimit) {
    super(OptimizerEnum.MONTE_CARLO_ADAPTIVE, lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs, status);
    this.candidateLineups = candidateLineups;
    this.simulationsRequired = simulationsRequired;
    this.comparisonsThatReachedSimLimit = comparisonsThatReachedSimLimit;
  }

  public MonteCarloAdaptiveResult(long estimatedCompletionTimeMs) {
    super(OptimizerEnum.MONTE_CARLO_ADAPTIVE, null, 0, 0, 0, 0, ResultStatusEnum.ESTIMATE, null,
        estimatedCompletionTimeMs);
    this.candidateLineups = null;
    this.simulationsRequired = 0;
    this.comparisonsThatReachedSimLimit = 0;
  }

  public Set<Long> getCandidateLineups() {
    return candidateLineups;
  }

  public Long getSimulationsRequired() {
    return this.simulationsRequired;
  }

  public Long getComparisonsThatReachedSimLimit() {
    return this.comparisonsThatReachedSimLimit;
  }

  @Override
  public String getHumanReadableDetails() {
    StringBuilder sb = new StringBuilder(super.getHumanReadableDetails());
    sb.append("Avg simulations run per lineup: ");
    sb.append(StringUtils.formatDecimal(((double) this.simulationsRequired / (double) super.getCountCompleted()), 2));
    sb.append("\n");
    sb.append("% of indeterminate comparisons: ");
    sb.append(StringUtils
        .formatDecimal(((double) this.comparisonsThatReachedSimLimit / (double) super.getCountCompleted() * 100), 3));
    return sb.toString();
  }
}
