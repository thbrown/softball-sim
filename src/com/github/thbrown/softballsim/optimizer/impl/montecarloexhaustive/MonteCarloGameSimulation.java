package com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive;

import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.lineup.BattingLineup;
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

  private static HitResult[] hitResults = new HitResult[32];

  static {
    for (int i = 0; i < 32; i++) {
      // First three bits represent if a runner is present (1) on each base or not (0)
      boolean first = (i & 1) == 1;
      boolean second = (i >> 1 & 1) == 1;
      boolean third = (i >> 2 & 1) == 1;

      // Next two bits are the type of hit (00 - single, 01 - double, 10 - triple, 11 - homerun)
      // Add one so the value is the number of bases that a hit earns (e.g. double = 2 bases)
      int bases = (i >> 3) + 1;

      // Calculate the ending configuration
      int runsResultingFromHit = 0;
      for (int j = 0; j < bases; j++) {
        runsResultingFromHit += third ? 1 : 0;
        third = second;
        second = first;
        first = (j == 0) ? true : false;
      }

      // Save the results in a map for quick later lookup
      hitResults[i] = new HitResult(first, second, third, runsResultingFromHit);
    }
  }

  // Class that holds info about what the field and score look like.
  private static class HitResult {
    HitResult(boolean first, boolean second, boolean third, int runsScored) {
      this.first = first;
      this.second = second;
      this.third = third;
      this.runsScored = runsScored;
    }

    public boolean first;
    public boolean second;
    public boolean third;
    public int runsScored;
  }

  // Class that holds info about whether or not a player is on each base
  private static class BasesState {
    public boolean first = false;
    public boolean second = false;
    public boolean third = false;
  }

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
          runsThisInning += updateRunsAndBasesAfterHit(numBases, bases);
        } else {
          outs++;
        }

        if (VERBOSE) {
          String message =
              StringUtils.padRight(p.getName(), NAME_PADDING) +
                  "\t hit:" + mapBasesToHitType(numBases) +
                  "\t outs:" + outs +
                  "\t score:" + gameScore;
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

  private static int updateRunsAndBasesAfterHit(int numBases, BasesState bases) {
    // There 3 bases (2^3) that may or may not be holding a player
    // There are 4 non-out hit types (single, double, triple, hr) for 32 possibilities
    // We can enumerate all possibilities here for quick computation.
    int index = 0;
    // First three bits represent the presence (1) or absence (0) of runners on each base
    index |= bases.first ? 1 : 0;
    index |= bases.second ? 2 : 0;
    index |= bases.third ? 4 : 0;
    // Second two bits are the hit type
    index |= ((numBases - 1) << 3);

    // Lookup the solution in our map
    HitResult result = hitResults[index];
    bases.first = result.first;
    bases.second = result.second;
    bases.third = result.third;
    return result.runsScored;
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
