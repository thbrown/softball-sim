package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloGameSimulation;
import com.github.thbrown.softballsim.util.Logger;

/**
 * Manages the relationship between two results and acts as the judge between a best result so far
 * state and a series of new states
 */
public class TTestTask implements Callable<TTestTaskResult> {

  // TODO: make these configurable
  private static int INITIAL_GAMES_TO_SIMULATE = 10;
  private static int SAMPLE_CHUNK_SIZE = 10;
  private final static TTest tester = new TTest();

  private final List<LineupComposite> toTest;
  private final int inningsPerGame;
  private final double alpha;
  
  private long simulationsRequired = 0;

  public TTestTask(List<LineupComposite> toTest, int inningsPerGame, double alpha) {
    this.inningsPerGame = inningsPerGame;
    this.toTest = toTest;
    this.alpha = alpha;
  }

  @Override
  public TTestTaskResult call() throws Exception {

    LineupComposite bestSoFar = null;

    // Run simulations for the lineups they have none.
    for (LineupComposite toEvaluate : toTest) {
      // T-test requires at least 2 samples
      if (toEvaluate.getStats().getN() < 2) {
        simulateGames(INITIAL_GAMES_TO_SIMULATE, inningsPerGame, toEvaluate);
      }
    }

    Set<LineupComposite> eleminatedLineups = new HashSet<>();
    for (LineupComposite toEvaluate : toTest) {
      // If we have no best stored, the very next one becomes the best
      if (bestSoFar == null) {
        bestSoFar = toEvaluate;
        continue;
      }

      // Run simulations until the difference in mean runs scored is statistically significant
      SummaryStatistics statsA = bestSoFar.getStats();
      SummaryStatistics statsB = toEvaluate.getStats();

      while (true) {
        double pValue = tester.tTest(statsA, statsB);
        //Logger.log(statsA.getN() + " " + statsB.getN() + " " + statsA.getMean() + " " + statsB.getMean() + " " + pValue);
        if(pValue <= alpha) {
          //Logger.log("DONE! " + pValue + " " + statsA.getN() + " " + statsB.getN());
          break;
        }
        
        if(statsA.getN() >= 1000000 && statsB.getN() >= 1000000) {
          Logger.log("WARN: Reached simulation limit " + statsA.getMean() + " " + statsB.getMean() + " " + pValue);
          break;
        }
        
        // The different is not yet significant, which one do we need to run more simulations for?
        if (statsA.getN() < statsB.getN()) {
          // Do more simulations for the "bestSoFar" lineup
          simulateGames(SAMPLE_CHUNK_SIZE, inningsPerGame, bestSoFar);
        } else {
          // Do more simulations for the "toEvaluate" lineup
          simulateGames(SAMPLE_CHUNK_SIZE, inningsPerGame, toEvaluate);
        }
      }

      // Has the new lineup we are testing de-throned the champion?
      if (statsB.getMean() > statsA.getMean()) {
        eleminatedLineups.add(bestSoFar);
        bestSoFar = toEvaluate;
      } else {
        eleminatedLineups.add(toEvaluate);
      }
    }
    return new TTestTaskResult(bestSoFar, eleminatedLineups, simulationsRequired);
  }

  private void simulateGames(int numberOfGamesToSimulate, int inningsPerGame, LineupComposite composite) {
    for (int i = 0; i < numberOfGamesToSimulate; i++) {
      double score =
          MonteCarloGameSimulation.simulateGame(composite.getLineup(), inningsPerGame, composite.getHitGenerator());
      composite.addSample(score);
    }
    simulationsRequired += numberOfGamesToSimulate;
  }
}
