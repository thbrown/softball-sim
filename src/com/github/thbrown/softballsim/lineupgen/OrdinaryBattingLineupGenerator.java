package com.github.thbrown.softballsim.lineupgen;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiFunction;

import com.github.thbrown.softballsim.PermutationGeneratorUtil;
import com.github.thbrown.softballsim.Player;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.OrdinaryBattingLineup;

public class OrdinaryBattingLineupGenerator implements LineupGenerator {

  private Queue<BattingLineup> allPossibleLineups = new LinkedList<>();

  private static final BiFunction<List<Map<String, String>>, String, Void> ADD_LINE_TO_GROUPS_FUNCTION = (
      groups, line) -> {
    String[] splitLine = line.split(",");

    validate(splitLine);

    String key = splitLine[0];
    String value = line.replace(splitLine[0], "");
    LineupGeneratorUtil.addEntryToGroup(groups.get(0), key, value);

    return null;
  };

  @Override
  public BattingLineup getNextLineup() {
    return allPossibleLineups.poll();
  }

  @Override
  public void readDataFromFile(String statsPath) {
    List<Player> players = new LinkedList<>();

    List<Map<String, String>> groups = LineupGeneratorUtil.readFilesFromPath(statsPath,
        1 /* numGroups */,
        ADD_LINE_TO_GROUPS_FUNCTION);
    LineupGeneratorUtil.createPlayersFromMap(groups.get(0), players);

    // Find all batting lineup permutations
    List<List<Player>> lineups = PermutationGeneratorUtil.permute(players);
    for (List<Player> lineup : lineups) {
      allPossibleLineups.add(new OrdinaryBattingLineup(lineup));
    }
  }

  private static void validate(String[] splitLine) {
    LineupGeneratorUtil.validateHitValues(Arrays.copyOfRange(splitLine, 1, splitLine.length));
  }

  @Override
  public BattingLineup getIntitialLineup() {
    BattingLineup someLineup = allPossibleLineups.peek();
    // TODO: Sort by batting avarage, that's an okay first guess
    return someLineup;
  }
}
