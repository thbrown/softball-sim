package com.github.thbrown.softballsim.datasource.local;

import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.datasource.DataSourceFunctions;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;

/**
 * Local data source prints data to the console via the logger on each event.
 */
public class DataSourceFunctionsFileSystem implements DataSourceFunctions {

  @Override
  public void onUpdate(ProgressTracker tracker) {
    Result currentResult = tracker.getCurrentResult();
    if (currentResult != null) {
      double progressPercentage =
          ((double) currentResult.getCountCompleted()) / ((double) currentResult.getCountTotal()) * 100;
      String progress = StringUtils.formatDecimal(progressPercentage, 2);
      Logger.log(progress + "% complete -- Estimated Seconds Remaining: " + tracker.getEstimatedSecondsRemaining()
          + " (Estimated time total: " + tracker.getEstimatedSecondsTotal() + ")");
    }
  }

  @Override
  public void onComplete(Result finalResult) {
    Logger.log(finalResult);
  }

  @Override
  public void onEstimationReady(ProgressTracker tracker) {
    Logger.log("Estimated completion time in seconds: " + tracker.getEstimatedSecondsTotal());
  }

}
