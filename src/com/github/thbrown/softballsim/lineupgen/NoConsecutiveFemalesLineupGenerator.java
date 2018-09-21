package com.github.thbrown.softballsim.lineupgen;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.github.thbrown.softballsim.PermutationGeneratorUtil;
import com.github.thbrown.softballsim.Player;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.NoConsecutiveFemalesBattingLineup;

public class NoConsecutiveFemalesLineupGenerator implements LineupGenerator {
  
  private Queue<BattingLineup> allPossibleLineups = new LinkedList<>();
  
  List<Player> groupA = new ArrayList<>();
  List<Player> groupB = new ArrayList<>();

  @Override
  public void readDataFromFile(String statsPath) {
    List<Map<String, String>> groups = LineupGeneratorUtil.readFilesFromPath(statsPath,
        2 /* numGroups */, LineupGeneratorUtil.ADD_LINE_TO_TWO_GROUPS_FUNCTION);
    
    groups.get(1).remove("Laura");
    groups.get(1).remove("Jenn");
    groups.get(1).remove("BobbiJo");
    groups.get(1).remove("Morgan");

    
    groups.get(0).remove("Jeff");
    groups.get(0).remove("Darren");
    groups.get(0).remove("Dan");
    groups.get(0).remove("Randy");

    
    LineupGeneratorUtil.createPlayersFromMap(groups.get(0), groupA);
    LineupGeneratorUtil.createPlayersFromMap(groups.get(1), groupB);
    
    List<Player> players = new ArrayList<>();
    players.addAll(groupA);
    players.addAll(groupB);
    
    List<List<Player>> lineups = PermutationGeneratorUtil.permute(players);
    for (List<Player> lineup : lineups) {
      if (isValidLineup(lineup)) {
        allPossibleLineups.add(new NoConsecutiveFemalesBattingLineup(lineup));
      }
    }
  }
  
  private boolean bothPlayersAreGroupB(Player A, Player B) {
    return groupB.contains(A) && groupB.contains(B);
  }

  private boolean isValidLineup(List<Player> lineup) {
    if(lineup.size() < 2) {
      return false;
    }
    if(bothPlayersAreGroupB(lineup.get(0),lineup.get(lineup.size()-1))) {
      return false;
    } else {
      for(int i = 0; i < lineup.size() - 1; i++) {
        if(bothPlayersAreGroupB(lineup.get(i),lineup.get(i+1))) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public BattingLineup getNextLineup() {
    return allPossibleLineups.poll();
  }
}
