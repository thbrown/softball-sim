package com.github.thbrown.softballsim;

import java.util.Map;
import java.util.concurrent.Callable;

import com.github.thbrown.softballsim.lineup.BattingLineup;

public class Simulation implements Callable<Result> {
  
  final private static boolean VERBOSE = false;
  private static final int NAME_PADDING = 24; // Just for formatting verbose output
  
  private static int MAX_RUNS_PER_INNING = 100;

  private BattingLineup lineup;
  private int numberOfGamesToSimulate;
  private int inningsPerGame;

  private boolean first; 
  private boolean second;
  private boolean third;
  
  private static HitResult[] hitResults = new HitResult[32];
  
  static {
    for(int i = 0; i < 32; i++) {
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

  Simulation(BattingLineup lineup, int numberOfGamesToSimulate, int inningsPerGame) {
	if(lineup == null) {
		Logger.log("NULL LINEUP");
	}
    this.lineup = lineup;
    this.numberOfGamesToSimulate = numberOfGamesToSimulate;
    this.inningsPerGame = inningsPerGame;
  }

  public Result call() {
    return run();
  }

  public Result run() {
    double totalScore = 0;

    // Full Simulation
    for (int i = 0; i < numberOfGamesToSimulate; i++) {

      // Game
      int gameScore = 0;
      for (int inning = 0; this.inningsPerGame > inning; inning++) {

        // Inning
        int outs = 0;
        int runsThisInning = 0;
        while (outs < 3 && runsThisInning < MAX_RUNS_PER_INNING) {
          Player p = lineup.getNextBatter();
          int bases = p.hit();
          if (bases > 0) {
            runsThisInning += updateRunsAndBasesAfterHit(bases);
          } else {
            outs++;
          }

          if (VERBOSE) {
            String message =
                padRight(p.getName(), NAME_PADDING) +
                    "\t hit:" + mapBasesToHitType(bases) +
                    "\t outs:" + outs +
                    "\t score:" + gameScore;
            Logger.log(message);
          }
        }
        gameScore += runsThisInning;
        if (VERBOSE) {
          Logger.log("--------------");
        }
        clearBases();
      }
      if (VERBOSE) {
        Logger.log("Runs Scored: " + gameScore);
        Logger.log("=============================================================");
      }
      totalScore += gameScore;
      lineup.reset();

    }
    
    double score = totalScore / numberOfGamesToSimulate;
    
    Result result = new Result(score, lineup);
    return result;
  }

  private int updateRunsAndBasesAfterHit(int bases) {
    // There 3 bases (2^3) that may or may not be holding a player
    // There are 4 non-out hit types (single, double, triple, hr) for 32 possibilities
    // We can enumerate all possibilities here for quick computation.
    /*
    int index = 0;
    // First three bits represent the presence (1) or absence (0) of runners on each base
    index |= this.first ? 1 : 0;
    index |= this.second ? 2 : 0;
    index |= this.third ? 4 : 0;
    // Second two bits are the hit type
    index |= ((bases-1) << 3);
    
    // Lookup the solution in our map
    HitResult result = hitResults[index];
    this.first = result.first;
    this.second = result.second;
    this.third = result.third;
    return result.runsScored;
    */
    return 0;
  }

  private void clearBases() {
    this.first = false;
    this.second = false;
    this.third = false;
  }

  private String mapBasesToHitType(int bases) {
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
              "Value was %s", bases));
    }
  }

  public BattingLineup getLineup() {
    return lineup;
  }

  // Thank you
  // http://stackoverflow.com/questions/388461/how-can-i-pad-a-string-in-java
  public static String padRight(String s, int n) {
    return String.format("%1$-" + n + "s", s);
  }
}
