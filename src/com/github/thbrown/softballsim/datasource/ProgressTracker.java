package com.github.thbrown.softballsim.datasource;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import com.github.thbrown.softballsim.Logger;
import com.github.thbrown.softballsim.Result;

/**
 * This class allows the main thread to print a progress message
 * every updateInterval milliseconds
 */
public class ProgressTracker {
    
  protected final long totalChunks;
  protected final long updateInterval;

  protected long chunkCounter;
  protected long nextUpdateTime;
  protected long lastChunkCompletedCount = 0;
  
  protected DecimalFormat df = new DecimalFormat("#.##"); 
  
  public ProgressTracker(long totalOperations, long updateFrequency, long startIndex) {
    this.totalChunks = totalOperations;
    this.updateInterval = updateFrequency;
    this.chunkCounter = startIndex;
    this.lastChunkCompletedCount = startIndex;
    this.nextUpdateTime = System.currentTimeMillis() + updateFrequency;
  }
  
  public void markOperationAsComplete(Result bestResult, Map<Long, Long> histo) {
    chunkCounter++;
    long time = System.currentTimeMillis();
    if(time > nextUpdateTime) { 
      onMilestone(bestResult, histo);
      nextUpdateTime = time + updateInterval;
      lastChunkCompletedCount = chunkCounter;
    }
  }
  
  /**
   * Returns a progress report about the current state of the optimization
   */
  public Map<String,Object> onMilestone(Result bestResult, Map<Long, Long> histo) {
    long remainingChunks = this.totalChunks - this.chunkCounter;
    double rate = ((double)(chunkCounter - lastChunkCompletedCount))/ (this.updateInterval/1000);
    long remainingSeconds = (long) (remainingChunks / rate);
    
    Logger.log(df.format(chunkCounter*100/totalChunks) + "% complete (~" + String.format( "%.2f", (double)remainingSeconds/60.0/60.0 ) + " hours remaining) Rate:" + rate + " lineups per sec");
    
    // TODO: This needs to move to some optimization specific file
    Map<String,Object> progressReport = new HashMap<>();
    progressReport.put("command", "IN_PROGRESS");
    progressReport.put("complete", chunkCounter);
    progressReport.put("total", totalChunks);
    progressReport.put("histogram", histo);
    progressReport.put("lineup", bestResult.getLineup().toMap());
    progressReport.put("score", bestResult.getScore());
    progressReport.put("remainingSeconds", remainingSeconds);
    Logger.log(progressReport);
    return progressReport;
  }
  
}
