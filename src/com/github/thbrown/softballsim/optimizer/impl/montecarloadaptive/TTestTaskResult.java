package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.Optional;
import java.util.Set;

public class TTestTaskResult {

  private LineupComposite bestLineupComposite;
  private Set<LineupComposite> eliminatedLineupComposites;
  private long simulationsRequired;

  public TTestTaskResult(LineupComposite bestLineupComposite, Set<LineupComposite> eliminatedLineupComposites,
      long simulationsRequired) {
    this.bestLineupComposite = bestLineupComposite;
    this.eliminatedLineupComposites = eliminatedLineupComposites;
    this.simulationsRequired = simulationsRequired;
  }

  public LineupComposite getBestLineupComposite() {
    return this.bestLineupComposite;
  }

  public Set<LineupComposite> getEliminatedLineupComposites() {
    return this.eliminatedLineupComposites;
  }

  public long getSimulationsRequired() {
    return simulationsRequired;
  }

  @Override
  public String toString() {
    return bestLineupComposite.getStats().getMean() + " "
        + Optional.ofNullable(bestLineupComposite.getLineup()).map(v -> v.toString()).orElse("-");
  }
}
