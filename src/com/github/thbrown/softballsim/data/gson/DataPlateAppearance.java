package com.github.thbrown.softballsim.data.gson;

public class DataPlateAppearance {
  private String id;
  private String player_id;
  private String result;
  private DataLocation location;

  private transient DataTeam team;
  private transient DataGame game;
  private transient DataPlayer player;

  public String getId() {
    return id;
  }

  public String getPlayer_id() {
    return player_id;
  }

  public String getResult() {
    return result;
  }

  public DataLocation getLocation() {
    return location;
  }

  public void setGame(DataGame game) {
    this.game = game;
  }

  public void setTeam(DataTeam team) {
    this.team = team;
  }

  public void setPlayer(DataPlayer player) {
    this.player = player;
  }

  public DataGame getGame() {
    return game;
  }

  public DataTeam getTeam() {
    return team;
  }

  public DataPlayer getPlayer() {
    return player;
  }
}
