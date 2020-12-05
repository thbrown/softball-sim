package com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive;

import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.BasesUtil.BasesState;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;

/**
 * Static class that run Monte Carlo simulations of the offensive side of a game using player's
 * supplied hitting data.
 */
public class MonteCarloGameSimulation {

  final private static boolean VERBOSE = false;
  private static final int NAME_PADDING = 24; // Just for formatting verbose output

  private static final int MAX_RUNS_PER_INNING = 100;

  public static double simulateGame(BattingLineup lineup, int inningsPerGame, HitGenerator hitGenerator) {

    // Game
    BasesState bases = new BasesState();
    int gameScore = 0;
    int batterIndex = 0;
    for (int inning = 0; inningsPerGame > inning; inning++) {

      // Inning
      int outs = 0;
      int runsThisInning = 0;
      while (outs < 3 && runsThisInning < MAX_RUNS_PER_INNING) {
        DataPlayer p = lineup.getBatter(batterIndex);
        batterIndex++;
        int numBases = hitGenerator.hit(p.getId());
        if (numBases > 0) {
          runsThisInning += BasesUtil.updateRunsAndBasesAfterHit(numBases, bases);
        } else {
          outs++;
        }

        if (VERBOSE) {
          String message =
              StringUtils.padRight(p.getName(), NAME_PADDING) +
                  "\t hit:" + mapBasesToHitType(numBases) +
                  "\t outs:" + outs +
                  "\t score:" + (gameScore + runsThisInning);
          Logger.log(message);
        }
      }
      gameScore += runsThisInning;
      if (VERBOSE) {
        Logger.log("--------------");
      }
      clearBases(bases);
    }
    if (VERBOSE) {
      Logger.log("Runs Scored: " + gameScore);
      Logger.log("=============================================================");
    }
    return gameScore;
  }

  private static void clearBases(BasesState bases) {
    bases.first = false;
    bases.second = false;
    bases.third = false;
  }

  private static String mapBasesToHitType(int bases) {
    switch (bases) {
      case 0:
        return "out";
      case 1:
        return "single";
      case 2:
        return "double";
      case 3:
        return "triple";
      case 4:
        return "homerun";
      default:
        throw new IllegalArgumentException(String.format(
            "Something is wrong, bases must be between 0 and 4 inclusive." +
                "Value was %s",
            bases));
    }
  }
}
