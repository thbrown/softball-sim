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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import com.github.thbrown.softballsim.Msg;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineupindexer.BattingLineupIndexer;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.Optimizer;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.HitGenerator;
import com.github.thbrown.softballsim.util.Logger;

public class MonteCarloAdaptiveOptimizer implements Optimizer<MonteCarloAdaptiveResult> {

  @Override
  public String getJsonDefinitionFileName() {
    return "monte-carlo-adaptive.json";
  }

  private static final int TASK_MAX_LINEUP_COUNT = 10;
  private static final int TASK_MIN_LINEUP_COUNT = 1;

  private static final int TASK_BUFFER_SIZE = 2000;


  @Override
  public MonteCarloAdaptiveResult optimize(List<String> playersInLineup, LineupTypeEnum lineupType,
      DataStats battingData, Map<String, String> arguments, ProgressTracker progressTracker,
      MonteCarloAdaptiveResult existingResult) {

    // Start the timer
    long startTimestamp = System.currentTimeMillis();

    // Check that the batting data we have is sufficient to run this optmizer
    validateData(battingData, playersInLineup);

    // Get the arguments as their expected types
    MonteCarloAdaptiveArgumentParser parsedArguments = new MonteCarloAdaptiveArgumentParser(arguments);
    final double ALPHA = parsedArguments.getAlpha();

    // Since this optimizer involves iterating over all possible lineups, we'll use the lineup indexer
    BattingLineupIndexer indexer = lineupType.getLineupIndexer(battingData, playersInLineup);

    // It might make sense to take a best guess at the optimal lineup by sorting each batter by AVG.
    // If the first lineup is a reasonably good one, we'll be comparing new lineups against it and since
    // it's more likely there will be a greater difference between mean runs scored for this lineup
    // we'll
    // have to run less simulations to detect that difference. This would be better if we only had one
    // best lineup
    // instead of one best lineup for each thread like we have now.

    List<DataPlayer> firstLineup = indexer.getLineup(0).asList();
    Collections.sort(firstLineup, new Comparator<DataPlayer>() {
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
    List<String> playerIdsSortedByBattingAverage =
        firstLineup.stream().map(v -> v.getId()).collect(Collectors.toList());
    indexer = lineupType.getLineupIndexer(battingData, playerIdsSortedByBattingAverage);


    // Our optimizer is parallelizable so we want to take advantage of multiple cores
    ExecutorService executor = Executors.newFixedThreadPool(parsedArguments.getThreads());
    Queue<Future<TTestTaskResult>> results = new LinkedList<>();

    /*
     * Build a hitGenerator that can be used across threads, this way we only have to parse the stats
     * data once. We're using the first lineup here (index 0) to get a list of players, but we could
     * have used any lineup.
     */
    List<DataPlayer> someLineup = indexer.getLineup(0).asList();
    HitGenerator hitGenerator = new HitGenerator(someLineup);

    // We need to keep track of what lineups are looking the best so far, in case the simulations
    // gets interrupted we can restart where we left off
    Queue<LineupComposite> inProgressLineups = new LinkedList<>();
    Set<Long> preExistingCandidateLineupIndexes =
        Optional.ofNullable(existingResult).map(v -> v.getCandidateLineups()).orElse(Collections.emptySet());

    long simulationsRun = 0;
    LineupComposite firstLineupComposite = new LineupComposite(indexer.getLineup(0), hitGenerator, 0L);
    SynchronizationLineupCompositeWrapper bestLineup = new SynchronizationLineupCompositeWrapper(firstLineupComposite);
    Set<LineupComposite> candidateLineupsGlobal = new LinkedHashSet<>();
    for (Long l : preExistingCandidateLineupIndexes) {
      LineupComposite composite = new LineupComposite(indexer.getLineup(l), hitGenerator, l);
      candidateLineupsGlobal.add(composite);
      inProgressLineups.add(composite);
    }

    // Queue up a few tasks to process (number of tasks is capped by TASK_BUFFER_SIZE)
    long startIndex = Optional.ofNullable(existingResult).map(v -> v.getCountCompleted()).orElse(0L);
    long lineupIndex = startIndex;

    for (int i = 0; i < TASK_BUFFER_SIZE; i++) {
      List<LineupComposite> lineupsToTest = new ArrayList<>(10);

      int taskSize = getNumberOfLineupsToAddToTask(indexer.size() - lineupIndex,
          parsedArguments.getThreads());
      for (int j = 0; j < taskSize; j++) {

        // First get any in progress lineups from the queue and add them to the task
        if (!inProgressLineups.isEmpty()) {
          LineupComposite toEnqueue = inProgressLineups.remove();
          lineupsToTest.add(toEnqueue);
          continue;
        }

        // Second get fresh lineups
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

      TTestTask task = new TTestTask(lineupsToTest, parsedArguments.getInnings(), ALPHA, bestLineup);
      results.add(executor.submit(task));
    }

    // Process results as they finish executing
    long progressCounter = startIndex;
    LineupComposite bestLineupCompositeSoFar = bestLineup.getCopyOfBestLineupComposite();
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

      // Maintain the list of active lineups
      candidateLineupsGlobal.removeAll(result.getEliminatedLineupComposites());
      if (result.getBestLineupComposite() != null) {
        candidateLineupsGlobal.add(result.getBestLineupComposite());
        inProgressLineups.add(result.getBestLineupComposite());
      }
      simulationsRun += result.getSimulationsRequired();

      // Update the progress tracker
      progressCounter += result.getEliminatedLineupComposites().size();
      bestLineupCompositeSoFar = bestLineup.getCopyOfBestLineupComposite();
      Set<Long> candidateLineupIndexes =
          candidateLineupsGlobal.stream().map(v -> v.lineupIndex()).collect(Collectors.toSet());
      long elapsedTime = (System.currentTimeMillis() - startTimestamp)
          + Optional.ofNullable(existingResult).map(v -> v.getElapsedTimeMs()).orElse(0l);
      MonteCarloAdaptiveResult partialResult = new MonteCarloAdaptiveResult(bestLineupCompositeSoFar.getLineup(),
          bestLineupCompositeSoFar.getStats().getMean(), indexer.size(), progressCounter, elapsedTime,
          candidateLineupIndexes, simulationsRun);
      progressTracker.updateProgress(partialResult);

      // Add task - TODO: this is duplicate code from above, clean it up
      int numberOfLineupsToTest =
          getNumberOfLineupsToAddToTask(indexer.size() - lineupIndex, parsedArguments.getThreads());
      List<LineupComposite> lineupsToTest = new ArrayList<>(numberOfLineupsToTest);
      for (int j = 0; j < numberOfLineupsToTest; j++) {
        // First get any in progress lineups from the queue and add them to the task
        if (!inProgressLineups.isEmpty()) {
          LineupComposite toEnqueue = inProgressLineups.remove();
          lineupsToTest.add(toEnqueue);
          continue;
        }
        // Second get fresh lineups
        if (lineupIndex < indexer.size()) {
          LineupComposite composite = new LineupComposite(indexer.getLineup(lineupIndex), hitGenerator, lineupIndex);
          lineupsToTest.add(composite);
          lineupIndex++;
        }
      }

      if (lineupsToTest.size() > 0) {
        TTestTask task = new TTestTask(lineupsToTest, parsedArguments.getInnings(), ALPHA, bestLineup);
        results.add(executor.submit(task));
      }


      // Print a warning if the buffer gets low
      ThreadPoolExecutor ex = (ThreadPoolExecutor) executor;
      if (ex.getQueue().size() < parsedArguments.getThreads()
          && (indexer.size() - lineupIndex) > parsedArguments.getThreads()) {
        Logger.log("WARNING: Task buffer is low and this may affect multithreaded performance. TaskSize "
            + ex.getQueue().size() + " " + (indexer.size() - lineupIndex));
      }

    }
    executor.shutdown();

    /*
     * if (candidateLineupsGlobal.size() != 0) { throw new RuntimeException(
     * "There should no lineups remaining, but there were " + candidateLineupsGlobal.size()); }
     */

    Set<Long> candidateLineupIndexes =
        candidateLineupsGlobal.stream().map(v -> v.lineupIndex()).collect(Collectors.toSet());
    long elapsedTime = (System.currentTimeMillis() - startTimestamp)
        + Optional.ofNullable(existingResult).map(v -> v.getElapsedTimeMs()).orElse(0l);
    MonteCarloAdaptiveResult finalResult = new MonteCarloAdaptiveResult(bestLineupCompositeSoFar.getLineup(),
        bestLineupCompositeSoFar.getStats().getMean(), indexer.size(), indexer.size(), elapsedTime,
        candidateLineupIndexes, simulationsRun);
    progressTracker.updateProgress(finalResult);
    return finalResult;
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

}
