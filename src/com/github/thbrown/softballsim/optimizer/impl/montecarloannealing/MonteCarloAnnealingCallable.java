package com.github.thbrown.softballsim.optimizer.impl.montecarloannealing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.Pair;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineupindexer.BattingLineupIndexer;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.LineupComposite;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.TTestTask;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.TTestTaskResult;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.statstransform.RangeSummaryStatisticsTransform;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.statstransform.SummaryStatisticsTransform;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.HitGenerator;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloGameSimulation;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.MiscUtils;
import java.util.concurrent.Callable;

public class MonteCarloAnnealingCallable implements Callable<Result> {


  private static final int PRELIMINARY_DATA_SAMPLE_SIZE = 100;
  private static final int PRELIMINARY_DATA_GAME_SIMULATIONS = 10000;

  private static final int FINAL_RESULT_ITERATIONS = 1000000;
  private static final int MAX_CACHE_SIZE = 10000;

  // [0-1] A value closer to 0 results in a steeper decline in temperature near
  // the beginning
  private static final double SKEW = 1;

  private double ALPHA;
  private boolean LOWEST;
  private int INNINGS;
  private long DURATION;
  private int THREADS;
  private BattingLineupIndexer indexer;
  private ProgressTracker progressTracker;

  public MonteCarloAnnealingCallable(double ALPHA, boolean LOWEST, int INNINGS, long DURATION,
      BattingLineupIndexer indexer, int THREADS, ProgressTracker progressTracker) {
    this.ALPHA = ALPHA;
    this.LOWEST = LOWEST;
    this.INNINGS = INNINGS;
    this.DURATION = DURATION;
    this.indexer = indexer;
    this.THREADS = THREADS;
    this.progressTracker = progressTracker;
  }

  @Override
  public Result call() {
    // Start the timer
    long startTimestamp = System.currentTimeMillis();

    // Calc next progress repor time
    long nextUpdateTime = startTimestamp + 1000 * ThreadLocalRandom.current().nextInt(0, THREADS * 2);

    // Use summary statistics about a sampling of all possible lineups to determine out annealing
    // parameters
    SummaryStatistics dataStats = MiscUtils.getSummaryStatisticsForIndexer(indexer, PRELIMINARY_DATA_SAMPLE_SIZE,
        PRELIMINARY_DATA_GAME_SIMULATIONS, INNINGS);
    double maxTemperature = dataStats.getStandardDeviation() * 3;

    // Choose a random lineup
    long activeLineupIndex = ThreadLocalRandom.current().nextLong(0, indexer.size());
    BattingLineup activeLineup = indexer.getLineup(activeLineupIndex);
    HitGenerator activeLineupHitGenerator = new HitGenerator(activeLineup.asList());
    LineupComposite activeComposite = new LineupComposite(activeLineup, activeLineupHitGenerator, activeLineupIndex);

    // We are pretty much guaranteed to hit the same lineups multiple times, especially at the end of
    // the optimization, so we'll cache our most recent computations for a speed increase
    LinkedHashMap<Long, LineupComposite> cachedLineups =
        new LinkedHashMap<>() {
          @Override
          protected boolean removeEldestEntry(Map.Entry<Long, LineupComposite> eldest) {
            return size() > MAX_CACHE_SIZE;
          }
        };

    long totalSimulations = 0;

    final long durationMs = this.DURATION * 1000;
    final long startTime = System.currentTimeMillis();
    for (long i = 0; i < durationMs; i = System.currentTimeMillis() - startTime) {

      double temperature = this.getTemperature(maxTemperature, 0, durationMs, i);

      // Get a random neighbor
      Pair<Long, BattingLineup> comparisonPair = indexer.getRandomNeighbor(activeComposite.getIndex());

      long comparisonLinupIndex = comparisonPair.getFirst();
      BattingLineup comparisonLineup = comparisonPair.getSecond();
      HitGenerator comparisonLineupHitGenerator = new HitGenerator(comparisonLineup.asList());
      LineupComposite comparisonComposite = new LineupComposite(comparisonLineup, comparisonLineupHitGenerator,
          comparisonLinupIndex);

      if (cachedLineups.containsKey(comparisonLinupIndex)) {
        // We've already done some game simulations on this lineup. Re-use lineup
        // composite so we don't have to re-compute everything
        comparisonComposite = cachedLineups.get(comparisonLinupIndex);
      } else {
        cachedLineups.put(comparisonLinupIndex, comparisonComposite);
      }

      // Simulate both until we achieve a small enough t-value (or we reach the max number of allowed
      // optimizations)
      List<LineupComposite> lineupsToTTest = new ArrayList<>(2);
      lineupsToTTest.add(activeComposite);
      lineupsToTTest.add(comparisonComposite);

      // This transform modifies the tTest such that it now tells us the confidence that the means of the
      // two populations are within 'temperature' of each other. This requires a smaller sample size to
      // determine.
      SummaryStatisticsTransform transform = new RangeSummaryStatisticsTransform(temperature);
      TTestTask task = new TTestTask(lineupsToTTest, INNINGS, ALPHA, transform, LOWEST);
      TTestTaskResult result = task.call();

      totalSimulations += result.getSimulationsRequired();

      // Update the two lineupComposites under test TTest returns the best lineup composite, we'll need to
      // check whether that one is the active or comparison
      if (result.getBestLineupComposite().equals(activeComposite)) {
        activeComposite = result.getBestLineupComposite();
        comparisonComposite = result.getEliminatedLineupComposites().iterator().next(); // Should only have one element
      } else {
        comparisonComposite = result.getBestLineupComposite();
        activeComposite = result.getEliminatedLineupComposites().iterator().next(); // Should only have one element
      }

      // Accept the comparisonLineup if the mean difference in runs is less than the temperature
      // this.getTemperature(maxTemperature, annealingStartTimestamp, annealingStartTimestamp +
      // parsedArguments.getDuration(), System.currentTimeMillis());
      double diff = 0;
      if (LOWEST) {
        diff = comparisonComposite.getStats().getMean() - activeComposite.getStats().getMean();
      } else {
        diff = activeComposite.getStats().getMean() - comparisonComposite.getStats().getMean();
      }

      if (diff < temperature) {
        // Logger.log(comparisonComposite.getStats().getMean() + " is better than " +
        // activeComposite.getStats().getMean());
        activeComposite = comparisonComposite;
      }

      // Update the progress if it's time and our result is better than current best one
      if (progressTracker != null && System.currentTimeMillis() > nextUpdateTime) {
        Result newResult =
            new MonteCarloAnnealingResult(activeComposite.getLineup(),
                activeComposite.getStats().getMean(), (long) durationMs, (long) i,
                System.currentTimeMillis() - startTimestamp, ResultStatusEnum.IN_PROGRESS);

        Result r = progressTracker.getCurrentResult();
        if (newResult.getCountCompleted() > r.getCountCompleted()) {
          progressTracker.updateProgress(newResult);
        }
        nextUpdateTime = startTimestamp + 1000 * ThreadLocalRandom.current().nextInt(0, THREADS * 2);
      }
    }

    // Make sure the final result has at least FINAL_RESULT_ITERATIONS iterations
    for (long i = activeComposite.getStats().getN(); i < FINAL_RESULT_ITERATIONS; i++) {
      double score = MonteCarloGameSimulation.simulateGame(activeComposite.getLineup(), INNINGS,
          activeComposite.getHitGenerator());
      activeComposite.addSample(score);
    }

    /*
     * System.out.println( "Simulations Required: " + totalSimulations + " which is " + totalSimulations
     * / iterations + " simulations per iteration or " + totalSimulations / (indexer.size()) +
     * " simulations per lineup " + activeComposite.getStats().getN());
     */

    return new MonteCarloAnnealingResult(activeComposite.getLineup(),
        activeComposite.getStats().getMean(), (long) durationMs, (long) durationMs,
        System.currentTimeMillis() - startTimestamp, ResultStatusEnum.COMPLETE);
  };


  private double getTemperature(double maxTemperature, long startIndex, long endIndex, long activeIndex) {
    // Determine alpha value such that the temperature is maxTemperature at
    // startIndex and nearly 0
    // at endIndex
    maxTemperature = maxTemperature + SKEW;
    double alpha = Math.pow((SKEW / maxTemperature), 1.0 / (endIndex - startIndex));

    // Exponential Multiplicative
    // Tk = T0 * a^k --- (.8 <= a <= .9)
    return (maxTemperature * Math.pow(alpha, activeIndex)) - SKEW;

    // Quadratic Mutiplicative
    // Tk = T0 / (1+a*k^2) --- (a > 0)
    // return (maxTemperature / (1 + alpha * Math.pow(activeIndex,2)) - SKEW;
  }



}
