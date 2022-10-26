package com.github.thbrown.softballsim.data.gson;

import java.util.List;
import java.util.Set;

public class DataTeam {
  private List<DataGame> games;
  private String id;
  private String name;

  private transient DataGame team;
  private transient Set<DataPlayer> players;
  private transient Set<DataPlateAppearance> plateAppearances;

  public List<DataGame> getGames() {
    return games;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public DataGame getTeam() {
    return team;
  }

  public void setTeam(DataGame team) {
    this.team = team;
  }

  public Set<DataPlateAppearance> getPlateAppearances() {
    return plateAppearances;
  }

  public void setPlateAppearances(Set<DataPlateAppearance> plateAppearances) {
    this.plateAppearances = plateAppearances;
  }

  public Set<DataPlayer> getPlayers() {
    return players;
  }

  public void setPlayers(Set<DataPlayer> players) {
    this.players = players;
  }

}
