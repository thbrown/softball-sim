package com.github.thbrown.softballsim.lineupgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiFunction;

import com.github.thbrown.softballsim.PermutationGeneratorUtil;
import com.github.thbrown.softballsim.Player;
import com.github.thbrown.softballsim.lineup.AlternatingBattingLineup;
import com.github.thbrown.softballsim.lineup.BattingLineup;

public class AlternatingBattingLineupGenerator implements LineupGenerator {

  private Queue<BattingLineup> allPossibleLineups = new LinkedList<>();

  private static final BiFunction<List<Map<String, String>>, String, Void> ADD_LINE_TO_GROUPS_FUNCTION = (
      groups, line) -> {
    String[] splitLine = line.split(",");
    validate(splitLine);

    String key = splitLine[0];
    String value = line.replace(splitLine[0] + "," + splitLine[1], "");
    String groupString = splitLine[1];
    if (groupString.equals("A")) {
      LineupGeneratorUtil.addEntryToGroup(groups.get(0), key, value);
    } else {
      LineupGeneratorUtil.addEntryToGroup(groups.get(1), key, value);
    }
    return null;
  };

  @Override
  public void readDataFromFile(String statsPath) {
    List<Player> groupA = new ArrayList<>();
    List<Player> groupB = new ArrayList<>();

    List<Map<String, String>> groups = LineupGeneratorUtil.readFilesFromPath(statsPath,
        2 /* numGroups */, ADD_LINE_TO_GROUPS_FUNCTION);
    LineupGeneratorUtil.createPlayersFromMap(groups.get(0), groupA);
    LineupGeneratorUtil.createPlayersFromMap(groups.get(1), groupB);

    // Find all batting lineup permutations
    List<List<Player>> groupALineups = PermutationGeneratorUtil.permute(groupA);
    List<List<Player>> groupBLineups = PermutationGeneratorUtil.permute(groupB);

    for (List<Player> groupAPermutation : groupALineups) {
      for (List<Player> groupBPermutation : groupBLineups) {
        // TODO: Need to account for both groupA bat first and groupB bat first.
        allPossibleLineups.add(new AlternatingBattingLineup(groupAPermutation, groupBPermutation));
        // allPossibleLineups.add(new
        // AlternatingBattingLineup(groupBPermutation, groupAPermutation));
      }
    }
  }

  @Override
  public BattingLineup getNextLineup() {
    return allPossibleLineups.poll();
  }

  private static void validate(String[] splitLine) {
    if (!splitLine[1].equals("A") && !splitLine[1].equals("B")) {
      throw new RuntimeException("Expected each player to be in either group A or B");
    }
    LineupGeneratorUtil.validateHitValues(Arrays.copyOfRange(splitLine, 2, splitLine.length));
  }
}
