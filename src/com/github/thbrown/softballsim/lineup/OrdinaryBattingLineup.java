package com.github.thbrown.softballsim.lineup;

import java.util.List;
import java.util.Objects;
import com.github.thbrown.softballsim.data.gson.DataPlayer;

public class OrdinaryBattingLineup implements BattingLineup {

  private List<DataPlayer> players;

  public OrdinaryBattingLineup(List<DataPlayer> players) {
    this.players = players;
    if (players.size() <= 0) {
      throw new IllegalArgumentException("You must include at least one player in the lineup.");
    }
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
    return OrdinaryBattingLineup.class.getSimpleName();
  }

  @Override
  public int hashCode() {
    return Objects.hash(players);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof OrdinaryBattingLineup) {
      if (((OrdinaryBattingLineup) other).players.equals(this.players)) {
        return true;
      }
    }
    return false;
  }

}
