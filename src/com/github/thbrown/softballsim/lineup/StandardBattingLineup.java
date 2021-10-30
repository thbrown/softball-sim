package com.github.thbrown.softballsim.lineup;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;

public class StandardBattingLineup implements BattingLineup {

  private final List<DataPlayer> players;
  private final int size;

  public StandardBattingLineup(List<DataPlayer> players) {
    this.players = Collections.unmodifiableList(players);
    if (players.size() <= 0) {
      throw new IllegalArgumentException("You must include at least one player in the lineup.");
    }
    this.size = players.size();
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("\t").append(DataPlayer.getListHeader()).append("\n");
    for (DataPlayer p : players) {
      result.append("\t").append(p).append("\n");
    }
    return result.toString();
  }

  @Override
  public List<DataPlayer> asList() {
    return this.players;
  }

  @Override
  public DataPlayer getBatter(int index) {
    int adjustedIndex = index % players.size();
    return players.get(adjustedIndex);
  }

  public static String getType() {
    return StandardBattingLineup.class.getSimpleName();
  }

  @Override
  public int hashCode() {
    return Objects.hash(players);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof StandardBattingLineup) {
      if (((StandardBattingLineup) other).players.equals(this.players)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void populateStats(DataStats battingData) {
    for (int i = 0; i < players.size(); i++) {
      DataPlayer statslessPlayer = players.get(i);
      DataPlayer statsfullPlayer = battingData.getPlayerById(statslessPlayer.getId());
      if (statsfullPlayer == null) {
        throw new RuntimeException("Failed to populate stats for player " + statslessPlayer
            + " as no stats for this player were found in batting data. Try running the optimization again with the -F flag.");
      }
      players.set(i, statsfullPlayer);
    }
  }

  @Override
  public void populateStats(List<DataPlayer> playersWithStatsData) {
    // TODO Auto-generated method stub

    for (int i = 0; i < players.size(); i++) {
      DataPlayer statslessPlayer = players.get(i);
      DataPlayer statsfullPlayer =
          playersWithStatsData.stream().filter(v -> v.getId() == statslessPlayer.getId()).findAny().orElse(null);
      if (statsfullPlayer == null) {
        throw new RuntimeException("Failed to populate stats for player " + statslessPlayer
            + " as no stats for this player were found in batting data. Try running the optimization again with the -F flag.");
      }
      players.set(i, statsfullPlayer);
    }
  }

  @Override
  public int size() {
    return this.size;
  }


}
