package com.github.thbrown.softballsim.optimizer;

import java.util.List;
import java.util.Map;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;

public interface Optimizer<R extends Result> {

  /**
   * Code that gets executed to actually determine the optimized lineup. This is where all the magic
   * happens.
   * 
   * @param playersInLineup - List of player ids to include in the optimized lineup
   * @param lineupType - Type of lineup that should be returned
   * @param battingData - Batting statistics for any games a playersInLineup has played in
   * @param arguments - Custom list of arguments provided by the user. These can be defined for each
   *        optimizer in the JSON optimizer definition file
   * @param progressTracker - Used to display the omtimization's progress to the end user.
   *        progressTracker.updateProgress(...) should be called every few seconds.
   * @param existingResult - This optimization may have been paused and resumed. If so, this object
   *        contains the most recent result saved by a call to progressTracker.updateProgress(...)
   *        that can be used to resume the optimization from it's previous state without starting
   *        over. Null if no previous result was saved.
   */
  public Result optimize(List<String> playersInLineup, LineupTypeEnum lineupType, DataStats battingData,
      Map<String, String> arguments, ProgressTracker progressTracker, R existingResult);
  
  /**
   * Returns an estimate for the amount of time it will take your optimization to run.
   */
  public Result estimate(List<String> playersInLineup, LineupTypeEnum lineupType, DataStats battingData,
      Map<String, String> arguments, ProgressTracker progressTracker, R existingResult);

  /**
   * Just return the class of that result generic (Result.class if you are not using your own subclass of result).
   * 
   * This is currently required to serialize/de-serialize partial results.
   * 
   * TODO: See if there is a way to fix this: It's a shame that this has to be specified twice. Once
   * by overriding this method and once in the declaration of the subclass.
   */
  public Class<? extends Result> getResultClass();

}
