package com.github.thbrown.softballsim.datasource;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import com.github.thbrown.softballsim.Result;

/**
 * This class allows each thread to print a progress message
 * if a milestone calculation has been completed (i.e. every 1%)
 */
public class ProgressTracker {
  
  protected final long triggerValue;
  protected final long totalOperations;
  
  protected long operationCounter = 0;

  protected DecimalFormat df = new DecimalFormat("#.##"); 
  
  public ProgressTracker(long totalOperations, long numberOfUpdateUnits, long startIndex) {
    this.totalOperations = totalOperations;
    if(numberOfUpdateUnits == 0) {
      this.triggerValue = 0;
      return;
    }
    this.operationCounter = startIndex;
    this.triggerValue = totalOperations/numberOfUpdateUnits;
  }
  
  public synchronized void markOperationAsComplete(Result bestResult, Map<Long, Long> histo) {
    operationCounter++;
    if(this.triggerValue > 0 && operationCounter % this.triggerValue == 0) {
      onMilestone(bestResult, histo);
    }  
  }
  
  public void onMilestone(Result bestResult, Map<Long, Long> histo) {
    // TODO: find a way to remove this from the sync block
    System.out.println(df.format(operationCounter*100/totalOperations) + "% complete");
    Map<String,Object> inProgressCommand = new HashMap<>();
    inProgressCommand.put("command", "IN_PROGRESS");
    inProgressCommand.put("complete", operationCounter);
    inProgressCommand.put("total", totalOperations);
    inProgressCommand.put("histoSoFar", histo);
    inProgressCommand.put("bestLineupSoFar", bestResult.getLineup().toMap());
    inProgressCommand.put("bestLineupScoreSoFar", bestResult.getScore());
    
    //for(Strin inProgressCommand.keySet())
    System.out.println(inProgressCommand);
  }
  
}
