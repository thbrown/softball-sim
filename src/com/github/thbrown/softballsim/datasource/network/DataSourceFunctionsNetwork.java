package com.github.thbrown.softballsim.datasource.network;

import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.datasource.DataSourceFunctions;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.datasource.local.DataSourceFunctionsFileSystem;

/**
 * Functions class that sends data over the network for each lifecycle event as well as logging
 * things to the console.
 * 
 * Note: Using inheritance here because it's an easy way to re-use the logging from the parent class
 * but I don't really like it because as we add more DataSources we may want to share code between
 * different branches of the class hierarchy which can't be done without refactoring the logic in to
 * some common class. So, TODO: Move logging stuff into common class that both
 * DataSourceFunctionsNetwork and DataSourceFunctionsFileSystem can use and remove the extends
 * relationship.
 */
public class DataSourceFunctionsNetwork extends DataSourceFunctionsFileSystem implements DataSourceFunctions {

  private final NetworkHelper network;

  public DataSourceFunctionsNetwork(NetworkHelper network) {
    super(null);
    this.network = network;
  }

  @Override
  public void onUpdate(ProgressTracker tracker) {
    super.onUpdate(tracker);
    Result latestResult = tracker.getCurrentResult();
    DataSourceNetworkCommandInProgress command = new DataSourceNetworkCommandInProgress(latestResult);
    network.writeCommand(command);
  }

  @Override
  public void onComplete(Result finalResult) {
    super.onComplete(finalResult);
    DataSourceNetworkCommandComplete command = new DataSourceNetworkCommandComplete(finalResult);
    network.writeCommand(command);
  }

  @Override
  public void onEstimationReady(ProgressTracker tracker) {
    super.onEstimationReady(tracker);
    DataSourceNetworkCommandEstimate command = new DataSourceNetworkCommandEstimate(tracker.getEstimatedSecondsTotal());
    network.writeCommand(command);
  }

}
