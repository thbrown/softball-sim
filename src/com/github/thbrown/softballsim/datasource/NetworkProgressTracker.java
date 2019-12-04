package com.github.thbrown.softballsim.datasource;

import java.io.PrintWriter;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.util.Logger;

public class NetworkProgressTracker extends ProgressTracker {

  private final PrintWriter network;

  public NetworkProgressTracker(Result initialResult, PrintWriter network) {
    super(initialResult);
    this.network = network;
  }

  @Override
  public void onUpdate() {
    Result latestResult = super.getCurrentResult();
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
  public void onComplete() {
    // TODO Auto-generated method stub

  }

}
