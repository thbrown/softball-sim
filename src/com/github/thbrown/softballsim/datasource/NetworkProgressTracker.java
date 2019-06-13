package com.github.thbrown.softballsim.datasource;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.github.thbrown.softballsim.Result;
import com.google.gson.Gson;

public class NetworkProgressTracker extends ProgressTracker {
  
  private final PrintWriter network;
  private final Gson gson;

  public NetworkProgressTracker(long totalOperations, long numberOfUpdateUnits, long startIndex, Gson gson, PrintWriter network) {
    super(totalOperations, numberOfUpdateUnits, startIndex);
    this.network = network;
    this.gson = gson;
  }
  
  @Override
  public void onMilestone(Result bestResult, Map<Long, Long> histo) {
    super.onMilestone(bestResult, histo);
    
    // TODO: This needs to move to some optimization specific file
    Map<String,Object> inProgressCommand = new HashMap<>();
    inProgressCommand.put("command", "IN_PROGRESS");
    inProgressCommand.put("complete", operationCounter);
    inProgressCommand.put("total", totalOperations);
    inProgressCommand.put("histogram", histo);
    inProgressCommand.put("lineup", bestResult.getLineup().toMap());
    inProgressCommand.put("score", bestResult.getScore());
    String jsonInProgressCommand = gson.toJson(inProgressCommand);
    network.println(jsonInProgressCommand);
    System.out.println("SENT: \t\t" + jsonInProgressCommand);
    
    // If the connection was broken, stop computation. We can't save the results anyways.
    if(network.checkError()) {
      throw new RuntimeException("Network Stream Encountered An Error. Terminating Compute.");
    };
  }

}
