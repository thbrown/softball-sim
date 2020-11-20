package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.List;
import java.util.Set;

public class TTestTaskWithBestLineup extends TTestTask {

  SynchronizedLineupCompositeWrapper overallBestLineup;
  long newLineupsAdded;

  public TTestTaskWithBestLineup(SynchronizedLineupCompositeWrapper bestLineup, List<LineupComposite> toTest,
      int inningsPerGame, double alpha, long newLineupsAdded) {
    super(toTest, inningsPerGame, alpha);
    this.overallBestLineup = bestLineup;
    this.newLineupsAdded = newLineupsAdded;
  }

  @Override
  public TTestTaskResult call() {

    // { Before we do the t-test, get a copy of the best lineup so far and add it to the list of lineups
    // to be t-tested }
    LineupComposite bestLineupBeforeSimulations = overallBestLineup.getCopyOfBestLineupComposite();
    super.toTest.add(0, bestLineupBeforeSimulations);

    // Run the ttest!
    TTestTaskResult result = super.call();

    // We have the result! Check to see if the best lineup from the ttest matches overallBestLineup, if
    // so add all the sampling data we just calculated to overallBestLineup
    boolean wasUpdated = overallBestLineup.updateIfEqual(result.getBestLineupComposite());

    // Since there will be no change to the overallBestLineup, we'll return a null best lineup to
    // indicate no change is required
    if (wasUpdated) {
      return new TTestTaskResultWithNewLineups(null, result.getEliminatedLineupComposites(),
          result.getSimulationsRequired(), this.newLineupsAdded);
    }

    if (bestLineupBeforeSimulations.equals(result.getBestLineupComposite())) {
      // The overallBestLineup must have changed while we were running this task's simulations, since all
      // the lineups in this task are worse than the
      // bestLineupBeforeSimulations and bestLineupBeforeSimulations is worse than some other linuep, none
      // of the lineups in this task can be the best
      // lineup.
      return new TTestTaskResultWithNewLineups(null, result.getEliminatedLineupComposites(),
          result.getSimulationsRequired(), this.newLineupsAdded);
    } else {
      // Our ttest found a lineup which was better than bestLineupBeforeSimulations! Return this result so
      // either the overallBestLineup will be updated, or
      // the result lineup will continue to be evaluated against the current overallBestLineup.
      return new TTestTaskResultWithNewLineups(result.getBestLineupComposite(), result.getEliminatedLineupComposites(),
          result.getSimulationsRequired(), this.newLineupsAdded);
    }

  }

}
