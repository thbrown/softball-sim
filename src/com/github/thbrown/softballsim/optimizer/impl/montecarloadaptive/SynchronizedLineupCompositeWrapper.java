package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.inference.TTest;
import com.github.thbrown.softballsim.lineup.BattingLineup;

public class SynchronizedLineupCompositeWrapper {

  private final static TTest tester = new TTest();

  public LineupComposite best;

  public SynchronizedLineupCompositeWrapper(LineupComposite lc) {
    this.best = lc;
  }

  public boolean replaceIfCurrentIsInCollection(LineupComposite newBestResult, Collection eliminationCollection) {
    synchronized (this) {
      if (eliminationCollection.contains(best)) {
        this.best = new LineupComposite(newBestResult);
        return true;
      }
    }
    return false;
  }

  public LineupComposite getCopyOfBestLineupComposite() {
    synchronized (this) {
      return new LineupComposite(best);
    }
  }

  /**
   * If the lineups are the same, merge the stats objects
   */
  public boolean updateIfEqual(LineupComposite bestResult) {
    StatisticalSummary additionalData = bestResult.getStats();
    synchronized (this) {
      if (bestResult.equals(best)) {
        bestResult.incorperateAdditionalStats(additionalData);
        return true;
      }
      return false;
    }
  }

}
