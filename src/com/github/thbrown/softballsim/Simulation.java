package com.github.thbrown.softballsim;

import com.github.thbrown.softballsim.lineup.BattingLineup;

public class Simulation {

  private BattingLineup lineup;

  private boolean first;
  private boolean second;
  private boolean third;

  Simulation(BattingLineup lineup) {
    this.lineup = lineup;
  }

  public double run(int numberOfGamesToSimulate) {
    int totalScore = 0;

    // Full Simulation
    for (int i = 0; i < numberOfGamesToSimulate; i++) {

      // Game
      int gameScore = 0;
      for (int inning = 0; SoftballSim.INNINGS_PER_GAME > inning; inning++) {

        // Inning
        int outs = 0;
        while (outs < 3) {
          Player p = lineup.getNextBatter();
          int bases = p.hit();
          if (bases > 0) {
            gameScore += updateRunsAndBasesAfterHit(bases);
          } else {
            outs++;
          }

          if (SoftballSim.VERBOSE) {
            String message =
                padRight(p.name, SoftballSim.NAME_PADDING) +
                    "\t hit:" + mapBasesToHitType(bases) +
                    "\t outs:" + outs +
                    "\t score:" + gameScore;
            System.out.println(message);
          }
        }
        if (SoftballSim.VERBOSE) {
          System.out.println("--------------");
        }
        clearBases();

      }
      if (SoftballSim.VERBOSE) {
        System.out.println("Runs Scrored: " + gameScore);
        System.out.println("=============================================================");
      }
      totalScore += gameScore;
      lineup.reset();

    }
    return (double) totalScore / numberOfGamesToSimulate;
  }

  private int updateRunsAndBasesAfterHit(int bases) {
    int runsResultingFromHit = 0;
    for (int i = 0; i < bases; i++) {
      // TODO: Consider if sometimes the runner should score from 2nd.
      runsResultingFromHit += this.third ? 1 : 0;
      this.third = this.second;
      this.second = this.first;
      this.first = (i == 0) ? true : false;
    }
    return runsResultingFromHit;
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

  // Thank you
  // http://stackoverflow.com/questions/388461/how-can-i-pad-a-string-in-java
  public static String padRight(String s, int n) {
    return String.format("%1$-" + n + "s", s);
  }
}
