package com.github.thbrown.softballsim;

import java.text.DecimalFormat;

/**
 * This class gets passed to each simulation and allows each thread to print a progress message
 * if a landmark calculation has been completed (i.e. every 1%)
 */
public class ProgressTracker {
  
  private double percentagePerUpdate = 0;
  private long operationCounter = 0;
  private long updateCounter = 0;
  private long triggerValue = 0;
  
  private DecimalFormat df = new DecimalFormat("#.##"); 
  
  public ProgressTracker(long totalOperations, long numberOfUpdateUnits) {
    this.triggerValue = totalOperations/numberOfUpdateUnits;
    this.percentagePerUpdate = ((double)this.triggerValue/totalOperations); 
    System.out.println(totalOperations + " " + this.triggerValue + " " + this.percentagePerUpdate);
  }
  
  public synchronized void markOperationAsComplete() {
    operationCounter++;
    if(operationCounter % this.triggerValue == 0) {
      updateCounter++;
      System.out.println(df.format(updateCounter*percentagePerUpdate*100) + "% complete");
    }
  }
  
}
