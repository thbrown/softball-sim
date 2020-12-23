package com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive;

import java.util.Optional;
import com.github.thbrown.softballsim.lineup.BattingLineup;

public class TaskResult {
  private double avgScore;
  private BattingLineup lineup;

  public TaskResult(double score, BattingLineup lineup) {
    this.avgScore = score;
    this.lineup = lineup;
  }

  public double getScore() {
    return this.avgScore;
  }

  public BattingLineup getLineup() {
    return this.lineup;
  }

  @Override
  public String toString() {
    return avgScore + " " + Optional.ofNullable(lineup).map(v -> v.toString()).orElse("null");
  }
}
