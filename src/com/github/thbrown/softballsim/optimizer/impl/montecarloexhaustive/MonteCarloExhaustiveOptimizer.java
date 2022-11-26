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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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
import com.github.thbrown.softballsim.util.Logger;

public class MonteCarloExhaustiveOptimizer implements Optimizer<MonteCarloExhaustiveResult> {

  private static int TASK_BUFFER_SIZE = 1000;

  @Override
  public MonteCarloExhaustiveResult optimize(List<String> playersInLineup, LineupTypeEnum lineupType,
      DataStats battingData, Map<String, String> arguments, ProgressTracker progressTracker,
      MonteCarloExhaustiveResult existingResult) throws Exception {

    // Start the timer
    long startTimestamp = System.currentTimeMillis();

    // Check that the batting data we have is sufficient to run this optimizer
    validateData(battingData, playersInLineup);

    // Get the arguments as their expected types
    MonteCarloExhaustiveArgumentParser parsedArguments = new MonteCarloExhaustiveArgumentParser(arguments);

    // Since this optimizer involves iterating over all possible lineups, we'll use
    // the lineup indexer
    BattingLineupIndexer indexer = lineupType.getLineupIndexer(battingData, playersInLineup);

    // Print the details before we start
    DecimalFormat formatter = new DecimalFormat("#,###");
    Logger.log("Possible lineups: \t\t" + formatter.format(indexer.size()));
    Logger.log("Games to simulate per lineup: \t" + parsedArguments.getGames());
    Logger.log("Innings per game: \t\t" + parsedArguments.getInnings());
    Logger.log("Threads used: \t\t\t" + parsedArguments.getThreads());
    Logger.log("Select lowest scoring lineup?: \t" + parsedArguments.isLowestScore());
    Logger.log("*********************************************************************");

    // Our optimizer is parallelizable so we want to take advantage of multiple
    // cores
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

    // Queue up a few tasks to process (number of tasks is capped by
    // TASK_BUFFER_SIZE)
    long startIndex = Optional.ofNullable(existingResult).map(v -> v.getCountCompleted()).orElse(0L);
    long max = indexer.size() - startIndex > TASK_BUFFER_SIZE ? TASK_BUFFER_SIZE + startIndex : indexer.size();
    for (long l = startIndex; l < max; l++) {
      MonteCarloMultiGameSimulationTask task = new MonteCarloMultiGameSimulationTask(indexer.getLineup(l),
          parsedArguments.getGames(), parsedArguments.getInnings(), hitGenerator);
      results.add(executor.submit(task));
    }

    double optimalScoreStart = parsedArguments.isLowestScore() ? Double.MAX_VALUE : 0;
    double oppositeOfOptimalScoreStart = parsedArguments.isLowestScore() ? 0 : Double.MAX_VALUE;

    // Process results as they finish executing
    double initialOppositeLineupScore =
        Optional.ofNullable(existingResult).map(v -> v.getOppositeOfOptimalScore()).orElse(oppositeOfOptimalScoreStart);
    BattingLineup initialOppositeLineup =
        Optional.ofNullable(existingResult).map(MonteCarloExhaustiveResult::getOppositeOfOptimalLineup)
            // The serialized result does not save the player's stats
            .map(lineup -> {
              lineup.populateStats(battingData);
              return lineup;
            }).orElse(null);

    double initialScore = Optional.ofNullable(existingResult).map(v -> v.getLineupScore()).orElse(optimalScoreStart);
    BattingLineup initialLineup = Optional.ofNullable(existingResult).map(MonteCarloExhaustiveResult::getLineup)
        // The serialized result does not save the player's stats
        .map(lineup -> {
          lineup.populateStats(battingData);
          return lineup;
        }).orElse(null);

    TaskResult optimalResult = new TaskResult(initialScore, initialLineup);
    TaskResult oppositeOfOptimalResult = new TaskResult(initialOppositeLineupScore, initialOppositeLineup);
    Map<Long, Long> histo = Optional.ofNullable(existingResult).map(v -> v.getHistogram())
        .orElse(new HashMap<Long, Long>());
    long progressCounter = startIndex; // Lineups completed
    long lineupQueueCounter = max; // Lineups ready to be enqueued
    while (!results.isEmpty()) {
      // Wait for the result
      TaskResult result = null;
      Future<TaskResult> future = results.poll();
      if (future != null) {
        result = future.get();
      }

      // Print Lineup Index and Score (For research purposes)
      // Logger.log(indexer.getIndex(result.getLineup()) + "\t" + result.getScore());

      // Update histogram
      long key = (long) (result.getScore() * 10);
      if (histo.containsKey(key)) {
        histo.put(key, histo.get(key) + 1);
      } else {
        histo.put(key, 1L);
      }

      if (parsedArguments.isLowestScore()) {
        // Update the optimal lineup, if necessary
        if (optimalResult == null || result.getScore() < optimalResult.getScore()) {
          optimalResult = result;
        }
        // Update the oppositeOfOptimal lineup, if necessary
        if (oppositeOfOptimalResult == null || result.getScore() > oppositeOfOptimalResult.getScore()) {
          oppositeOfOptimalResult = result;
        }
      } else {
        // Update the optimal lineup, if necessary
        if (optimalResult == null || result.getScore() > optimalResult.getScore()) {
          optimalResult = result;
        }
        // Update the oppositeOfOptimal lineup, if necessary
        if (oppositeOfOptimalResult == null || result.getScore() < oppositeOfOptimalResult.getScore()) {
          oppositeOfOptimalResult = result;
        }
      }

      // DEBUG: Print index and score
      // Logger.log(progressCounter + "\t" + result.getScore() + "\t");

      // Update the progress tracker
      progressCounter++;
      long elapsedTime = (System.currentTimeMillis() - startTimestamp)
          + Optional.ofNullable(existingResult).map(v -> v.getElapsedTimeMs()).orElse(0l);
      MonteCarloExhaustiveResult partialResult = new MonteCarloExhaustiveResult(optimalResult.getLineup(),
          optimalResult.getScore(), indexer.size(), progressCounter, elapsedTime, histo,
          ResultStatusEnum.IN_PROGRESS, oppositeOfOptimalResult.getLineup(), oppositeOfOptimalResult.getScore());
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
    MonteCarloExhaustiveResult finalResult = new MonteCarloExhaustiveResult(optimalResult.getLineup(),
        optimalResult.getScore(), indexer.size(), progressCounter, elapsedTime, histo,
        ResultStatusEnum.COMPLETE, oppositeOfOptimalResult.getLineup(), oppositeOfOptimalResult.getScore());
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

  @Override
  public Result estimate(List<String> playersInLineup, LineupTypeEnum lineupType, DataStats battingData,
      Map<String, String> arguments, MonteCarloExhaustiveResult existingResult) throws Exception {

    validateData(battingData, playersInLineup);
    MonteCarloExhaustiveArgumentParser parsedArguments = new MonteCarloExhaustiveArgumentParser(arguments);
    BattingLineupIndexer indexer = lineupType.getLineupIndexer(battingData, playersInLineup);

    List<DataPlayer> someLineup = indexer.getLineup(0).asList();
    HitGenerator hitGenerator = new HitGenerator(someLineup);

    // Warmup
    for (int i = 0; i < 10000; i++) {
      long randomIndex = ThreadLocalRandom.current().nextLong(indexer.size());
      BattingLineup randomLineup = indexer.getLineup(randomIndex);
      MonteCarloGameSimulation.simulateGame(randomLineup, parsedArguments.getInnings(), hitGenerator);
    }

    // Test, how many games can we simulate in 5 seconds;
    long startTime = System.nanoTime();
    long startTimeMillis = System.currentTimeMillis();
    long counter = 0;

    while ((System.currentTimeMillis() - startTimeMillis) < TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS)) {
      long randomIndex = ThreadLocalRandom.current().nextLong(indexer.size());
      BattingLineup randomLineup = indexer.getLineup(randomIndex);
      MonteCarloGameSimulation.simulateGame(randomLineup, parsedArguments.getInnings(), hitGenerator);
      counter++;
    }

    long elapsedTimeNanos = System.nanoTime() - startTime;

    Logger.log("Tested " + counter + " games in "
        + TimeUnit.MILLISECONDS.convert(elapsedTimeNanos, TimeUnit.NANOSECONDS) + "ms");

    long nanosPerGame = (long) ((double) elapsedTimeNanos / (counter));
    long totalGamesRequired = indexer.size() * parsedArguments.getGames();
    long nanosToTestAllGames = totalGamesRequired * nanosPerGame;

    Logger.log("We estimate being able to test " + totalGamesRequired + " games in "
        + TimeUnit.MILLISECONDS.convert(nanosToTestAllGames, TimeUnit.NANOSECONDS) + "ms");

    // Concurrent calc speedup (assuming perfect concurrency)
    long estimationTimeNanos = (long) ((double) nanosToTestAllGames
        / Math.min(parsedArguments.getThreads(), Runtime.getRuntime().availableProcessors()));

    Logger.log("Assuming " + Runtime.getRuntime().availableProcessors() + " concurrent processors, that can be completed in "
        + TimeUnit.MILLISECONDS.convert(estimationTimeNanos, TimeUnit.NANOSECONDS) + "ms");

    /*
     * // Parrellelizable ExecutorService executor =
     * Executors.newFixedThreadPool(parsedArguments.getThreads()); Queue<Future<TaskResult>> results =
     * new LinkedList<>();
     * 
     * List<DataPlayer> someLineup = indexer.getLineup(0).asList(); HitGenerator hitGenerator = new
     * HitGenerator(someLineup);
     * 
     * // Queue up a few tasks to process, this will serve as a warmup final long WARM_UP_ITERATIONS =
     * Math.min(indexer.size(), NUM_ITERATIONS / 2); for (long l = 0; l < WARM_UP_ITERATIONS; l++) {
     * MonteCarloMultiGameSimulationTask task = new
     * MonteCarloMultiGameSimulationTask(indexer.getLineup(l), parsedArguments.getGames(),
     * parsedArguments.getInnings(), hitGenerator); results.add(executor.submit(task)); }
     * 
     * // Wait for all the results to finish while (!results.isEmpty()) { Future<TaskResult> future =
     * results.poll(); if (future != null) { future.get(); } }
     * 
     * // Queue up a few tasks to process, we'll measure the elapsed time and extrapolate this to
     * determine // the optimization's execution time final long ESTIMATION_ITERATIONS =
     * Math.min(indexer.size(), NUM_ITERATIONS / 2); long startTime = System.nanoTime(); for (long l =
     * 0; l < ESTIMATION_ITERATIONS; l++) { MonteCarloMultiGameSimulationTask task = new
     * MonteCarloMultiGameSimulationTask(indexer.getLineup(l), parsedArguments.getGames(),
     * parsedArguments.getInnings(), hitGenerator); results.add(executor.submit(task)); }
     * 
     * // Wait for all the results to finish while (!results.isEmpty()) { Future<TaskResult> future =
     * results.poll(); if (future != null) { future.get(); } }
     * 
     * // TODO: Account for partial results??
     */

    long estimationTimeMs = TimeUnit.MILLISECONDS.convert(estimationTimeNanos, TimeUnit.NANOSECONDS);
    return new MonteCarloExhaustiveResult(estimationTimeMs);
  }
}
