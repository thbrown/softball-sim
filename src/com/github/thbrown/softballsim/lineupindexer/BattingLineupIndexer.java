package com.github.thbrown.softballsim.lineupindexer;

import com.github.thbrown.softballsim.lineup.BattingLineup;

public interface BattingLineupIndexer {

  /**
   * Number of possible lineups
   */
  public long size();

  /**
   * All possible lineups are indexed between 0 and size() - 1. This method gets the lineup at the
   * given index.
   */
  public BattingLineup getLineup(long index);

  /**
   * Given a lineup this method returns its index. TODO
   */
  // public BattingLineup getIndex(List<String> lineup);

}
