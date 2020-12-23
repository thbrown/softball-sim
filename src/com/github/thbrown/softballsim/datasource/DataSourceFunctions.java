package com.github.thbrown.softballsim.datasource;

import com.github.thbrown.softballsim.Result;

/**
 * Functions that are invoked as part of an optimization's lifecycle.
 */
public interface DataSourceFunctions {

  /**
   * Run on a time interval while an optimization is running, hopefully update results are available
   * each time.
   */
  public void onUpdate(ProgressTracker tracker);

  /**
   * Run once when an optimimization completes.
   */
  public void onComplete(Result finalResult);

  /**
   * Run once if the estimate only argument is provided and the optimization has completed or the time
   * threshold for reporting and estimated completion time has been reached
   */
  public void onEstimationReady(ProgressTracker tracker);

}
