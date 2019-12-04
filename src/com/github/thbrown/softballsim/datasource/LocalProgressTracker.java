package com.github.thbrown.softballsim.datasource;

import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;

/**
 * This progress updater just prints the progress to std out.
 */
public class LocalProgressTracker extends ProgressTracker {

  public LocalProgressTracker(Result initialResult) {
    super(initialResult);
  }

  @Override
  public void onUpdate() {
    Result currentResult = super.getCurrentResult();
    if (currentResult != null) {
      double progressPercentage =
          ((double) currentResult.getCountCompleted()) / ((double) currentResult.getCountTotal()) * 100;
      String progress = StringUtils.formatDecimal(progressPercentage, 2);
      Logger.log(progress + "% complete -- Estimated Seconds Remaining: " + super.getEstimatedMsRemaining());
    }
  }

  @Override
  public void onComplete() {
    // TODO Auto-generated method stub

  }

}
