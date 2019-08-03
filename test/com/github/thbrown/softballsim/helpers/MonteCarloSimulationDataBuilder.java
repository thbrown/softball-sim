package com.github.thbrown.softballsim.helpers;

import java.util.Arrays;

import com.google.gson.Gson;

@SuppressWarnings("unused") // Fields are serialized
public class MonteCarloSimulationDataBuilder extends SimulationDataBuilder {

  private String optimizationType = "0";
  private String id = "<not_set>";
  private String iterations = "10000";
  private String lineupType = "1";
  private String startIndex = "0";
  private String innings = "7";
  private int threadCount = 1;

  private PlayerBuilder[] players;

  // TODO
  // private String initialLineup;
  // private String initialScore;
  // private String initialHisto;

  public MonteCarloSimulationDataBuilder withId(String id) {
    this.id = id;
    return this;
  }

  public MonteCarloSimulationDataBuilder withIterations(int iterations) {
    this.iterations = String.valueOf(iterations);
    return this;
  }

  public MonteCarloSimulationDataBuilder withLineupType(int lineupType) {
    this.lineupType = String.valueOf(lineupType);
    return this;
  }

  public MonteCarloSimulationDataBuilder withStartIndex(long startIndex) {
    this.startIndex = String.valueOf(startIndex);
    return this;
  }

  public MonteCarloSimulationDataBuilder withInnings(int innings) {
    this.innings = String.valueOf(innings);
    return this;
  }

  public MonteCarloSimulationDataBuilder withPlayers(PlayerBuilder[] players) {
    this.players = players;
    return this;
  }

  public MonteCarloSimulationDataBuilder withThreadCount(int threadCount) {
    this.threadCount = threadCount;
    return this;
  }

}
