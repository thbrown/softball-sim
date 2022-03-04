package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.util.Pair;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.statstransform.SummaryStatisticsTransform;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloGameSimulation;
import com.github.thbrown.softballsim.util.Logger;

/**
 * Manages the relationship between two results and acts as the judge between a best result so far
 * state and a series of new states
 */
public class TTestTask implements Callable<TTestTaskResult> {

  // TODO: make these configurable
  public static int MAX_ITERATIONS = 1000000;
  private static int INITIAL_GAMES_TO_SIMULATE = 1000;
  private static int SAMPLE_CHUNK_SIZE = 1000;
  private final static TTest tester = new TTest();

  protected final List<LineupComposite> toTest;
  private final int inningsPerGame;
  private final double alpha;
  private final boolean lowest;

  private long simulationsRequired = 0;
  private long comparisonsThatReachedSimLimit = 0;

  private SummaryStatisticsTransform transform;

  public TTestTask(List<LineupComposite> toTest, int inningsPerGame, double alpha,
      SummaryStatisticsTransform transform, boolean lowest) {
    this.inningsPerGame = inningsPerGame;
    this.toTest = toTest;
    this.alpha = alpha;
    this.transform = transform;
    this.lowest = lowest;
  }

  public TTestTask(List<LineupComposite> toTest, int inningsPerGame, double alpha, boolean lowest) {
    this(toTest, inningsPerGame, alpha, null, lowest);
  }

  @Override
  public TTestTaskResult call() {
    // Run simulations for the lineups if they have none.
    for (LineupComposite toEvaluate : toTest) {
      // T-test requires at least 2 toEvaluate
      if (toEvaluate.getStats().getN() < 2) {
        simulateGames(INITIAL_GAMES_TO_SIMULATE, inningsPerGame, toEvaluate);
      }
    }

    LineupComposite bestSoFar = null;
    Set<LineupComposite> eliminatedLineups = new HashSet<>();
    for (LineupComposite toEvaluate : toTest) {
      if (bestSoFar == null) {
        bestSoFar = toEvaluate;
      }

      // Just in case we have any duplicate lineupComposites
      if (toEvaluate.equals(bestSoFar)) {
        continue;
      }

      // Run simulations until the difference in mean runs scored is statistically significant
      StatisticalSummary statsA;
      StatisticalSummary statsB;

      while (true) {
        statsA = bestSoFar.getStats();
        statsB = toEvaluate.getStats();

        // Transform the SummaryStatistics to alter the nature of the tTest, if necessary
        if (this.transform != null) {
          Pair<StatisticalSummary, StatisticalSummary> transformed = this.transform.transform(statsA, statsB);
          statsA = transformed.getFirst();
          statsB = transformed.getSecond();
        }

        double pValue = tester.tTest(statsA, statsB);
        // Logger.log(statsA.getN() + " " + statsB.getN() + " " + statsA.getMean() + " " + statsB.getMean()
        // + " " + pValue);

        // Check if we have a large enough sample size to determine that populations are different
        if (pValue <= alpha) {
          break;
        }

        // Check if we've exceeded the maximum number of allowed samples
        if (statsA.getN() >= MAX_ITERATIONS && statsB.getN() >= MAX_ITERATIONS) {
          comparisonsThatReachedSimLimit++;
          Logger.log("WARN: Reached simulation limit " + statsA.getMean() + " " + statsB.getMean() + " " + pValue); // PIZZA
          break;
        }

        // We need more samples, lets calculate them for whichever lineup we have less samples for
        if (statsA.getN() < statsB.getN()) {
          // Do more simulations for the "bestSoFar" lineup
          simulateGames(SAMPLE_CHUNK_SIZE, inningsPerGame, bestSoFar);
        } else {
          // Do more simulations for the "toEvaluate" lineup
          simulateGames(SAMPLE_CHUNK_SIZE, inningsPerGame, toEvaluate);
        }
      }

      if (this.lowest) {
        // Has the new lineup we are testing de-throned the champion? - we are looking for the lowest
        // scroing lineup
        if (statsB.getMean() < statsA.getMean()) {
          eliminatedLineups.add(bestSoFar);
          bestSoFar = toEvaluate;
        } else {
          eliminatedLineups.add(toEvaluate);
        }
      } else {
        // Has the new lineup we are testing de-throned the champion? - we are looking for the highest
        // scroing lineup
        if (statsB.getMean() > statsA.getMean()) {
          eliminatedLineups.add(bestSoFar);
          bestSoFar = toEvaluate;
        } else {
          eliminatedLineups.add(toEvaluate);
        }
      }
    }

    return new TTestTaskResult(bestSoFar, eliminatedLineups, simulationsRequired, comparisonsThatReachedSimLimit);
  }

  private List<Double> simulateGames(int numberOfGamesToSimulate, int inningsPerGame, LineupComposite composite) {
    List<Double> simulatedGames = new ArrayList<>(numberOfGamesToSimulate);
    for (int i = 0; i < numberOfGamesToSimulate; i++) {
      double score =
          MonteCarloGameSimulation.simulateGame(composite.getLineup(), inningsPerGame, composite.getHitGenerator());
      composite.addSample(score);
      simulatedGames.add(score);
    }
    simulationsRequired += numberOfGamesToSimulate;
    return simulatedGames;
  }
}
