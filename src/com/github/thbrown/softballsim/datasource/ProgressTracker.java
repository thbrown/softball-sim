package com.github.thbrown.softballsim.datasource;

import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.util.CircularArray;
import com.github.thbrown.softballsim.util.Logger;

/**
 * Maintains a record of an optimization's partial results. When running, sends results on a time
 * interval to registered functions that have been provided on construction.
 * 
 * This class must be thread safe as it is accessed by thread(s) that update the progress and a
 * separate thread that reports the progress.
 * 
 * TODO: It might be good to split the thread safe data storage function of this class and the
 * update thread function into separate classes.
 */
public final class ProgressTracker implements Runnable {

  private final static int UPDATE_PERIOD_MS = 5000;

  // This should be at least as big as the number of threads, more will give better time estimation at
  // the expense of a bigger memory footprint
  private final static int RESULTS_BUFFER_SIZE = 128;

  // These variable are written by one thread an read by another
  private CircularArray<Result> results = new CircularArray<>(RESULTS_BUFFER_SIZE);
  private Long estimatedSecondsRemaining = null;
  private Long estimatedSecondsTotal = null;

  private final DataSourceFunctions functions;

  public ProgressTracker(Result initialResult, DataSourceFunctions functions) {
    this.functions = functions;
    updateFields(initialResult);
  }

  public void updateProgress(Result updatedResult) {
    updateFields(updatedResult);
  }

  private void updateFields(Result updatedResult) {
    Result oldResult;
    synchronized (this) {
      oldResult = results.earliest();
    }

    if (oldResult != null && updatedResult != null && !updatedResult.equals(oldResult)) {
      // If we have two different results, we can update the estimated completion times
      long calculationsDoneBetweenUpdates = updatedResult.getCountCompleted() - oldResult.getCountCompleted();
      long timeBetweenUpdates = updatedResult.getElapsedTimeMs() - oldResult.getElapsedTimeMs();
      long remainingCalculations = updatedResult.getCountTotal() - updatedResult.getCountCompleted();
      double rate = ((double) calculationsDoneBetweenUpdates) / ((double) timeBetweenUpdates / 1000.0);

      // Pre-calculated to avoid holding the lock while doing the math
      long remainingSeconds = (long) (remainingCalculations / rate);
      long secondsTotal = remainingSeconds + (updatedResult.getElapsedTimeMs() / 1000);
      synchronized (this) {
        this.estimatedSecondsRemaining = remainingSeconds;
        this.estimatedSecondsTotal = secondsTotal;
        results.add(updatedResult);
      }

    } else {
      // Otherwise, we'll just update the list of results
      synchronized (this) {
        results.add(updatedResult);
      }
    }

    // If we have a timeEstimate and the thread has been interrupted, cancel the rest of the updates??
    // TODO: What if multiple threads are calling this
  }

  @Override
  public void run() {
    // The parent thread interrupts this progress updater thread when the progress updater is no longer
    // needed
    while (!Thread.interrupted()) {
      try {
        Thread.sleep(UPDATE_PERIOD_MS);
      } catch (InterruptedException e) {
        break;
      }
      functions.onUpdate(this);
    }
  }

  public Result getCurrentResult() {
    synchronized (this) {
      return results.get(0);
    }
  }

  public Long getEstimatedSecondsRemaining() {
    synchronized (this) {
      return estimatedSecondsRemaining;
    }
  }

  public Long getEstimatedSecondsTotal() {
    synchronized (this) {
      return estimatedSecondsTotal;
    }
  }
}
