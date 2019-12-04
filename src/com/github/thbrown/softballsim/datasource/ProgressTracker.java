package com.github.thbrown.softballsim.datasource;

import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.util.CircularArray;
import com.github.thbrown.softballsim.util.Logger;

/**
 * Define how the application provides updates on progress to the user.
 */
public abstract class ProgressTracker implements Runnable {

  private final static int UPDATE_PERIOD_MS = 5000;
  private final static int RESULTS_SIZE = 5;

  // These variable are written by one thread an read by another
  private CircularArray<Result> results = new CircularArray<>(RESULTS_SIZE);
  private Long estimatedSecondsRemaining = null;

  public ProgressTracker(Result initialResult) {
    results.add(initialResult);
  }

  public void updateProgress(Result updatedResult) {
    synchronized (this) {
      results.add(updatedResult);
    }
  }

  @Override
  public void run() {
    // The parent thread interrupts this progress updater thread when the progress updater is no longer
    // needed
    while (!Thread.interrupted()) {
      Result oldResult;
      Result recentResult;
      synchronized (this) {
        oldResult = results.earliest();
        recentResult = results.get(0);
      }

      if (oldResult != null && recentResult != null && !recentResult.equals(oldResult)) {
        long calculationsDoneBetweenUpdates = recentResult.getCountCompleted() - oldResult.getCountCompleted();
        long timeBetweenUpdates = recentResult.getElapsedTimeMs() - oldResult.getElapsedTimeMs();
        long remainingCalculations = recentResult.getCountTotal() - recentResult.getCountCompleted();
        double rate = ((double) calculationsDoneBetweenUpdates) / ((double) timeBetweenUpdates / 1000.0);
        this.estimatedSecondsRemaining = (long) (remainingCalculations / rate);
      }

      this.onUpdate();
      try {
        Thread.sleep(UPDATE_PERIOD_MS);
      } catch (InterruptedException e) {
        break;
      }
    }
    this.onComplete();
    Logger.log("Updater Completed");
  }

  public Result getCurrentResult() {
    synchronized (this) {
      return results.get(0);
    }
  }

  public Long getEstimatedMsRemaining() {
    synchronized (this) {
      if (estimatedSecondsRemaining != null) {
        return new Long(estimatedSecondsRemaining);
      }
      return null;
    }
  }

  public abstract void onUpdate();

  // TODO: Do we want this here? Or somewhere else?
  public abstract void onComplete();

}
