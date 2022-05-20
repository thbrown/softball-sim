package com.github.thbrown.softballsim.lineupindexer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.util.Pair;
import com.github.thbrown.softballsim.lineup.BattingLineup;

/**
 * Associates a set of lineups with indices from 0 - lineups.size()
 */
public interface BattingLineupIndexer<T extends BattingLineup> {

  /**
   * Number of possible lineups
   */
  public long size();

  /**
   * All possible lineups are indexed between 0 and size() - 1. This method gets the lineup at the
   * given index.
   */
  public T getLineup(long index);

  /**
   * Given a lineup, this method returns its index.
   */
  public long getIndex(T lineup);

  /***
   * Given a lineup's index, get a neighboring lineup (and that neighbor's index).
   * 
   * The definition of "neighbor" is up to the implementer, but a good implementation will balance two
   * competing goals. Focusing on one of these at the expense of the other will result in a poor
   * implementation.
   * 
   * 1) Neighbors should have an estimated 'runs scored' value that is more alike than non-neighbors.
   * 
   * 2) It should be possible to reach the optimal lineup from any other lineup by successive calls to
   * getRandomNeighbor(...). The fewer steps required, the better the implementation. Because of this,
   * a single lineup typically has lots of neighbors.
   */
  public Pair<Long, T> getRandomNeighbor(long index);


}
