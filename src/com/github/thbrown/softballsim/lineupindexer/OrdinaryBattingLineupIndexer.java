package com.github.thbrown.softballsim.lineupindexer;

import java.util.ArrayList;
import java.util.List;
import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.OrdinaryBattingLineup;
import com.github.thbrown.softballsim.util.CombinatoricsUtil;

public class OrdinaryBattingLineupIndexer implements BattingLineupIndexer {
  private List<DataPlayer> players = new ArrayList<>();
  private long size;

  public OrdinaryBattingLineupIndexer(DataStats stats, List<String> players) {
    // Get the DataPlayers by id, this isn't as efficient as it could be
    for (DataPlayer player : stats.getPlayers()) {
      if (players.contains(player.getId())) {
        this.players.add(player);
      }
    }
    try {
      this.size = CombinatoricsUtil.factorial(players.size());
    } catch (RuntimeException r) {
      throw new RuntimeException("Unable to index all possible lineups with that many players ("
          + stats.getPlayers().size()
          + "). If you have not specified a list of players, do so using the '" + CommandLineOptions.PLAYERS_IN_LINEUP
          + "' flag. Otherwise, don't specify more than 20 players", r);
    }
  }

  @Override
  public long size() {
    return this.size;
  }

  @Override
  public BattingLineup getLineup(long index) {
    if (index >= this.size) {
      return null;
    }
    int[] order = CombinatoricsUtil.getIthPermutation(players.size(), index);
    List<DataPlayer> lineup = CombinatoricsUtil.mapListToArray(players, order);
    return new OrdinaryBattingLineup(lineup);
  }

}
