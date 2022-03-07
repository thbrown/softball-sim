package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.Optional;
import java.util.Set;

/**
 * The output of a TTestTask
 */
public class TTestTaskResult {

  private LineupComposite bestLineupComposite;
  private Set<LineupComposite> eliminatedLineupComposites;
  private long simulationsRequired;
  private long comparisonsThatReachedSimLimit;

  public TTestTaskResult(LineupComposite bestLineupComposite, Set<LineupComposite> eliminatedLineupComposites,
      long simulationsRequired, long comparisonsThatReachedSimLimit) {
    this.bestLineupComposite = bestLineupComposite;
    this.eliminatedLineupComposites = eliminatedLineupComposites;
    this.simulationsRequired = simulationsRequired;
    this.comparisonsThatReachedSimLimit = comparisonsThatReachedSimLimit;
  }

  public LineupComposite getBestLineupComposite() {
    return this.bestLineupComposite;
  }

  public Set<LineupComposite> getEliminatedLineupComposites() {
    return this.eliminatedLineupComposites;
  }

  /**
   * @return the number of monte carlo simulations the ttest performed in total to determine the best
   *         lineup.
   */
  public long getSimulationsRequired() {
    return this.simulationsRequired;
  }

  public long getComparisonsThatReachedSimLimit() {
    return this.comparisonsThatReachedSimLimit;
  }

  @Override
  public String toString() {
    return bestLineupComposite.getStats().getMean() + " "
        + Optional.ofNullable(bestLineupComposite.getLineup()).map(v -> v.toString()).orElse("-");
  }

}
