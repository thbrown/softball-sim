package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.List;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.stat.inference.TTest;

public class SynchronizationLineupCompositeWrapper {

  private final static TTest tester = new TTest();

  public LineupComposite best;

  public SynchronizationLineupCompositeWrapper(LineupComposite lc) {
    this.best = lc;
  }

  public boolean updateIfEqual(LineupComposite bestResult, List<Double> data) {
    synchronized (this) {
      if (bestResult.equals(best)) {
        for (double datum : data) {
          best.getStats().addValue(datum);
        }
        return true;
      }
      return false;
    }
  }

  public boolean replaceIfEqual(LineupComposite oldBestResult, LineupComposite newBestResult) {
    synchronized (this) {
      if (oldBestResult.equals(best)) {
        this.best = newBestResult;
        return true;
      }
    }
    return false;
  }

  public boolean replaceIfSuperior(LineupComposite newBestResult, double alpha) {

    synchronized (this) {
      try {
        double pValue = tester.tTest(best.getStats(), newBestResult.getStats());
        if (pValue <= alpha) {
          return false;
        }
      } catch (NumberIsTooSmallException e) {
        // Swallow this and replace the best
      }
      this.best = newBestResult;
    }
    return true;
  }

  public LineupComposite getCopyOfBestLineupComposite() {
    synchronized (this) {
      return new LineupComposite(best);
    }
  }
}
