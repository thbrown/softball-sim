package com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive;

import java.util.concurrent.Callable;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.util.Logger;

/**
 * Wraps the MonteCarloGameSimulation in a Callable so it can be run in a separate thread. Also
 * wraps the call in a loop so we can simulate multiple games in one call.
 */
public class MonteCarloMultiGameSimulationTask implements Callable<TaskResult> {

  private BattingLineup lineup;
  private long numberOfGamesToSimulate;
  private int inningsPerGame;

  private HitGenerator hitGenerator;

  public MonteCarloMultiGameSimulationTask(BattingLineup lineup, long numberOfGamesToSimulate, int inningsPerGame,
      HitGenerator hitGenerator) {
    if (lineup == null) {
      Logger.log("NULL LINEUP");
    }
    this.lineup = lineup;
    this.numberOfGamesToSimulate = numberOfGamesToSimulate;
    this.inningsPerGame = inningsPerGame;
    this.hitGenerator = hitGenerator;
  }

  public TaskResult call() {
    return run();
  }

  public TaskResult run() {
    // Simulate *numberOfGamesToSimulate* games, average the runs scored, return the result of the
    // simulation
    double totalScore = 0;
    for (int i = 0; i < numberOfGamesToSimulate; i++) {
      double gameScore = MonteCarloGameSimulation.simulateGame(lineup, inningsPerGame, hitGenerator);
      totalScore += gameScore;
    }
    double meanScore = totalScore / numberOfGamesToSimulate;

    TaskResult result = new TaskResult(meanScore, lineup);
    return result;
  }

}
