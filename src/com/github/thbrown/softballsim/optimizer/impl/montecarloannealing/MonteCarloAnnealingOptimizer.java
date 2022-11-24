package com.github.thbrown.softballsim.optimizer.impl.montecarloannealing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineupindexer.BattingLineupIndexer;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.Optimizer;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MonteCarloAnnealingOptimizer implements Optimizer<Result> {

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
    final double ALPHA = parsedArguments.getAlpha();
    final boolean LOWEST = parsedArguments.isLowestScore();
    final int THREADS = parsedArguments.getThreads();
    final int INNINGS = parsedArguments.getInnings();
    final long DURATION = parsedArguments.getDuration();

    // We'll apply some simple parallelization - just run a different instance of
    // the optimizer on each
    // thread
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREADS);

    // Run an instance of the optimizer for thread
    List<Callable<Result>> tasks = new ArrayList<>();
    for (int i = 0; i < THREADS; i++) {
      MonteCarloAnnealingCallable task = new MonteCarloAnnealingCallable(ALPHA, LOWEST, INNINGS, DURATION, indexer,
          THREADS, progressTracker);
      tasks.add(task);
    }

    try {
      // Run the tasks then shutdown the executor
      List<Future<Result>> list = executor.invokeAll(tasks);
      executor.shutdown();

      // Of all the results, return the best one
      Result bestResult = null;
      for (Future<Result> future : list) {
        Result compare = future.get();
        bestResult = bestResult == null || compare.getLineupScore() > bestResult.getLineupScore() ? compare
            : bestResult;
      }
      return bestResult;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  private void validateData(DataStats battingData, List<String> playersInLineup) {
    // TODO Auto-generated method stub

  }

  @Override
  public Class<? extends Result> getResultClass() {
    return MonteCarloAnnealingResult.class;
  }

  @Override
  public Result estimate(List<String> playersInLineup, LineupTypeEnum lineupType, DataStats battingData,
      Map<String, String> arguments, Result existingResult) throws Exception {
    // Check that the batting data we have is sufficient to run this optimizer
    validateData(battingData, playersInLineup);
    MonteCarloAnnealingArgumentParser parsedArguments = new MonteCarloAnnealingArgumentParser(arguments);

    // This is a time-constrained optimization, to estimating the completion time is
    // easy
    return new MonteCarloAnnealingResult(parsedArguments.getDuration() * 1000);
  }

}
