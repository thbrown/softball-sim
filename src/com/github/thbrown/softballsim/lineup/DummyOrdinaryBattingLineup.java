package com.github.thbrown.softballsim.lineup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.thbrown.softballsim.Player;

/**
 * Batting order that holds the player names in the same manner as OrdinaryBattingLineup
 * but without any of their batting information,
 * 
 * @author thbrown
 */
public class DummyOrdinaryBattingLineup implements BattingLineup {

  private List<String> players;

  public DummyOrdinaryBattingLineup(List<String> playerNames) {
    super();
    if (players.size() <= 0) {
      throw new IllegalArgumentException("You must include at least one player in the lineup.");
    }
  }

  @Override
  public Player getNextBatter() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void reset() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("Players").append("\n");
    for (String p : players) {
      result.append("\t").append(p).append("\n");
    }
    return result.toString();
  }
  
  @Override
  public Map<String, List<String>> toMap() {
    Map<String,List<String>>result = new HashMap<>();
    result.put("GroupA", players);
    return result;
  }

  @Override
  public BattingLineup getRandomSwap() {
    throw new UnsupportedOperationException();
  }
}
