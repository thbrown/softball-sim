package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import com.github.thbrown.softballsim.Msg;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineupindexer.BattingLineupIndexer;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.Optimizer;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.statstransform.RangeSummaryStatisticsTransform;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.statstransform.SummaryStatisticsTransform;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.HitGenerator;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloExhaustiveResult;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloGameSimulation;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.MiscUtils;

public class MonteCarloAdaptiveOptimizer implements Optimizer<MonteCarloAdaptiveResult> {

  private static final int TASK_MAX_LINEUP_COUNT = 2;
  private static final int TASK_MIN_LINEUP_COUNT = 1;

  // Maximum number of tasks that should be queued up at once.
  private static final int TASK_BUFFER_SIZE = 20000;

  private long lineupIndex = 0;

  @Override
  public MonteCarloAdaptiveResult optimize(List<String> playersInLineup, LineupTypeEnum lineupType,
      DataStats battingData, Map<String, String> arguments, ProgressTracker progressTracker,
      MonteCarloAdaptiveResult existingResult) {

    // Start the timer
    long startTimestamp = System.currentTimeMillis();

    // Check that the batting data we have is sufficient to run this optimizer
    validateData(battingData, playersInLineup);

    // Get the arguments as their expected types
    MonteCarloAdaptiveArgumentParser parsedArguments = new MonteCarloAdaptiveArgumentParser(arguments);
    final double ALPHA = parsedArguments.getAlpha();
    final int INNINGS = parsedArguments.getInnings();
    final boolean LOWEST = parsedArguments.isLowestScore();

    // Since this optimizer involves iterating over all possible lineups, we'll use
    // the lineup indexer
    BattingLineupIndexer indexer = lineupType.getLineupIndexer(battingData, playersInLineup);

    // It might make sense to take a best guess at the optimal lineup by sorting
    // each batter by AVG.
    // If the first lineup is a reasonably good one, we'll be comparing new lineups
    // against it and since
    // it's more likely there will be a greater difference between mean runs scored
    // for this lineup
    // we'll have to run less simulations to detect that difference.
    List<DataPlayer> firstLineup = indexer.getLineup(0).asList();
    List<DataPlayer> modifiableList = new ArrayList<>(firstLineup);
    Collections.sort(modifiableList, new Comparator<DataPlayer>() {
      @Override
      public int compare(DataPlayer o1, DataPlayer o2) {
        double diff = (o2.getBattingAverage() - o1.getBattingAverage());
        if (diff < 0) {
          return -1;
        } else if (diff > 0) {
          return 1;
        }
        return 0;
      }
    });
    List<String> playerIdsSortedByBattingAverage = modifiableList.stream().map(v -> v.getId())
        .collect(Collectors.toList());
    indexer = lineupType.getLineupIndexer(battingData, playerIdsSortedByBattingAverage);

    // Our optimizer is parallelizable so we want to take advantage of multiple
    // cores
    ExecutorService executor = Executors.newFixedThreadPool(parsedArguments.getThreads());
    Queue<Future<TTestTaskResult>> results = new LinkedList<>();

    /*
     * Build a hitGenerator that can be used across threads, this way we only have to parse the stats
     * data once. We're using the first lineup here (index 0) to get a list of players, but we could
     * have used any lineup.
     */
    List<DataPlayer> someLineup = indexer.getLineup(0).asList();
    HitGenerator hitGenerator = new HitGenerator(someLineup);

    // This section involves setting up variables used by the simulation including restoring a paused
    // simulation
    long simulationsRun = Optional.ofNullable(existingResult).map(v -> v.getSimulationsRequired()).orElse(0L);
    long comparisonsThatReachedSimLimit =
        Optional.ofNullable(existingResult).map(v -> v.getComparisonsThatReachedSimLimit()).orElse(0L);

    BattingLineup startingLineup = Optional.ofNullable(existingResult).map(MonteCarloAdaptiveResult::getLineup)
        // The serialized result does not save the players stats
        .map(lineup -> {
          lineup.populateStats(battingData);
          return lineup;
        }).orElse(indexer.getLineup(0));

    LineupComposite startingLineupComposite = new LineupComposite(startingLineup, hitGenerator, 0L);
    SynchronizedLineupCompositeWrapper bestLineupComposite = new SynchronizedLineupCompositeWrapper(
        startingLineupComposite);

    // This is where the best lineups from older tasks wait to be added to a new task.
    Queue<LineupComposite> winnersPool = new LinkedList<>();

    // Candidate list contains all lineups before the 'lineupIndex' that have not yet been eliminated,
    // they may been in the winners pool waiting to be added to a task, or they may have already been
    // assigned to a task
    Set<LineupComposite> candidateLineups = new LinkedHashSet<>();

    Set<Long> savedCandidateLineupIndexes = Optional.ofNullable(existingResult)
        .map(MonteCarloAdaptiveResult::getCandidateLineups).orElse(Collections.emptySet());
    for (Long linupIndex : savedCandidateLineupIndexes) {
      LineupComposite composite = new LineupComposite(indexer.getLineup(linupIndex), hitGenerator, linupIndex);
      candidateLineups.add(composite);
      winnersPool.add(composite);
    }

    // Queue up a few tasks to process (number of tasks is capped by TASK_BUFFER_SIZE)
    long startIndex = Optional.ofNullable(existingResult).map(v -> v.getCountCompleted()).orElse(1L); // 0th lineup will
                                                                                                      // already be
                                                                                                      // added
    lineupIndex = startIndex;

    for (int i = 0; i < TASK_BUFFER_SIZE; i++) {
      int taskSize = getNumberOfLineupsToAddToTask(indexer.size() - lineupIndex, parsedArguments.getThreads());
      long savedLineupIndexerIndex = this.lineupIndex;
      List<LineupComposite> lineupsToTest = getLineupsToTest(taskSize, winnersPool, hitGenerator, indexer);
      long newLineupsAdded = this.lineupIndex - savedLineupIndexerIndex;
      if (lineupsToTest.size() > 0) {
        TTestTask task = new TTestTaskWithBestLineup(bestLineupComposite, lineupsToTest, INNINGS, ALPHA,
            newLineupsAdded, LOWEST);
        results.add(executor.submit(task));
      }
    }

    // Process results, in order of submission, as soon as the earliest submitted task finishes
    // executing
    long progressCounter = startIndex;
    while (!results.isEmpty()) {
      // Wait for the result
      TTestTaskResult result = null;
      try {
        Future<TTestTaskResult> future = results.poll();
        if (future != null) {
          result = future.get();
        }
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }

      // Replace the best lineup if necessary
      if (result.getBestLineupComposite() != null) { // Null means do nothing
        // If the bestLineup is in the elimination list, update the bestLineup
        boolean wasReplaced = bestLineupComposite.replaceIfCurrentIsInCollection(result.getBestLineupComposite(),
            result.getEliminatedLineupComposites());
        if (!wasReplaced) {
          // Result lineup hasn't yet been compared to the current bestLineup (this should only happen in
          // multi-threaded use cases). Enqueue it for further evaluation.
          winnersPool.add(result.getBestLineupComposite());
        }
      }

      // Maintain the list of active lineups
      candidateLineups.removeAll(result.getEliminatedLineupComposites());
      if (result.getBestLineupComposite() != null) {
        candidateLineups.add(result.getBestLineupComposite());
      }

      // Keep track of the number of simulations run so far
      simulationsRun += result.getSimulationsRequired();

      // Keep track of lineup pairs for which we couldn't find a statistically significant difference in
      // score
      comparisonsThatReachedSimLimit += result.getComparisonsThatReachedSimLimit();

      // Update the progress tracker
      LineupComposite bestLineupCopy = bestLineupComposite.getCopyOfBestLineupComposite();

      // Ugly cast :(
      progressCounter += ((TTestTaskResultWithNewLineups) result).getNewLineupsProcessed();
      // Logger.log(progressCounter + " of " + indexer.size() + " " +
      // (result.getEliminatedLineupComposites().size()));

      Set<Long> candidateLineupIndexes = candidateLineups.stream().map(LineupComposite::lineupIndex)
          .collect(Collectors.toSet());

      long elapsedTime = (System.currentTimeMillis() - startTimestamp)
          + Optional.ofNullable(existingResult).map(MonteCarloAdaptiveResult::getElapsedTimeMs).orElse(0l);

      MonteCarloAdaptiveResult partialResult = new MonteCarloAdaptiveResult(bestLineupCopy.getLineup(),
          bestLineupCopy.getStats().getMean(), indexer.size(), progressCounter - candidateLineups.size(), elapsedTime,
          candidateLineupIndexes, ResultStatusEnum.IN_PROGRESS, simulationsRun, comparisonsThatReachedSimLimit);

      progressTracker.updateProgress(partialResult);

      // Add new tasks
      int taskSize = getNumberOfLineupsToAddToTask(indexer.size() - lineupIndex, parsedArguments.getThreads());
      long savedLineupIndexerIndex = this.lineupIndex;
      List<LineupComposite> lineupsToTest = getLineupsToTest(taskSize, winnersPool, hitGenerator, indexer);
      long newLineupsAdded = this.lineupIndex - savedLineupIndexerIndex;

      if (lineupsToTest.size() > 0) {
        TTestTask task = new TTestTaskWithBestLineup(bestLineupComposite, lineupsToTest, INNINGS, ALPHA,
            newLineupsAdded, LOWEST);
        results.add(executor.submit(task));
      }

      // Print a warning if the buffer gets low
      ThreadPoolExecutor ex = (ThreadPoolExecutor) executor;
      if (ex.getQueue().size() < parsedArguments.getThreads()
          && (indexer.size() - lineupIndex) > parsedArguments.getThreads()) {
        Logger.log("WARNING: Task buffer is low and this may affect multi-threaded performance. TaskSize "
            + ex.getQueue().size() + " " + (indexer.size() - lineupIndex));
      }

    }
    executor.shutdown();

    LineupComposite bestLineupCopy = bestLineupComposite.getCopyOfBestLineupComposite();

    /*
     * if (candidateLineupsGlobal.size() != 0) { throw new RuntimeException(
     * "There should no lineups remaining, but there were " + candidateLineupsGlobal.size()); }
     */

    // Make sure we've run at least MAX_ITERATIONS games on our final result so the expected score is
    // accurate.
    // This is especially important when using a cached result. Since stats objects can't be serialized
    // and cached, a final result will have a score of NaN
    if (bestLineupCopy.getStats().getN() < TTestTask.MAX_ITERATIONS) {
      Logger.log("Top up iterations: " + (TTestTask.MAX_ITERATIONS - bestLineupCopy.getStats().getN()));
      for (int i = 0; i < TTestTask.MAX_ITERATIONS - bestLineupCopy.getStats().getN(); i++) {
        double score = MonteCarloGameSimulation.simulateGame(bestLineupCopy.getLineup(), INNINGS,
            bestLineupCopy.getHitGenerator());
        bestLineupCopy.addSample(score);
      }
    }

    Set<Long> candidateLineupIndexes = candidateLineups.stream().map(v -> v.lineupIndex()).collect(Collectors.toSet());
    long elapsedTime = (System.currentTimeMillis() - startTimestamp)
        + Optional.ofNullable(existingResult).map(v -> v.getElapsedTimeMs()).orElse(0l);
    MonteCarloAdaptiveResult finalResult = new MonteCarloAdaptiveResult(bestLineupCopy.getLineup(),
        bestLineupCopy.getStats().getMean(), indexer.size(), indexer.size(), elapsedTime, candidateLineupIndexes,
        ResultStatusEnum.COMPLETE, simulationsRun, comparisonsThatReachedSimLimit);

    Logger.log(
        "FINAL RESULT " + finalResult.getSimulationsRequired() + " " + finalResult.getComparisonsThatReachedSimLimit());

    progressTracker.updateProgress(finalResult);
    return finalResult;
  }

  private List<LineupComposite> getLineupsToTest(int taskSize, Queue<LineupComposite> inProgressLineups,
      HitGenerator hitGenerator, BattingLineupIndexer indexer) {
    List<LineupComposite> lineupsToTest = new LinkedList<>(); // LinkedList becasue we only iterate and sometimes need
                                                              // to add elements to the beginning of the list
    for (int j = 0; j < taskSize; j++) {

      // First, add the best lineup we have so far to the task list. Make a copy so other threads can
      // modify it.
      // Having a good lineup in each task reduces the runtime because we don't waste much time comparing
      // bad lineups to other bad lineups.
      // lineupsToTest.add(new LineupComposite(bestLineup));

      // First, get any in progress lineups from the queue and add them to the task
      if (!inProgressLineups.isEmpty()) {
        LineupComposite toEnqueue = inProgressLineups.remove();
        lineupsToTest.add(toEnqueue);
        continue;
      }

      // Second, get fresh lineups
      if (lineupIndex < indexer.size()) {
        LineupComposite composite = new LineupComposite(indexer.getLineup(lineupIndex), hitGenerator, lineupIndex);
        lineupsToTest.add(composite);
        lineupIndex++;
      }

      if (lineupIndex >= indexer.size()) {
        // All done, no more lineups to enqueue for simulation
        break;
      }
    }

    return lineupsToTest;
  }

  private int getNumberOfLineupsToAddToTask(long remainingLineups, int numberOfThreads) {
    long dynamicCap = remainingLineups / numberOfThreads;
    if (dynamicCap > TASK_MAX_LINEUP_COUNT) {
      return TASK_MAX_LINEUP_COUNT;
    } else if (dynamicCap < TASK_MIN_LINEUP_COUNT) {
      return TASK_MIN_LINEUP_COUNT;
    } else {
      return Math.toIntExact(dynamicCap);
    }
  }

  private void validateData(DataStats data, List<String> playersInLineup) {
    // All players in the lineup must have at least one plate appearance
    for (String playerId : playersInLineup) {
      DataPlayer player = data.getPlayerById(playerId);
      if (player.getPlateAppearanceCount() == 0) {
        throw new RuntimeException(Msg.PLAYER_HAS_NO_PA.args(player.getName(), player.getId()));
      }
    }
  }

  @Override
  public Class<? extends Result> getResultClass() {
    return MonteCarloAdaptiveResult.class;
  }

  @Override
  public Result estimate(List<String> playersInLineup, LineupTypeEnum lineupType, DataStats battingData,
      Map<String, String> arguments, MonteCarloAdaptiveResult existingResult) throws Exception {
    validateData(battingData, playersInLineup);
    MonteCarloAdaptiveArgumentParser parsedArguments = new MonteCarloAdaptiveArgumentParser(arguments);
    BattingLineupIndexer indexer = lineupType.getLineupIndexer(battingData, playersInLineup);

    List<DataPlayer> someLineup = indexer.getLineup(0).asList();
    HitGenerator hitGenerator = new HitGenerator(someLineup);

    // Configurable params
    final double NUM_STD_DEVIATIONS = .8; // (set higher for shorter estimation times and vice versa)
    final int LINEUPS_TO_TEST = 100;

    SummaryStatistics dataStats = MiscUtils.getSummaryStatisticsForIndexer(indexer, 100, 10000, 7);
    SummaryStatisticsTransform transform =
        new RangeSummaryStatisticsTransform(dataStats.getStandardDeviation() * NUM_STD_DEVIATIONS);

    // Warmup
    // Test random lineups are within NUM_STD_DEVIATIONS of each other
    Queue<Future<TTestTaskResult>> results = new LinkedList<>();
    ExecutorService executor = Executors.newFixedThreadPool(parsedArguments.getThreads());
    for (int i = 0; i < LINEUPS_TO_TEST; i++) {
      long randomIndexA = ThreadLocalRandom.current().nextLong(0, indexer.size());
      long randomIndexB = ThreadLocalRandom.current().nextLong(0, indexer.size());

      BattingLineup randomLineupA = indexer.getLineup(randomIndexA);
      BattingLineup randomLineupB = indexer.getLineup(randomIndexB);

      List<LineupComposite> list = new ArrayList<>(2);
      list.add(new LineupComposite(randomLineupA, hitGenerator, randomIndexA));
      list.add(new LineupComposite(randomLineupB, hitGenerator, randomIndexB));

      TTestTask task = new TTestTask(list, parsedArguments.getInnings(), parsedArguments.getAlpha(), transform, false);
      results.add(executor.submit(task));
    }

    // Wait for the results to finish
    while (!results.isEmpty()) {
      Future<TTestTaskResult> future = results.poll();
      if (future != null) {
        future.get();
      }
    }

    // Actual
    long startTimeNanos = System.nanoTime();

    // Test random lineups are within NUM_STD_DEVIATIONS of each other
    for (int i = 0; i < LINEUPS_TO_TEST; i++) {
      long randomIndexA = ThreadLocalRandom.current().nextLong(0, indexer.size());
      long randomIndexB = ThreadLocalRandom.current().nextLong(0, indexer.size());

      BattingLineup randomLineupA = indexer.getLineup(randomIndexA);
      BattingLineup randomLineupB = indexer.getLineup(randomIndexB);

      List<LineupComposite> list = new ArrayList<>(2);
      list.add(new LineupComposite(randomLineupA, hitGenerator, randomIndexA));
      list.add(new LineupComposite(randomLineupB, hitGenerator, randomIndexB));

      TTestTask task = new TTestTask(list, parsedArguments.getInnings(), parsedArguments.getAlpha(), transform, false);
      results.add(executor.submit(task));
    }

    // Wait for the results to finish
    while (!results.isEmpty()) {
      Future<TTestTaskResult> future = results.poll();
      if (future != null) {
        future.get();
      }
    }

    executor.shutdown();

    long elapsedTimeNanos = System.nanoTime() - startTimeNanos;

    // Do some extrapolation math
    long estimationTimeNanos = (long) ((double) elapsedTimeNanos / LINEUPS_TO_TEST * indexer.size());
    long estimationTimeMs = TimeUnit.MILLISECONDS.convert(estimationTimeNanos, TimeUnit.NANOSECONDS);

    // Adjustments based on data points from my laptop, not sure if they hold across computers
    if (lineupType == LineupTypeEnum.STANDARD) {
      estimationTimeMs = (long) (Math.pow(estimationTimeMs, .7) * 45);
    } else if (lineupType == LineupTypeEnum.ALTERNATING_GENDER) {
      estimationTimeMs = (long) (Math.pow(estimationTimeMs, .85) * 18);
    } else if (lineupType == LineupTypeEnum.NO_CONSECUTIVE_FEMALES) {
      estimationTimeMs = (long) (Math.pow(estimationTimeMs, .45) * 600);
    }

    return new MonteCarloAdaptiveResult(estimationTimeMs);

  }

}
