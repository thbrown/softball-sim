package com.github.thbrown.softballsim.lineup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.helpers.DataPlayerLookup;
import com.github.thbrown.softballsim.util.StringUtils;

/**
 * Batting order that strictly alternates between two groups of players. (i.e. women and men or
 * older and younger). Batters in a group with less players will bat more often.
 * 
 * TODO: add optional out for skipping batter
 *
 * @author thbrown
 */
public class AlternatingBattingLineup implements BattingLineup {

  private final List<DataPlayer> groupA;
  private final List<DataPlayer> groupB;

  private final int size;

  public AlternatingBattingLineup(List<DataPlayer> groupA, List<DataPlayer> groupB) {
    this.groupA = Collections.unmodifiableList(groupA);
    this.groupB = Collections.unmodifiableList(groupB);
    if (groupA.size() <= 0 || groupB.size() <= 0) {
      throw new IllegalArgumentException(String.format(
          "You must include at least one player of each gender.\n" +
              "Males: %s Females: %s .",
          groupA.size(), groupB.size()));
    }
    this.size = groupA.size() + groupB.size();
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
    int adjustedIndex = index / 2;
    if (index % 2 == 0) {
      return groupA.get(adjustedIndex % groupA.size());
    } else {
      return groupB.get(adjustedIndex % groupB.size());
    }
  }

  @Override
  public String toString() {
    final int SPACING = 3;
    final int INDENT = 1;

    // Max name length
    final int MAX_NAME_LENGTH_ALLOWED = 18;
    int maxNameLength = 0;
    for (DataPlayer p : groupA) {
      maxNameLength = p.getName().length() > maxNameLength ? p.getName().length() : maxNameLength;
    }
    for (DataPlayer p : groupB) {
      maxNameLength = p.getName().length() > maxNameLength ? p.getName().length() : maxNameLength;
    }
    maxNameLength = Math.min(maxNameLength, MAX_NAME_LENGTH_ALLOWED);

    StringBuilder builder = new StringBuilder();

    // Header 1
    builder.append("First Group").append("\n");
    builder.append(StringUtils.padLeft("", INDENT));
    builder.append(
        StringUtils.padRight("Name", maxNameLength + SPACING));
    // builder.append(StringUtils.padLeft("Id", 14 + SPACING));
    builder.append(StringUtils.padLeft("Avg", 5 + SPACING));
    builder.append(StringUtils.padLeft("Slg", 5 + SPACING));

    // Content 1
    for (DataPlayer p : groupA) {
      builder.append("\n");
      builder.append(StringUtils.padLeft("", INDENT));
      String truncatedName = StringUtils.trim(p.getName());
      builder
          .append(StringUtils.padRight(truncatedName, maxNameLength + SPACING));
      // builder.append(StringUtils.padLeft(p.getId(), 14 + SPACING));
      builder.append(StringUtils.padLeft(StringUtils.formatDecimal(p.getBattingAverage(), 3), 5 + SPACING));
      builder.append(StringUtils.padLeft(StringUtils.formatDecimal(p.getSluggingPercentage(), 3), 5 + SPACING));
    }
    builder.append("\n");

    // Header 2
    builder.append("Second Group").append("\n");
    builder.append(StringUtils.padLeft("", INDENT));
    builder.append(
        StringUtils.padRight("Name", maxNameLength + SPACING));
    // builder.append(StringUtils.padLeft("Id", 14 + SPACING));
    builder.append(StringUtils.padLeft("Avg", 5 + SPACING));
    builder.append(StringUtils.padLeft("Slg", 5 + SPACING));

    // Content 2
    for (DataPlayer p : groupB) {
      builder.append("\n");
      builder.append(StringUtils.padLeft("", INDENT));
      String truncatedName = StringUtils.trim(p.getName());
      builder
          .append(StringUtils.padRight(truncatedName, maxNameLength + SPACING));
      // builder.append(StringUtils.padLeft(p.getId(), 14 + SPACING));
      builder.append(StringUtils.padLeft(StringUtils.formatDecimal(p.getBattingAverage(), 3), 5 + SPACING));
      builder.append(StringUtils.padLeft(StringUtils.formatDecimal(p.getSluggingPercentage(), 3), 5 + SPACING));
    }
    return builder.toString();
  }

  public static String getType() {
    return AlternatingBattingLineup.class.getSimpleName();
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupA, groupB);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof AlternatingBattingLineup) {
      if (((AlternatingBattingLineup) other).groupB.equals(this.groupB)
          && ((AlternatingBattingLineup) other).groupA.equals(this.groupA)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void populateStats(DataPlayerLookup statsData) {
    for (int i = 0; i < groupA.size(); i++) {
      DataPlayer statslessPlayer = groupA.get(i);
      DataPlayer statsfullPlayer = statsData.getDataPlayer(statslessPlayer.getId());
      if (statsfullPlayer == null) {
        throw new RuntimeException("Failed to populate stats for player " + statslessPlayer
            + " as no stats for this player were found in batting data. Try running the optimization again with the -F flag.");
      }
      groupA.set(i, statsfullPlayer);
    }
    for (int i = 0; i < groupB.size(); i++) {
      DataPlayer statslessPlayer = groupB.get(i);
      DataPlayer statsfullPlayer = statsData.getDataPlayer(statslessPlayer.getId());
      if (statsfullPlayer == null) {
        throw new RuntimeException("Failed to populate stats for player " + statslessPlayer
            + " as no stats for this player were found in batting data. Try running the optimization again with the -F flag.");
      }
      groupB.set(i, statsfullPlayer);
    }
  }


  @Override
  public int size() {
    return this.size;
  }

  public List<DataPlayer> getGroupA() {
    return this.groupA;
  }

  public List<DataPlayer> getGroupB() {
    return this.groupB;
  }

  public String getDisplayInfo() {
    return this.toString();
  }

}
