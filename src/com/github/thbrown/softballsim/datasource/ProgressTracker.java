package com.github.thbrown.softballsim.datasource;

import org.apache.commons.cli.CommandLine;
import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.util.CircularArray;

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
  // This should be at least as big as the number of threads, more will give better time estimation at
  // the expense of a bigger memory footprint
  private final static int RESULTS_BUFFER_SIZE = 128;

  // These variable are written by one thread an read by another
  private CircularArray<Result> results = new CircularArray<>(RESULTS_BUFFER_SIZE); // For estimating run time over
                                                                                    // longer periods
  private Long estimatedSecondsRemaining = null;
  private Long estimatedSecondsTotal = null;

  private final CommandLine cmd;
  private final DataStats stats;

  private final DataSourceEnum functions;

  private final DataSourceEnum dataSource;

  private final long updateInterval;

  public ProgressTracker(Result initialResult, DataSourceEnum dataSource, CommandLine cmd, DataStats stats) {
    this.cmd = cmd;
    this.stats = stats;
    this.functions = dataSource;
    this.dataSource = dataSource;

    // Get update interfal from cmd line flags
    String updateIntervalString =
        cmd.getOptionValue(CommandLineOptions.UPDATE_INTERVAL, CommandLineOptions.UPDATE_INTERVAL_DEFAULT);
    this.updateInterval = Long.parseLong(updateIntervalString);

    if (initialResult == null) {
      initialResult = new Result(null, null, 0, 0, 0, 0, ResultStatusEnum.NOT_STARTED);
    }
    updateResults(initialResult, true);
  }

  public void updateProgress(Result updatedResult) {
    updateResults(updatedResult, false);
  }

  private void updateResults(Result updatedResult, boolean initialUpdate) {
    Result oldResult;
    synchronized (this) {
      oldResult = results.earliest();
    }

    if (oldResult != null && updatedResult != null && !updatedResult.equals(oldResult)) {
      // If we have two different results, we can update the estimated completion times (Edit: can't you
      // determine this from a single Result?)
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

    // Exit the application if the halt flag is set
    String control = dataSource.getControlFlag(cmd, stats);
    if (control != null && control.equals("HALT")) {
      System.exit(0); // Should we have a non-zero code?
    }

    // Exit the application if this is an estimate only run and this isn't the first update
    boolean estimateOnly = cmd.hasOption(CommandLineOptions.ESTIMATE_ONLY);
    if (estimateOnly && !initialUpdate) {
      System.exit(0); // Log here?
    }
  }

  @Override
  public void run() {
    // The parent thread interrupts this progress updater thread when the progress updater is no longer
    // needed
    while (!Thread.interrupted()) {
      try {
        Thread.sleep(updateInterval);
      } catch (InterruptedException e) {
        break;
      }
      functions.onUpdate(cmd, stats, this);
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
