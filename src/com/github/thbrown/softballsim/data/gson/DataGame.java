package com.github.thbrown.softballsim.data.gson;

import java.util.List;
import java.util.Set;

public class DataGame {
  private List<DataPlateAppearance> plateAppearances;
  private String id;
  private String opponent;
  private long date;
  private String park;
  private int lineupType;
  private List<String> lineup;

  private transient DataTeam team;
  private transient Set<DataPlayer> players;

  public List<DataPlateAppearance> getPlateAppearances() {
    return plateAppearances;
  }

  public void setPlateAppearances(List<DataPlateAppearance> plateAppearances) {
    this.plateAppearances = plateAppearances;
  }

  public String getId() {
    return id;
  }

  public String getOpponent() {
    return opponent;
  }

  public long getDate() {
    return date;
  }

  public String getPark() {
    return park;
  }

  public int getLineupType() {
    return lineupType;
  }

  public List<String> getLineup() {
    return lineup;
  }

  public void setTeam(DataTeam team) {
    this.team = team;
  }

  public void setPlayers(Set<DataPlayer> players) {
    this.players = players;
  }

  public DataTeam getTeam() {
    return this.team;
  }

  public Set<DataPlayer> getPlayers() {
    return this.players;
  }
}
