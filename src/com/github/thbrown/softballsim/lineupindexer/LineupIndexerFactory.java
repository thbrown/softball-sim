package com.github.thbrown.softballsim.lineupindexer;

import java.util.List;
import com.github.thbrown.softballsim.data.gson.DataStats;

/**
 * Functional interface used for registering lineup indexers in {@link LineupTypeEnum}.
 *
 * @author thbrown
 */
public interface LineupIndexerFactory {

  public BattingLineupIndexer getLineupIndexer(DataStats stats, List<String> players);

}
