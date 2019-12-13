package com.github.thbrown.softballsim.datasource;

import java.util.List;
import java.util.Map;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;

/**
 * This class facilitates the estimate only argument by wrapping the call to optimize to provide
 * different functionality. See the java doc for the {@link #optimize} method.
 */
public class EstimateOnlyExecutionWrapper {

  private final long TIME_ESTIMATE_RUN_DURATION = 5000;

  private final OptimizerEnum optimizer;
  private final DataSourceFunctions functions;

  public EstimateOnlyExecutionWrapper(OptimizerEnum optimizer, DataSourceFunctions functions) {
    this.optimizer = optimizer;
    this.functions = functions;
  }

  /**
   * Wraps the optimizer provided in the constructor such that it
   * <li>Is run in it's own thread</li>
   * <li>Reports the estimated time after some time period (or if optimization completed before that
   * time is reached)</li>
   * <li>Terminates the entire application after after the estimated time is reported</li>
   * <ul>
   * 
   * See the optimizer's optimize method for details on the generics used
   * 
   * TODO: only one T here, can/should we use wildcards instead?
   */
  public <T extends Result> void optimize(List<String> players, LineupTypeEnum lineupType, DataStats data,
      Map<String, String> arguments, ProgressTracker progressTracker, T existingResult) {
    Object lock = new Object();

    synchronized (lock) {
      // Start the optimization in its own thread
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          optimizer.optimize(players, lineupType, data, arguments, progressTracker, existingResult);
          synchronized (lock) {
            lock.notify();
          }
        }
      });
      thread.start();

      // Report the estimated completion time after we've gathered data for some period of time or if the
      // optimization has completed before we've reached that point
      try {
        lock.wait(TIME_ESTIMATE_RUN_DURATION);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    functions.onEstimationReady(progressTracker);

    // We don't want to keep running the optimizations, there is no good way to kill the optimizer
    // thread and any threads it may have spawned, so we'll just terminate the whole application.
    // The alternative to this is to have each optimizer implement a terminate method, but I think
    // that will make the barrier to add an optimization too high for now
    System.exit(0);
  }

}
