package com.github.thbrown.softballsim.data.gson;

import java.lang.reflect.Field;

public class DataPlateAppearance {
  private String id;
  private String playerId;
  private String result;
  private DataLocation location;

  private transient DataTeam team;
  private transient DataGame game;
  private transient DataPlayer player;

  public String getId() {
    return this.id;
  }

  public String getPlayerId() {
    return this.playerId;
  }

  public String getResult() {
    return this.result;
  }

  public DataLocation getLocation() {
    return this.location;
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
