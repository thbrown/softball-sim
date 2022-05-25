package com.github.thbrown.softballsim.optimizer.impl.sortbyaverage;

import com.github.thbrown.softballsim.optimizer.Optimizer;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.HitGenerator;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloGameSimulation;
import com.github.thbrown.softballsim.util.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineupindexer.BattingLineupIndexer;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;

public class SortByAverageOptimizer implements Optimizer<SortByAverageResult> {

  @Override
  public Class<? extends Result> getResultClass() {
    return SortByAverageResult.class;
  }

  @Override
  public Result optimize(List<String> playersInLineup, LineupTypeEnum lineupType, DataStats battingData,
      Map<String, String> arguments, ProgressTracker progressTracker, SortByAverageResult existingResult)
      throws Exception {

    long startTimestamp = System.currentTimeMillis();



    BattingLineup best = null;
    BattingLineupIndexer<?> indexer = lineupType.getLineupIndexer(battingData, playersInLineup);
    if (lineupType == LineupTypeEnum.STANDARD || lineupType == LineupTypeEnum.ALTERNATING_GENDER) {
      // For these lineup types we can just sort the players to avoid iterating over all the lineups.
      List<DataPlayer> firstLineup = indexer.getLineup(0).asList();
      List<DataPlayer> modifiableList = new ArrayList<>(firstLineup);
      Collections.sort(modifiableList, Comparator.comparing(DataPlayer::getBattingAverage).reversed());
      List<String> playerIdsSortedByBattingAverage = modifiableList.stream().map(v -> v.getId())
          .collect(Collectors.toList());
      indexer = lineupType.getLineupIndexer(battingData, playerIdsSortedByBattingAverage);
      best = indexer.getLineup(0);
    } else {
      /*
       * For all other lineups, we'll iterate over all the lineups selecting the one which minimizes the
       * square of any decrease in avg between consecutive players. This is slower, but works for all
       * lineups types.
       */
      double bestScore = Integer.MAX_VALUE;
      for (long l = 0; l < indexer.size(); l++) {
        BattingLineup lineup = indexer.getLineup(l);
        double lineupScore = 0;
        for (int i = 1; i < lineup.size(); i++) {
          DataPlayer prev = lineup.asList().get(i - 1);
          DataPlayer curr = lineup.asList().get(i);
          /*
           * DataPlayer prev = lineup.asList().get(i - 1); DataPlayer curr = lineup.asList().get(i);
           */
          if (curr.getBattingAverage() > prev.getBattingAverage()) {
            lineupScore += Math.pow(curr.getBattingAverage() - prev.getBattingAverage(), 2);
          }
        }
        // Lower score is better
        if (lineupScore < bestScore) {
          bestScore = lineupScore;
          best = lineup;
        }
      }
    }

    // Estimate lineup score using monte carlo simulations
    HitGenerator hitGenerator = new HitGenerator(best.asList());
    final double COUNT = 1000000;
    double sum = 0;
    for (int i = 0; i < COUNT; i++) {
      sum += MonteCarloGameSimulation.simulateGame(best, 7, hitGenerator);
    }
    double estimatedScore = sum / COUNT;
    long elapsedTime = (System.currentTimeMillis() - startTimestamp);

    return new SortByAverageResult(best, estimatedScore, 1, 1, elapsedTime, ResultStatusEnum.COMPLETE);
  }

  @Override
  public Result estimate(List<String> playersInLineup, LineupTypeEnum lineupType, DataStats battingData,
      Map<String, String> arguments, SortByAverageResult existingResult) throws Exception {
    return new SortByAverageResult(2000);
  }

}
