package com.github.thbrown.softballsim.lineupgen;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.github.thbrown.softballsim.PermutationGeneratorUtil;
import com.github.thbrown.softballsim.Player;
import com.github.thbrown.softballsim.lineup.AlternatingBattingLineup;
import com.github.thbrown.softballsim.lineup.BattingLineup;

public class AlternatingBattingLineupGenerator implements LineupGenerator {

  private Queue<BattingLineup> allPossibleLineups = new LinkedList<>();

  @Override
  public void readDataFromFile(String statsPath) {
    List<Player> groupA = new ArrayList<>();
    List<Player> groupB = new ArrayList<>();
    

    List<Map<String, String>> groups = LineupGeneratorUtil.readFilesFromPath(statsPath,
        2 /* numGroups */, LineupGeneratorUtil.ADD_LINE_TO_TWO_GROUPS_FUNCTION);

    LineupGeneratorUtil.createPlayersFromMap(groups.get(0), groupA);
    LineupGeneratorUtil.createPlayersFromMap(groups.get(1), groupB);

    // Find all batting lineup permutations
    List<List<Player>> groupALineups = PermutationGeneratorUtil.permute(groupA);
    List<List<Player>> groupBLineups = PermutationGeneratorUtil.permute(groupB);
    
    for (List<Player> groupAPermutation : groupALineups) {
      for (List<Player> groupBPermutation : groupBLineups) {
          allPossibleLineups.add(new AlternatingBattingLineup(groupAPermutation, groupBPermutation));
          allPossibleLineups.add(new AlternatingBattingLineup(groupBPermutation, groupAPermutation));
      }
    }
  }

  @Override
  public BattingLineup getNextLineup() {
    return allPossibleLineups.poll();
  }
}
