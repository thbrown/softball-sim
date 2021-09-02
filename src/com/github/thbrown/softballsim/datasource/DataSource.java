package com.github.thbrown.softballsim.datasource;

import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.data.gson.DataStats;

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
   * Retrieves additional command line options from the data source location.
   */
  public String[] getAdditionalOptions(CommandLine cmd);

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
  public void onUpdate(CommandLine cmd, DataStats stats, ProgressTracker tracker);

  /**
   * Called once after an optimizer has completed (whether is ends successfully or in error).
   */
  public void onComplete(CommandLine cmd, DataStats stats, Result finalResult);

  /**
   * Retrieves control false from data source, currently only used to halt.
   */
  public String getControlFlag(CommandLine cmd, DataStats stats);

}
