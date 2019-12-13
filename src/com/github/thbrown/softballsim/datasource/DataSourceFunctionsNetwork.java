package com.github.thbrown.softballsim.datasource;

import java.io.PrintWriter;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.util.Logger;

/**
 * Network data source prints data to the console via the logger on each event and sends those
 * events back over the network supplied on construction.
 */
public class DataSourceFunctionsNetwork implements DataSourceFunctions {

  private final PrintWriter network;

  public DataSourceFunctionsNetwork(Result initialResult, PrintWriter network) {
    this.network = network;
  }

  @Override
  public void onUpdate(ProgressTracker tracker) {
    Result latestResult = tracker.getCurrentResult();
    network.println(latestResult);
    Logger.log("SENT: \t\t" + latestResult);

    // If the connection was broken, stop computation. We can't save the results anyways.
    if (network.checkError()) {
      throw new RuntimeException("Network Stream Encountered An Error. Terminating Compute.");
    } ;

    /**
     * Map<String, Object> progressReport = new HashMap<>(); progressReport.put("command",
     * "IN_PROGRESS"); progressReport.put("complete", chunkCounter); progressReport.put("total",
     * totalChunks); progressReport.put("histogram", histo); progressReport.put("lineup",
     * bestResult.getLineup().toMap()); progressReport.put("score", bestResult.getScore());
     * progressReport.put("remainingTimeSec", remainingSeconds); progressReport.put("elapsedTimeMs",
     * this.startingElapsedTimeMs + (System.currentTimeMillis() - this.startTimeMs));
     * Logger.log(progressReport);
     */
  }

  @Override
  public void onComplete(Result finalResult) {
    // TODO Auto-generated method stub
  }

  @Override
  public void onEstimationReady(ProgressTracker tracker) {
    // TODO Auto-generated method stub
  }

}
