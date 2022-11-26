package com.github.thbrown.softballsim.datasource;

import org.apache.commons.cli.CommandLine;
import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.util.CircularArray;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.optimizer.EmptyResult;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;

/**
 * Maintains a record of an optimization's partial results. When running, sends results on a time
 * interval to DataSource functions that have been provided during construction.
 * 
 * This class must be thread safe as it is accessed by thread(s) that update the progress and a
 * separate thread that reports the progress.
 * 
 * TODO: It might be good to split the thread safe data storage function of this class and the
 * update thread function into separate classes.
 */
public final class ProgressTracker {
  // This should be at least as big as the number of threads, a larger value will give a more
  // accurate time estimation at the expense of a bigger memory footprint
  private final static int RESULTS_BUFFER_SIZE = 512;

  // Multiple threads have read/write access to 'results' all use must sync on
  // this class - used for determining estimating run used a sampling of recent
  // results. All results may not be included in this array!
  private CircularArray<Result> results = new CircularArray<>(RESULTS_BUFFER_SIZE);

  // This will always hold the most recent result, sync on all read/write access
  private Result mostRecentResult;

  private Long estimatedSecondsRemaining = null;
  private Long estimatedSecondsTotal = null;

  private final CommandLine cmd;
  private final DataStats stats;

  private final DataSourceEnum dataSource;

  private final long updateInterval;

  public ProgressTracker(Result initialResult, DataSourceEnum dataSource, CommandLine cmd, DataStats stats,
      OptimizerEnum optimizer) {
    this.cmd = cmd;
    this.stats = stats;
    this.dataSource = dataSource;

    // Get update interval from cmd line flags
    String updateIntervalString = cmd.getOptionValue(CommandLineOptions.UPDATE_INTERVAL,
        CommandLineOptions.UPDATE_INTERVAL_DEFAULT);
    this.updateInterval = Long.parseLong(updateIntervalString);

    if (initialResult == null) {
      initialResult = new EmptyResult(optimizer, ResultStatusEnum.NOT_STARTED);
    }
    updateResults(initialResult);
  }

  // This method is called from other threads
  public void updateProgress(Result updatedResult) {
    updateResults(updatedResult);
  }

  private void updateResults(Result updatedResult) {
    Result oldResult;
    synchronized (this) {
      oldResult = results.earliest();
    }

    // If we have two or more different results, we can update the estimated
    // completion time
    // TODO: Some kind of non-linear regression to improve estimation times?
    if (oldResult != null && updatedResult != null && !updatedResult.equals(oldResult)) {

      // Don't save results where the difference in time is less then the resolution
      // of the system. This messes up completion time estimates. If this is the final
      // result though, record it anyway.
      if (oldResult.getElapsedTimeMs() == updatedResult.getElapsedTimeMs()) {
        return;
      }

      long calculationsDoneBetweenUpdates = updatedResult.getCountCompleted() - oldResult.getCountCompleted();
      long timeBetweenUpdates = updatedResult.getElapsedTimeMs() - oldResult.getElapsedTimeMs();
      long remainingCalculations = updatedResult.getCountTotal() - updatedResult.getCountCompleted();
      double rate = ((double) calculationsDoneBetweenUpdates) / ((double) timeBetweenUpdates);

      // Logger.log("rate " + rate + " " + calculationsDoneBetweenUpdates + " " +
      // timeBetweenUpdates + " "
      // + (long) (remainingCalculations / rate));

      // Pre-calculated to avoid holding the lock while doing the math
      long remainingMs = (long) (remainingCalculations / rate);
      long msTotal = remainingMs + (updatedResult.getElapsedTimeMs());

      updatedResult = updatedResult.copyWithNewEstimatedTimeRemainingMs(remainingMs);

      synchronized (this) {
        this.estimatedSecondsRemaining = remainingMs / 1000;
        this.estimatedSecondsTotal = msTotal / 1000;
        this.mostRecentResult = updatedResult;
        results.add(updatedResult);
      }

    } else {
      // Otherwise, we'll just update the list of results
      synchronized (this) {
        this.mostRecentResult = updatedResult;
        results.add(updatedResult);
      }
    }

  }

  public Result run() {
    try {
      while (!Thread.interrupted()) {
        try {
          Thread.sleep(updateInterval);
        } catch (InterruptedException e) {
          break;
        }
        dataSource.onUpdate(cmd, stats, this);

        // If the optimization is in a terminal state, we don't need ProgressTracker
        Result recentResult = this.getCurrentResult();
        if (recentResult != null && recentResult.getStatus().isTerminal()) {
          Logger.log("Halting progress tracker, optimization in terminal status: " + recentResult.getStatus());
          break;
        }

        // Exit this thread if the halt flag is set
        String control = dataSource.getControlFlag(cmd, stats);
        if (control != null && control.equals("HALT")) {
          // Make sure onUpdate will use the result with the PAUSED status
          synchronized (this) {
            // TODO: what if this is still the initial result? (this can happen if a pause
            // occurs quickly after starting)
            this.updateProgress(this.getCurrentResult().copyWithNewStatus(ResultStatusEnum.PAUSED, null));
            dataSource.onUpdate(cmd, stats, this);
            Logger.log(getCurrentResult().toString());
            Logger.log("Exiting, halt flag detected");
            return getCurrentResult();
          }
        }
      }
    } catch (Exception e) {
      Logger.log("An unhandled exception occurred in the ProgressTracker, halting program");
      Logger.log(e);
      System.exit(1);
    }
    return getCurrentResult();
  }

  public Result getCurrentResult() {
    synchronized (this) {
      return mostRecentResult;
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
