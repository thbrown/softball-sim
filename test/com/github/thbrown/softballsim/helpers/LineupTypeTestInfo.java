package com.github.thbrown.softballsim.helpers;

import java.util.ArrayList;
import java.util.List;
import com.github.thbrown.softballsim.data.gson.*;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;

public class LineupTypeTestInfo {

  private static LineupTypeTestInfo singleton;

  private LineupTypeTestInfo() {}

  public static LineupTypeTestInfo getInstance() {
    if (singleton == null) {
      singleton = new LineupTypeTestInfo();
    }
    return singleton;
  }

  public String[] getDefaultArgs(LineupTypeEnum type) {

    if (type == LineupTypeEnum.STANDARD) {
      /*
       * final int INNINGS = 5; final double ALPHA = .01;
       * 
       * String[] args = {"-o", LineupTypeEnum.STANDARD.name(), "-A", String.valueOf(ALPHA), "-I",
       * String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE), "-T", String.valueOf(THREAD_COUNT),
       * "-f"};
       * 
       * "-l", LINEUP,
       */

    } else if (type == LineupTypeEnum.ALTERNATING_GENDER) {

    } else if (type == LineupTypeEnum.NO_CONSECUTIVE_FEMALES) {

    } else if (type == LineupTypeEnum.NO_CONSECUTIVE_FEMALES_AND_NO_THREE_CONSECUTIVE_MALES) {

    } else {
      throw new RuntimeException("Unrecognized lineup type " + type);
    }

    return null;
  }

  public List<String> getTestLineup(LineupTypeEnum type, int length, DataStats stats) {

    if (type == LineupTypeEnum.STANDARD) {
      List<String> players = new ArrayList<>();
      players.add("Dora"); // F
      players.add("Tina"); // F
      players.add("Brianna"); // F
      players.add("Alexa"); // F
      players.add("Keenan"); // M
      players.add("Nelly"); // M
      players.add("Paul"); // M
      players.add("Ivan"); // M
      players.add("Tim"); // M
      players.add("Aaron"); // M
      players.add("Joe"); // M
      players.add("Sue"); // F
      return processAndTrim(players, length, stats);
    } else if (type == LineupTypeEnum.ALTERNATING_GENDER) {
      List<String> players = new ArrayList<>();
      players.add("Dora"); // F
      players.add("Nelly"); // M
      players.add("Tina"); // F
      players.add("Paul"); // M
      players.add("Brianna"); // F
      players.add("Ivan"); // M
      players.add("Alexa"); // F
      players.add("Tim"); // M
      players.add("Lulu"); // F
      players.add("Aaron"); // M
      players.add("Sue"); // F
      players.add("Joe"); // M
      return processAndTrim(players, length, stats);
    } else if (type == LineupTypeEnum.NO_CONSECUTIVE_FEMALES) {
      List<String> players = new ArrayList<>();
      players.add("Nelly"); // M
      players.add("Paul"); // M
      players.add("Dora"); // F
      players.add("Ivan"); // M
      players.add("Tim"); // M
      players.add("Tina"); // F
      players.add("Aaron"); // M
      players.add("Roger"); // M
      players.add("Brianna"); // F
      players.add("James"); // M
      players.add("Joe"); // M
      players.add("Sue"); // F
      return processAndTrim(players, length, stats);
    } else if (type == LineupTypeEnum.NO_CONSECUTIVE_FEMALES_AND_NO_THREE_CONSECUTIVE_MALES) {
      List<String> players = new ArrayList<>();
      players.add("Joe"); // M
      players.add("Aaron"); // M
      players.add("Dora"); // F
      players.add("Nelly"); // M
      players.add("Tina"); // F
      players.add("Paul"); // M
      players.add("Brianna"); // F
      players.add("Ivan"); // M
      players.add("Alexa"); // F
      players.add("Tim"); // M
      players.add("Lulu"); // F
      players.add("James"); // M
      return processAndTrim(players, length, stats);
    } else {
      throw new RuntimeException("Unrecognized lineup type " + type);
    }

  }

  private List<String> processAndTrim(List<String> players, int length, DataStats stats) {
    players = stats.convertPlayersListToIds(players);
    while (players.size() > length) {
      players.remove(players.size() - 1);
    }
    if (players.size() > length) {
      throw new RuntimeException("Can't get a lineup of lenfth " + length + " from " + players);
    }
    return players;
  }

}
