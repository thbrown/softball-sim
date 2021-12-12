package com.github.thbrown.softballsim.lineup;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.helpers.DataPlayerLookup;
import com.github.thbrown.softballsim.util.StringUtils;

public class StandardBattingLineup implements BattingLineup {

  private final List<DataPlayer> players;
  private final int size;

  public StandardBattingLineup(List<DataPlayer> players) {
    this.players = Collections.unmodifiableList(players);
    if (players.size() <= 0) {
      throw new IllegalArgumentException("You must include at least one player in the lineup.");
    }
    this.size = players.size();
  }

  @Override
  public String toString() {
    final int SPACING = 3;

    // Max name length
    final int MAX_NAME_LENGTH_ALLOWED = 18;
    int maxNameLength = 0;
    for (DataPlayer p : players) {
      maxNameLength = p.getName().length() > maxNameLength ? p.getName().length() : maxNameLength;
    }
    maxNameLength = Math.min(maxNameLength, MAX_NAME_LENGTH_ALLOWED);

    StringBuilder builder = new StringBuilder();

    // Header
    builder.append(
        StringUtils.padRight("Name", maxNameLength + SPACING));
    // builder.append(StringUtils.padLeft("Id", 14 + SPACING));
    builder.append(StringUtils.padLeft("Avg", 5 + SPACING));
    builder.append(StringUtils.padLeft("Slg", 5 + SPACING));

    // Content
    for (DataPlayer p : players) {
      String truncatedName = StringUtils.trim(p.getName());
      builder.append("\n");
      builder.append(StringUtils.padRight(truncatedName, maxNameLength + SPACING));
      // builder.append(StringUtils.padLeft(p.getId(), 14 + SPACING));
      builder.append(StringUtils.padLeft(StringUtils.formatDecimal(p.getBattingAverage(), 3), 5 + SPACING));
      builder.append(StringUtils.padLeft(StringUtils.formatDecimal(p.getSluggingPercentage(), 3), 5 + SPACING));

    }
    return builder.toString();
  }

  @Override
  public List<DataPlayer> asList() {
    return this.players;
  }

  @Override
  public DataPlayer getBatter(int index) {
    int adjustedIndex = index % players.size();
    return players.get(adjustedIndex);
  }

  public static String getType() {
    return StandardBattingLineup.class.getSimpleName();
  }

  @Override
  public int hashCode() {
    return Objects.hash(players);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof StandardBattingLineup) {
      if (((StandardBattingLineup) other).players.equals(this.players)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void populateStats(DataPlayerLookup statsPlayers) {
    for (int i = 0; i < players.size(); i++) {
      DataPlayer statslessPlayer = players.get(i);
      DataPlayer statsfullPlayer = statsPlayers.getDataPlayer(statslessPlayer.getId());
      if (statsfullPlayer == null) {
        throw new RuntimeException("Failed to populate stats for player " + statslessPlayer
            + " as no stats for this player were found in batting data. Try running the optimization again with the -F flag.");
      }
      players.set(i, statsfullPlayer);
    }
  }

  @Override
  public int size() {
    return this.size;
  }

  public String getDisplayInfo() {
    return null; // No additional interesting information in the Standard batting lineup
  }


}
