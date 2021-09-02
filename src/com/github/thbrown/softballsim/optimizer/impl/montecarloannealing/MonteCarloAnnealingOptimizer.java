package com.github.thbrown.softballsim.optimizer.impl.montecarloannealing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.Pair;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineupindexer.BattingLineupIndexer;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.Optimizer;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.LineupComposite;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.SynchronizedLineupCompositeWrapper;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.TTestTask;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.TTestTaskResult;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.statstransform.RangeSummaryStatisticsTransform;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.statstransform.SummaryStatisticsTransform;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.HitGenerator;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloGameSimulation;

public class MonteCarloAnnealingOptimizer implements Optimizer<Result> {

  private static final int PRELIMINARY_DATA_SAMPLE_SIZE = 100;
  private static final int PRELIMINARY_DATA_GAME_SIMULATIONS = 10000;

  private static final int FINAL_RESULT_ITERATIONS = 1000000;

  // [0-1] A value closer to 0 results in a steeper decline in temperature near the beginning
  private static final double SKEW = 1;

  private static final double ALPHA = .001;

  @Override
  public Result optimize(List<String> playersInLineup, LineupTypeEnum lineupType, DataStats battingData,
      Map<String, String> arguments, ProgressTracker progressTracker, Result existingResult) {

    // Start the timer
    long startTimestamp = System.currentTimeMillis();

    // Check that the batting data we have is sufficient to run this optimizer
    validateData(battingData, playersInLineup);

    // Get the arguments as their expected types
    MonteCarloAnnealingArgumentParser parsedArguments = new MonteCarloAnnealingArgumentParser(arguments);
    BattingLineupIndexer indexer = lineupType.getLineupIndexer(battingData, playersInLineup);

    // Determine some statistical information about the data, we use this to inform our annealing
    // parameters
    SummaryStatistics dataStats = new SummaryStatistics();
    for (int i = 0; i < PRELIMINARY_DATA_SAMPLE_SIZE; i++) {
      long randomIndex = ThreadLocalRandom.current().nextLong(0, indexer.size());
      BattingLineup randomLineup = indexer.getLineup(randomIndex);
      HitGenerator hitGenerator = new HitGenerator(randomLineup.asList());
      SummaryStatistics lineupSummaryStatistics = new SummaryStatistics();
      for (int j = 0; j < PRELIMINARY_DATA_GAME_SIMULATIONS; j++) {
        double score = MonteCarloGameSimulation.simulateGame(randomLineup, parsedArguments.getInnings(), hitGenerator);
        lineupSummaryStatistics.addValue(score);
      }
      dataStats.addValue(lineupSummaryStatistics.getMean());
    }
    double maxTemperature = dataStats.getStandardDeviation() * 3;

    // Choose a random lineup
    long activeLineupIndex = ThreadLocalRandom.current().nextLong(0, indexer.size());
    BattingLineup activeLineup = indexer.getLineup(activeLineupIndex);
    HitGenerator activeLineupHitGenerator = new HitGenerator(activeLineup.asList());
    LineupComposite activeComposite = new LineupComposite(activeLineup, activeLineupHitGenerator, activeLineupIndex);

    // long annealingStartTimestamp = System.currentTimeMillis();

    long totalSimulations = 0;
    final long iterations = parsedArguments.getDuration();
    for (int i = 0; i < iterations; i++) {
      double temperature = this.getTemperature(maxTemperature, 0, iterations, i);

      // Get a random neighbor
      Pair<Long, BattingLineup> comparisonPair = indexer.getRandomNeighbor(activeComposite.getIndex());
      long comparisonLinupIndex = comparisonPair.getFirst();
      BattingLineup comparisonLineup = comparisonPair.getSecond();
      HitGenerator comparisonLineupHitGenerator = new HitGenerator(comparisonLineup.asList());
      LineupComposite comparisonComposite =
          new LineupComposite(comparisonLineup, comparisonLineupHitGenerator, comparisonLinupIndex);

      // Simulate both until we achieve a small enough t-value (or we reach the max number of allowed
      // optimizations)
      List<LineupComposite> lineupsToTTest = new ArrayList<>(2);
      lineupsToTTest.add(activeComposite);
      lineupsToTTest.add(comparisonComposite);

      // This transform modifies the tTest such that it now tells us the confidence that the mean of the
      // two populations is within 'temperature' of each other. This requires a smaller a sample size to
      // determine.
      SummaryStatisticsTransform transform = new RangeSummaryStatisticsTransform(temperature);
      TTestTask task = new TTestTask(lineupsToTTest, parsedArguments.getInnings(), ALPHA, transform);
      TTestTaskResult result = task.call();

      totalSimulations += result.getSimulationsRequired();

      // Update the two lineupComposites under test
      // TTest returns the best lineup composite, we'll need to check whether that one is the active or
      // comparison
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
      double diff = activeComposite.getStats().getMean() - comparisonComposite.getStats().getMean();
      // Logger.log(temperature + " " + diff + ((diff < temperature) ? " ACCEPT" : " REJECT"));
      if (diff < temperature) {
        // Logger.log(comparisonComposite.getStats().getMean() + " is better than " +
        // activeComposite.getStats().getMean());
        activeComposite = comparisonComposite;
      }

      progressTracker.updateProgress(new Result(OptimizerEnum.MONTE_CARLO_ANNEALING, activeComposite.getLineup(),
          activeComposite.getStats().getMean(), (long) iterations, (long) i,
          System.currentTimeMillis() - startTimestamp, ResultStatusEnum.PARTIAL));
    }

    // Make sure the final result has at least FINAL_RESULT_ITERATIONS iterations
    for (long i = activeComposite.getStats().getN(); i < FINAL_RESULT_ITERATIONS; i++) {
      double score =
          MonteCarloGameSimulation.simulateGame(activeComposite.getLineup(), parsedArguments.getInnings(),
              activeComposite.getHitGenerator());
      activeComposite.addSample(score);
    }

    System.out.println("Simulations Required " + totalSimulations + " which is " + totalSimulations / iterations
        + " per iteration or " + totalSimulations / (indexer.size()) + " per lineup "
        + activeComposite.getStats().getN());

    return new Result(OptimizerEnum.MONTE_CARLO_ANNEALING, activeComposite.getLineup(),
        activeComposite.getStats().getMean(), (long) iterations, (long) iterations,
        System.currentTimeMillis() - startTimestamp, ResultStatusEnum.COMPLETE);
  }

  private double getTemperature(double maxTemperature, long startIndex, long endIndex, long activeIndex) {
    // Determine alpha value such that the temperature is maxTemperature at startIndex and nearly 0
    // at endIndex
    maxTemperature = maxTemperature + SKEW;
    double alpha = Math.pow((SKEW / maxTemperature), 1.0 / (endIndex - startIndex));

    // Exponential Multiplicative
    // Tk = T0 * a^k --- (.8 <= a <= .9)
    return (maxTemperature * Math.pow(alpha, activeIndex)) - SKEW;

    // Quadratic Mutiplicative
    // Tk = T0 / (1+a*k^2) --- (a > 0)
  }

  private void validateData(DataStats battingData, List<String> playersInLineup) {
    // TODO Auto-generated method stub

  }

  @Override
  public Class<? extends Result> getResultClass() {
    return Result.class;
  }

}
