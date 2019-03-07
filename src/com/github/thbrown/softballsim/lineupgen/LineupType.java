package com.github.thbrown.softballsim.lineupgen;

import com.github.thbrown.softballsim.LineupGeneratorFactory;

public enum LineupType implements LineupGeneratorFactory {

  // Register batting lineup generators here
  ORDINARY(() -> new OrdinaryBattingLineupGenerator()),
  ALTERNATING(() -> new AlternatingBattingLineupGenerator()),
  NO_CONSECUTIVE_FEMALES(() -> new NoConsecutiveFemalesLineupGenerator());

  final LineupGeneratorFactory lineupGeneratorFactory;

  private LineupType(LineupGeneratorFactory lineupGeneratorFactory) {
    this.lineupGeneratorFactory = lineupGeneratorFactory;
  }

  @Override
  public LineupGenerator getLineupGenerator() {
    return lineupGeneratorFactory.getLineupGenerator();
  }
}
