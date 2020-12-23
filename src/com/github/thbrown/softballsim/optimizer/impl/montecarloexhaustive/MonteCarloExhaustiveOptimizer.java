package com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.github.thbrown.softballsim.Msg;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineupindexer.BattingLineupIndexer;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.Optimizer;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.Logger;

public class MonteCarloExhaustiveOptimizer implements Optimizer<MonteCarloExhaustiveResult> {

  private static int TASK_BUFFER_SIZE = 1000;

  @Override
  public String getJsonDefinitionFileName() {
    return "monte-carlo-exhaustive.json";
  }

  @Override
  public MonteCarloExhaustiveResult optimize(List<String> playersInLineup, LineupTypeEnum lineupType,
      DataStats battingData,
      Map<String, String> arguments, ProgressTracker progressTracker, MonteCarloExhaustiveResult existingResult) {

    // Start the timer
    long startTimestamp = System.currentTimeMillis();

    // Check that the batting data we have is sufficient to run this optmizer
    validateData(battingData, playersInLineup);

    // Get the arguments as their expected types
    MonteCarloExhaustiveArgumentParser parsedArguments = new MonteCarloExhaustiveArgumentParser(arguments);

    // Since this optimizer involves iterating over all possible lineups, we'll use the lineup indexer
    BattingLineupIndexer indexer = lineupType.getLineupIndexer(battingData, playersInLineup);

    // Print the details before we start
    DecimalFormat formatter = new DecimalFormat("#,###");
    Logger.log("*********************************************************************");
    Logger.log("Possible lineups: \t\t" + formatter.format(indexer.size()));
    Logger.log("Games to simulate per lineup: \t" + parsedArguments.getGames());
    Logger.log("Innings per game: \t\t" + parsedArguments.getInnings());
    Logger.log("Threads used: \t\t\t" + parsedArguments.getThreads());
    Logger.log("*********************************************************************");

    // Our optimizer is parallelizable so we want to take advantage of multiple cores
    ExecutorService executor = Executors.newFixedThreadPool(parsedArguments.getThreads());
    Queue<Future<TaskResult>> results = new LinkedList<>();

    /*
     * Build a hitGenerator that can be used across threads, this way we only have to parse the stats
     * data once. We're using the first lineup here (index 0) to get a list of players, but we could
     * have used any lineup.
     */
    final long STARTING_INDEX = 0;
    List<DataPlayer> someLineup = indexer.getLineup(STARTING_INDEX).asList();
    HitGenerator hitGenerator = new HitGenerator(someLineup);

    // Queue up a few tasks to process (number of tasks is capped by TASK_BUFFER_SIZE)
    long startIndex = Optional.ofNullable(existingResult).map(v -> v.getCountCompleted()).orElse(0L);
    long max = indexer.size() - startIndex > TASK_BUFFER_SIZE ? TASK_BUFFER_SIZE + startIndex : indexer.size();
    for (long l = startIndex; l < max; l++) {
      MonteCarloMultiGameSimulationTask task =
          new MonteCarloMultiGameSimulationTask(indexer.getLineup(l), parsedArguments.getGames(),
              parsedArguments.getInnings(), hitGenerator);
      results.add(executor.submit(task));
    }

    // Process results as they finish executing
    double worstScore = Optional.ofNullable(existingResult).map(v -> v.getWorstScore()).orElse(Double.MAX_VALUE);
    double initialScore = Optional.ofNullable(existingResult).map(v -> v.getLineupScore()).orElse(0.0);
    BattingLineup initialLineup = Optional.ofNullable(existingResult).map(v -> v.getLineup()).orElse(null);
    if (existingResult != null) {
      initialLineup.populateStats(battingData);
    }
    TaskResult bestResult = new TaskResult(initialScore, initialLineup);
    Map<Long, Long> histo =
        Optional.ofNullable(existingResult).map(v -> v.getHistogram()).orElse(new HashMap<Long, Long>());
    long progressCounter = startIndex; // Lineups completed
    long lineupQueueCounter = max; // Lineups ready to be enqueued
    while (!results.isEmpty()) {
      // Wait for the result
      TaskResult result = null;
      try {
        Future<TaskResult> future = results.poll();
        if (future != null) {
          result = future.get();
        }
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }

      // Print Lineup Index and Score (For research purposes)
      // Logger.log(indexer.getIndex(result.getLineup()) + "\t" + result.getScore());

      // Update worst score
      if (result.getScore() < worstScore) {
        worstScore = result.getScore();
      }

      // Update histogram
      long key = (long) (result.getScore() * 10);
      if (histo.containsKey(key)) {
        histo.put(key, histo.get(key) + 1);
      } else {
        histo.put(key, 1L);
      }

      // Update the best lineup, if necessary
      if (bestResult == null || result.getScore() > bestResult.getScore()) {
        bestResult = result;
      }

      // Update the progress tracker
      progressCounter++;
      long elapsedTime = (System.currentTimeMillis() - startTimestamp)
          + Optional.ofNullable(existingResult).map(v -> v.getElapsedTimeMs()).orElse(0l);
      MonteCarloExhaustiveResult partialResult =
          new MonteCarloExhaustiveResult(bestResult.getLineup(),
              bestResult.getScore(), indexer.size(), progressCounter, elapsedTime, histo, worstScore);
      progressTracker.updateProgress(partialResult);

      // Add another task to the buffer if there are any left
      BattingLineup lineup = indexer.getLineup(lineupQueueCounter);
      if (lineup != null) {
        lineupQueueCounter++;
        MonteCarloMultiGameSimulationTask s = new MonteCarloMultiGameSimulationTask(lineup, parsedArguments.getGames(),
            parsedArguments.getInnings(), hitGenerator);
        results.add(executor.submit(s));

        // Good for debugging
        // ThreadPoolExecutor ex =(ThreadPoolExecutor)executor;
        // Logger.log("Adding task 2 " + ex.getQueue().size() + " " + ex.);
      }
    }
    executor.shutdown();
    long elapsedTime = (System.currentTimeMillis() - startTimestamp)
        + Optional.ofNullable(existingResult).map(v -> v.getElapsedTimeMs()).orElse(0l);
    MonteCarloExhaustiveResult finalResult =
        new MonteCarloExhaustiveResult(bestResult.getLineup(),
            bestResult.getScore(), indexer.size(), progressCounter, elapsedTime, histo, worstScore);
    return finalResult;
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
    return MonteCarloExhaustiveResult.class;
  }
}
