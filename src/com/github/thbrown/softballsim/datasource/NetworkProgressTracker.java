package com.github.thbrown.softballsim.datasource;

import java.io.PrintWriter;
import java.util.Map;

import com.github.thbrown.softballsim.Logger;
import com.github.thbrown.softballsim.Result;
import com.google.gson.Gson;

/**
 * Progress tracker that sends the progress report over the network in
 * addition to printing it in the logs
 */
public class NetworkProgressTracker extends ProgressTracker {
  
  private final PrintWriter network;
  private final Gson gson;

  public NetworkProgressTracker(long totalOperations, long numberOfUpdateUnits, long startIndex, Gson gson, PrintWriter network, long initialElapsedTimeMs) {
    super(totalOperations, numberOfUpdateUnits, startIndex, initialElapsedTimeMs);
    this.network = network;
    this.gson = gson;
  }
  
  @Override
  public Map<String, Object> onMilestone(Result bestResult, Map<Long, Long> histo) {
    Map<String, Object> progressReport = super.onMilestone(bestResult, histo);
    
    String jsonInProgressCommand = gson.toJson(progressReport);
    network.println(jsonInProgressCommand);
    Logger.log("SENT: \t\t" + jsonInProgressCommand);
    
    // If the connection was broken, stop computation. We can't save the results anyways.
    if(network.checkError()) {
      throw new RuntimeException("Network Stream Encountered An Error. Terminating Compute.");
    };
    
    return progressReport;
  }

}
