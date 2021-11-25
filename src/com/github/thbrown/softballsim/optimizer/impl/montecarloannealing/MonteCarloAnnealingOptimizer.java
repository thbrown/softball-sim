package com.github.thbrown.softballsim.optimizer.impl.montecarloannealing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
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
import com.github.thbrown.softballsim.optimizer.impl.montecarloannealing.*;
import com.github.thbrown.softballsim.util.Logger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;


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

    // We'll apply some simple parallelization - just run a different instance of the optimizer on each
    // thread
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREADS);

    // Run an instance of the optimizer for thread
    List<Callable<Result>> tasks = new ArrayList<>();
    for (int i = 0; i < THREADS; i++) {
      MonteCarloAnnealingCallable task =
          new MonteCarloAnnealingCallable(ALPHA, LOWEST, INNINGS, DURATION, indexer, THREADS, progressTracker);
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
        bestResult =
            bestResult == null || compare.getLineupScore() > bestResult.getLineupScore() ? compare : bestResult;
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
    return Result.class;
  }

}
