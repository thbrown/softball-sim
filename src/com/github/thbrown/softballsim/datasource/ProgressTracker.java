package com.github.thbrown.softballsim.datasource;

import org.apache.commons.cli.CommandLine;
import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.util.CircularArray;
import com.github.thbrown.softballsim.util.Logger;
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
  // This should be at least as big as the number of threads, more will give
  // better time estimation at
  // the expense of a bigger memory footprint
  private final static int RESULTS_BUFFER_SIZE = 512;

  // Multiple threads have read/write access to 'results' all use must sync on
  // this class - used for determining estimating run used a sampleing of recent
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
      initialResult = new Result(optimizer, null, 0, 0, 0, 0, ResultStatusEnum.NOT_STARTED);
    }
    updateResults(initialResult, true);
  }

  // This method is called from other threads
  public void updateProgress(Result updatedResult) {
    updateResults(updatedResult, false);
  }

  private void updateResults(Result updatedResult, boolean initialUpdate) {
    Result oldResult;
    synchronized (this) {
      mostRecentResult = updatedResult;
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

      updatedResult = new Result(updatedResult, remainingMs);

      synchronized (this) {
        this.estimatedSecondsRemaining = remainingMs / 1000;
        this.estimatedSecondsTotal = msTotal / 1000;
        mostRecentResult = updatedResult;
        results.add(updatedResult);
      }

    } else {
      // Otherwise, we'll just update the list of results
      synchronized (this) {
        results.add(updatedResult);
      }
    }

  }

  public Result run() {
    // The parent thread interrupts this progress updater thread when the progress
    // updater is no longer needed
    while (!Thread.interrupted()) {
      try {
        Thread.sleep(updateInterval);
      } catch (InterruptedException e) {
        break;
      }
      dataSource.onUpdate(cmd, stats, this);

      // Exit this thread if this is an estimate only run (since we've waited for
      // one updateInterval)
      boolean estimateOnly = cmd.hasOption(CommandLineOptions.ESTIMATE_ONLY);
      if (estimateOnly) {
        // Make sure onUpdate will use the result with the ESTIMATE status
        synchronized (this) {
          this.updateProgress(new Result(this.getCurrentResult(), ResultStatusEnum.ESTIMATE, null));
          dataSource.onUpdate(cmd, stats, this);
          Logger.log(getCurrentResult().toString());
          Logger.log("Exiting, estimate only");
          return getCurrentResult();
        }
      }

      // Exit this thread if the halt flag is set
      String control = dataSource.getControlFlag(cmd, stats);
      if (control != null && control.equals("HALT")) {
        // Make sure onUpdate will use the result with the PAUSED status
        synchronized (this) {
          this.updateProgress(new Result(this.getCurrentResult(), ResultStatusEnum.PAUSED, null));
          dataSource.onUpdate(cmd, stats, this);
          Logger.log(getCurrentResult().toString());
          Logger.log("Exiting, halt flag detected");
          return getCurrentResult();
        }
      }
    }
    Logger.log("Exiting, optimization ended");
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
