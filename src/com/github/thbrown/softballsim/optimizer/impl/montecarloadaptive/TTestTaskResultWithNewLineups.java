package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.Set;

/**
 * Desperate attempt to add the number of new lineups processed by the ttest in order to help us
 * maintain an accurate progress counter.
 */
public class TTestTaskResultWithNewLineups extends TTestTaskResult {

  private long newLineupsProcessed = 0;

  public TTestTaskResultWithNewLineups(LineupComposite bestLineupComposite,
      Set<LineupComposite> eliminatedLineupComposites,
      long simulationsRequired, long newTasksAdded) {
    super(bestLineupComposite, eliminatedLineupComposites, simulationsRequired);
    this.newLineupsProcessed = newTasksAdded;
  }

  public long getNewLineupsProcessed() {
    return newLineupsProcessed;
  }
}
