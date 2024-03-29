package com.github.thbrown.softballsim.data.gson.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.util.Logger;

/**
 * Adapter class that lets a BattingLineup be used as a DataPlayerLookup for purposes of
 * de-serialization.
 */
public class DataPlayerLookupImpl implements DataPlayerLookup {

  private Map<String, DataPlayer> dataPlayers;

  public DataPlayerLookupImpl(List<BattingLineup> lineups) {
    this.dataPlayers = new HashMap<>();
    for (BattingLineup lineup : lineups) {
      for (DataPlayer player : lineup.asList()) {
        if (dataPlayers.containsKey(player.getId()) && !dataPlayers.get(player.getId()).equals(player)) {
          // This indicates the dataPlayer objects are not singletons, not sure if that's a problem or not.
          Logger.warn("Conflicting stats provided for player " + player.getId());
        } else {
          dataPlayers.put(player.getId(), player);
        }
      }
    }
  }

  @Override
  public DataPlayer getDataPlayer(String playerId) {
    return dataPlayers.get(playerId);
  }

}
