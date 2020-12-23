package com.github.thbrown.softballsim.optimizer.impl.expectedvalue;

import java.util.List;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.BasesUtil;
import com.github.thbrown.softballsim.util.Logger;

/**
 * Static class that calculates the expected number of runs from a given lineup of players
 */
public class ExpectedValue {

  public static int OUTS_PER_INNING = 3;

  public static double getExpectedValue(BattingLineup lineup, int inningsPerGame, int maxPlateAppearance) {
    HitsForEachPlayer playerHits = new HitsForEachPlayer(lineup);
    double expectedRuns = 0;
    double totalProbability = 0;
    long index = 0;

    // Keep simulating till we've done 99% of the possibilities
    while (totalProbability < .99) {
      long start = System.nanoTime();
      List<Integer> hits = playerHits.getHitsByIndex(index, inningsPerGame * OUTS_PER_INNING, maxPlateAppearance);
      long A = System.nanoTime();
      double score = ExpectedValue.getExpectedScore(hits);
      long B = System.nanoTime();
      double probability = ExpectedValue.getProbability(lineup, hits);
      long C = System.nanoTime();
      Logger.log((A - start) + " " + (B - A) + " " + (C - B));


      expectedRuns += (score * probability);
      totalProbability += probability;
      index++;
    }

    return expectedRuns;
  }

  /**
   * How many runs would the given array of hits produce?
   */
  private static double getExpectedScore(List<Integer> hits) {
    double score = 0;
    int outCounter = 0;
    BasesUtil.BasesState bases = new BasesUtil.BasesState();
    for (int hit : hits) {
      if (hit == 0) {
        outCounter++;
        // Reset the bases after three outs
        if (outCounter == 3) {
          bases.clear();
          continue;
        }
      }
      score += BasesUtil.updateRunsAndBasesAfterHit(hit, bases);
    }
    return score;
  }

  /**
   * What are the chances that the given lineup gets the given hit results?
   */
  private static double getProbability(BattingLineup lineup, List<Integer> hits) {
    int lineupIndex = 0;
    double probability = 1;
    for (int hit : hits) {
      DataPlayer batter = lineup.getBatter(lineupIndex);
      // TODO: cache this math
      if (hit == 0) {
        probability *= (double) (batter.getOutCount() + batter.getSacCount()) / batter.getPlateAppearanceCount();
      } else if (hit == 1) {
        probability *= (double) (batter.getSingleCount() + batter.getWalkCount()) / batter.getPlateAppearanceCount();
      } else if (hit == 2) {
        probability *= (double) (batter.getDoubleCount()) / batter.getPlateAppearanceCount();
      } else if (hit == 3) {
        probability *= (double) (batter.getTripleCount()) / batter.getPlateAppearanceCount();
      } else if (hit == 4) {
        probability *= (double) (batter.getHomerunCount()) / batter.getPlateAppearanceCount();
      }
      lineupIndex = (lineupIndex + 1) % lineup.size();
      if (probability == 0) {
        return 0;
      }
    }
    return probability;
  }
}
