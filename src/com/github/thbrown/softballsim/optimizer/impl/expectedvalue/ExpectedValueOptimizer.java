package com.github.thbrown.softballsim.optimizer.impl.expectedvalue;

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
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineupindexer.BattingLineupIndexer;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.Optimizer;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.HitGenerator;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloExhaustiveResult;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.TaskResult;
import com.github.thbrown.softballsim.util.Logger;

public class ExpectedValueOptimizer implements Optimizer<MonteCarloExhaustiveResult> {

  private static int TASK_BUFFER_SIZE = 1000;

  @Override
  public MonteCarloExhaustiveResult optimize(List<String> playersInLineup, LineupTypeEnum lineupType,
      DataStats battingData, Map<String, String> arguments, ProgressTracker progressTracker,
      MonteCarloExhaustiveResult existingResult) {

    // Start the timer
    long startTimestamp = System.currentTimeMillis();

    // Check that the batting data we have is sufficient to run this optmizer
    validateData(battingData, playersInLineup);

    // Get the arguments as their expected types
    ExpectedValueArgumentParser parsedArguments = new ExpectedValueArgumentParser(arguments);

    // Since this optimizer involves iterating over all possible lineups, we'll use
    // the lineup indexer
    BattingLineupIndexer indexer = lineupType.getLineupIndexer(battingData, playersInLineup);

    // Print the details before we start
    DecimalFormat formatter = new DecimalFormat("#,###");
    Logger.log("Possible lineups: \t\t" + formatter.format(indexer.size()));
    Logger.log("Maximum batters per game: \t" + parsedArguments.getMaxBatters());
    Logger.log("Innings per game: \t\t" + parsedArguments.getInnings());
    Logger.log("Threads used: \t\t\t" + parsedArguments.getThreads());
    Logger.log("Lowest?: \t\t\t" + parsedArguments.isLowestScore());
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
      ExpectedValueTask task = new ExpectedValueTask(indexer.getLineup(l), parsedArguments.getMaxBatters(),
          parsedArguments.getInnings());
      results.add(executor.submit(task));
    }

    double optimalScoreStart = parsedArguments.isLowestScore() ? Double.MAX_VALUE : 0;
    double oppositeOfOptimalScoreStart = parsedArguments.isLowestScore() ? 0 : Double.MAX_VALUE;

    // Process results as they finish executing
    double initialOppositeLineupScore =
        Optional.ofNullable(existingResult).map(v -> v.getOppositeOfOptimalScore()).orElse(oppositeOfOptimalScoreStart);
    BattingLineup initialOppositeLineup =
        Optional.ofNullable(existingResult).map(MonteCarloExhaustiveResult::getOppositeOfOptimalLineup)
            // The serialized result does not save the players stats
            .map(lineup -> {
              lineup.populateStats(battingData);
              return lineup;
            }).orElse(null);

    double initialScore = Optional.ofNullable(existingResult).map(v -> v.getLineupScore()).orElse(optimalScoreStart);
    BattingLineup initialLineup = Optional.ofNullable(existingResult).map(MonteCarloExhaustiveResult::getLineup)
        // The serialized result does not save the players stats
        .map(lineup -> {
          lineup.populateStats(battingData);
          return lineup;
        }).orElse(null);

    TaskResult optimalResult = new TaskResult(initialScore, initialLineup);
    TaskResult oppositeOfOptimalResult = new TaskResult(initialScore, initialLineup);
    Map<Long, Long> histo = Optional.ofNullable(existingResult).map(v -> v.getHistogram())
        .orElse(new HashMap<Long, Long>());
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

      // Update the progress tracker
      progressCounter++;
      long elapsedTime = (System.currentTimeMillis() - startTimestamp)
          + Optional.ofNullable(existingResult).map(v -> v.getElapsedTimeMs()).orElse(0l);
      ExpectedValueResult partialResult = new ExpectedValueResult(optimalResult.getLineup(), optimalResult.getScore(),
          indexer.size(), progressCounter, elapsedTime, histo, ResultStatusEnum.IN_PROGRESS,
          oppositeOfOptimalResult.getLineup(), oppositeOfOptimalResult.getScore());
      progressTracker.updateProgress(partialResult);

      // Add another task to the buffer if there are any left
      BattingLineup lineup = indexer.getLineup(lineupQueueCounter);
      if (lineup != null) {
        lineupQueueCounter++;
        ExpectedValueTask s = new ExpectedValueTask(lineup, parsedArguments.getMaxBatters(),
            parsedArguments.getInnings());
        results.add(executor.submit(s));

        // Good for debugging
        // ThreadPoolExecutor ex =(ThreadPoolExecutor)executor;
        // Logger.log("Adding task 2 " + ex.getQueue().size() + " " + ex.);
      }
    }
    executor.shutdown();
    long elapsedTime = (System.currentTimeMillis() - startTimestamp)
        + Optional.ofNullable(existingResult).map(v -> v.getElapsedTimeMs()).orElse(0l);
    ExpectedValueResult finalResult = new ExpectedValueResult(optimalResult.getLineup(), optimalResult.getScore(),
        indexer.size(), progressCounter, elapsedTime, histo, ResultStatusEnum.COMPLETE,
        oppositeOfOptimalResult.getLineup(), oppositeOfOptimalResult.getScore());
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
    // TODO Auto-generated method stub
    return null;
  }
}
