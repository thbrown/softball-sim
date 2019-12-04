package com.github.thbrown.softballsim.data.gson;

import java.util.List;

public class DataStats {
  private List<DataPlayer> players;
  private List<DataTeam> teams;

  public List<DataPlayer> getPlayers() {
    return players;
  }

  public List<DataTeam> getTeams() {
    return teams;
  }

  public String getPlayersAsCommaSeparatedString() {
    return String.join(",", players.stream().map(v -> v.getId()).toArray(String[]::new));
  }
}
