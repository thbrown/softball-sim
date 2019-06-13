package com.github.thbrown.softballsim.gson;

import java.io.PrintWriter;
import java.util.List;

import com.google.gson.Gson;

public abstract class BaseOptimizationData {

  private List<ParsedPlayerEntry> players;
  
  public List<ParsedPlayerEntry> getPlayers() {
    return players;
  }

  public abstract void runSimulation(Gson gson, PrintWriter network);

}
