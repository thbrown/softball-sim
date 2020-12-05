package com.github.thbrown.softballsim.optimizer.impl.expectedvalue;

import java.util.concurrent.Callable;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.TaskResult;
import com.github.thbrown.softballsim.util.Logger;

public class ExpectedValueTask implements Callable<TaskResult> {

  private BattingLineup lineup;
  private int inningsPerGame;
  private int maxNumberOfPlateApperancesPerGame;

  public ExpectedValueTask(BattingLineup lineup, int maxNumberOfPlateApperancesPerGame, int inningsPerGame) {
    if (lineup == null) {
      Logger.log("NULL LINEUP");
    }
    this.lineup = lineup;
    this.inningsPerGame = inningsPerGame;
    this.maxNumberOfPlateApperancesPerGame = maxNumberOfPlateApperancesPerGame;
  }

  public TaskResult call() {
    return run();
  }

  public TaskResult run() {
    TaskResult result = new TaskResult(
        ExpectedValue.getExpectedValue(lineup, inningsPerGame, this.maxNumberOfPlateApperancesPerGame), lineup);
    return result;
  }

}
