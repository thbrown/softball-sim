package com.github.thbrown.softballsim.lineupgen;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.github.thbrown.softballsim.Player;
import com.github.thbrown.softballsim.lineup.BattingLineup;

public class NoConsecutiveFemalesLineupGenerator implements LineupGenerator {
  
  private Queue<BattingLineup> allPossibleLineups = new LinkedList<>();
  
  List<Player> groupA = new ArrayList<>();
  List<Player> groupB = new ArrayList<>();

  @Override
  public void readDataFromFile(String statsPath) {
    List<Map<String, String>> groups = LineupGeneratorUtil.readFilesFromPath(statsPath,
        2 /* numGroups */, LineupGeneratorUtil.ADD_LINE_TO_TWO_GROUPS_FUNCTION);
        
    LineupGeneratorUtil.createPlayersFromMap(groups.get(0), groupA);
    LineupGeneratorUtil.createPlayersFromMap(groups.get(1), groupB);
    
    List<Player> players = new ArrayList<>();
    players.addAll(groupA);
    players.addAll(groupB);
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
  public BattingLineup getIntitialLineup() {
    // TODO: Sort by batting average, that's an okay first guess
    return getLineup(0);
  }

  @Override
  public BattingLineup getLineup(long index) {
	// TODO Auto-generated method stub
	return null;
  }
	
  @Override
  public long size() {
	  // TODO Auto-generated method stub
	  return 0;
  }
}
