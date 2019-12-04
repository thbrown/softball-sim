package com.github.thbrown.softballsim;

import java.util.Optional;
import com.github.thbrown.softballsim.lineup.BattingLineup;

public class Result {
  private final BattingLineup lineup;
  private final double lineupScore;
  private final long countTotal;
  private final long countCompleted;
  private final long elapsedTimeMs;

  public Result(BattingLineup lineup, double lineupScore, long countTotal, long countCompleted, long elapsedTimeMs) {
    this.lineup = lineup;
    this.lineupScore = lineupScore;
    this.countTotal = countTotal;
    this.countCompleted = countCompleted;
    this.elapsedTimeMs = elapsedTimeMs;
  }

  @Override
  public String toString() {
    return "Optimal lineup: \n"
        + Optional.ofNullable(lineup).map(v -> v.toString()).orElse("null") + "\n"
        + "Lineup expected score: " + this.lineupScore + "\n"
        + getHumanReadableDetails() + "\n"
        + "Elapsed time (ms): " + this.elapsedTimeMs;
  }

  public double getLineupScore() {
    return lineupScore;
  }

  public long getCountTotal() {
    return countTotal;
  }

  public long getCountCompleted() {
    return countCompleted;
  }

  public long getElapsedTimeMs() {
    return elapsedTimeMs;
  }

  public BattingLineup getLineup() {
    return lineup;
  }

  public String getHumanReadableDetails() {
    return "";
  }
}
