package com.github.thbrown.softballsim.datasource;

import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;

public interface DataSource {

  /**
   * Command line options specific to this optimizer
   */
  public List<Option> getCommandLineOptions();

  /**
   * Retrieves stats data from the data source location. That should be parsed and returned as a
   * DataStats object.
   * 
   * @param cmd
   */
  public DataStats getData(CommandLine cmd);

  /**
   * Retrieves the cache (if available) if this optimizer has been run with the same options on the
   * same data before.
   * 
   * @param stats
   */
  public Result getCachedResult(CommandLine cmd, DataStats stats);

  /**
   * Called on a set interval while an optimizer is running.
   */
  public default void onUpdate(CommandLine cmd, DataStats stats, ProgressTracker tracker) {
    Result currentResult = tracker.getCurrentResult();
    if (currentResult != null) {
      double progressPercentage = ((double) currentResult.getCountCompleted())
          / ((double) currentResult.getCountTotal()) * 100;
      String progress = StringUtils.formatDecimal(progressPercentage, 2);
      Logger.log(progress + "% complete -- Estimated Seconds Remaining: " + tracker.getEstimatedSecondsRemaining()
          + " (Estimated time total: " + tracker.getEstimatedSecondsTotal() + ")");
    }
  }

  /**
   * Called once after an optimizer has completed (whether is ends successfully or in error).
   */
  public void onComplete(CommandLine cmd, DataStats stats, Result finalResult);

  /**
   * Retrieves control false from data source, currently only used to halt.
   */
  public String getControlFlag(CommandLine cmd, DataStats stats);

}
