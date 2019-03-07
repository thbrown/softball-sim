package com.github.thbrown.softballsim.lineup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.thbrown.softballsim.Player;

/**
 * Batting order that holds the player names in the same manner as AlternatingBattingLineup
 * but without any of their batting information
 *
 * @author thbrown
 */
public class DummyAlternatingBattingLineup implements BattingLineup {

  private List<String> groupANames;
  private List<String> groupBNames;

  public DummyAlternatingBattingLineup(List<String> groupANames, List<String> groupBNames) {
    this.groupANames = groupANames;
    this.groupBNames = groupBNames;
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
    result.append("GroupA").append("\n");
    for (String p : groupANames) {
      result.append("\t").append(p).append("\n");
    }

    result.append("GroupB").append("\n");
    for (String p : groupBNames) {
      result.append("\t").append(p).append("\n");
    }
    return result.toString();
  }
  
  @Override
  public Map<String, List<String>> toMap() {
    Map<String,List<String>>result = new HashMap<>();
    result.put("GroupA", groupANames);
    result.put("GroupB", groupANames);
    return result;
  }
  
  @Override
  public BattingLineup getRandomSwap() {
    throw new UnsupportedOperationException();
  }
  
}
