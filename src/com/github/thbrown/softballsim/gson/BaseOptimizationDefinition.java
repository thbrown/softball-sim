package com.github.thbrown.softballsim.gson;

import java.io.PrintWriter;
import java.util.List;

import com.github.thbrown.softballsim.Player;
import com.google.gson.Gson;

public abstract class BaseOptimizationDefinition {

  private List<Player> players;
  
  public List<Player> getPlayers() {
    return players;
  }

  public abstract void runSimulation(Gson gson, PrintWriter network);

}
