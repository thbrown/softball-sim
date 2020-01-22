package com.github.thbrown.softballsim.lineup;

import java.util.ArrayList;
import java.util.List;
import com.github.thbrown.softballsim.data.gson.DataPlayer;

/**
 * Batting order that strictly alternates between two groups of players. (i.e. women and men or
 * older and younger). Batters in a group with less players will bat more often.
 * 
 * TODO: add optional out for skipping batter
 *
 * @author thbrown
 */
public class AlternatingBattingLineup implements BattingLineup {

  private List<DataPlayer> groupA;
  private List<DataPlayer> groupB;

  public AlternatingBattingLineup(List<DataPlayer> groupA, List<DataPlayer> groupB) {
    this.groupA = groupA;
    this.groupB = groupB;
    if (groupA.size() <= 0 || groupB.size() <= 0) {
      throw new IllegalArgumentException(String.format(
          "You must include at least one player of each gender.\n" +
              "Males: %s Females: %s .",
          groupA.size(), groupB.size()));
    }
  }


  @Override
  public List<DataPlayer> asList() {
    // This ordering is up for debate
    List<DataPlayer> lineup = new ArrayList<>();
    if (groupA.size() == groupB.size()) {
      // Interleave
      List<DataPlayer> as = new ArrayList<>(groupA);
      List<DataPlayer> bs = new ArrayList<>(groupB);
      for (int i = 0; i < groupA.size(); i++) {
        lineup.add(as.get(i));
        lineup.add(bs.get(i));
      }
    } else {
      // Group A first then Group B
      lineup.addAll(groupA);
      lineup.addAll(groupB);
    }
    return lineup;
  }

  @Override
  public DataPlayer getBatter(int index) {
    int adjustedIndex = index = index / 2;
    if (index % 2 == 0) {
      return groupA.get(adjustedIndex % groupA.size());
    } else {
      return groupB.get(adjustedIndex % groupB.size());
    }
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("GroupA").append("\n");
    result.append("\t").append(DataPlayer.getListHeader()).append("\n");
    for (DataPlayer p : groupA) {
      result.append("\t").append(p).append("\n");
    }

    result.append("GroupB").append("\n");
    result.append("\t").append(DataPlayer.getListHeader()).append("\n");
    for (DataPlayer p : groupB) {
      result.append("\t").append(p).append("\n");
    }
    return result.toString();

  }

  public static String getType() {
    return AlternatingBattingLineup.class.getSimpleName();
  }

}
