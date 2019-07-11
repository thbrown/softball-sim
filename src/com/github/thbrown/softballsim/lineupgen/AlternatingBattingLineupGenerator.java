package com.github.thbrown.softballsim.lineupgen;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.github.thbrown.softballsim.CombinatoricsUtil;
import com.github.thbrown.softballsim.Player;
import com.github.thbrown.softballsim.lineup.AlternatingBattingLineup;
import com.github.thbrown.softballsim.lineup.BattingLineup;

public class AlternatingBattingLineupGenerator implements LineupGenerator {

  private List<Player> groupA = new ArrayList<>();
  private List<Player> groupB = new ArrayList<>();
  
  private long groupBSize;
  private long groupASize;
  private long size;

  @Override
  public void readDataFromFile(String statsPath) {
    List<Map<String, String>> groups = LineupGeneratorUtil.readFilesFromPath(statsPath,
        2 /* numGroups */, LineupGeneratorUtil.ADD_LINE_TO_TWO_GROUPS_FUNCTION);
    initCommon(groups);
  }
  
  @Override
  public void readDataFromString(String data) {
    List<Map<String, String>> groups = LineupGeneratorUtil.readDataFromString(data,
        2 /* numGroups */, LineupGeneratorUtil.ADD_LINE_TO_TWO_GROUPS_FUNCTION);
    initCommon(groups);
  }
  
  private void initCommon(List<Map<String, String>> groups) {
    LineupGeneratorUtil.createPlayersFromMap(groups.get(0), groupA);
    LineupGeneratorUtil.createPlayersFromMap(groups.get(1), groupB);
    groupASize = CombinatoricsUtil.factorial(groupA.size());
    groupBSize = CombinatoricsUtil.factorial(groupB.size());
    this.size = groupASize * groupBSize * 2;
  }

  @Override
  public BattingLineup getLineup(long index) {
    if(index >= this.size) {
      return null;
    }
	
    long groupAIndex = index % groupASize;
    int[] groupAOrder = CombinatoricsUtil.getIthPermutation(groupA.size(), groupAIndex);
    List<Player> groupAOrderList = CombinatoricsUtil.mapListToArray(groupA, groupAOrder);
    		  
    long groupBIndex = (int) Math.floor(index / groupASize);
    int[] groupBOrder = CombinatoricsUtil.getIthPermutation(groupB.size(), groupBIndex);
    List<Player> groupBOrderList = CombinatoricsUtil.mapListToArray(groupB, groupBOrder);
    
    if(index < this.size/2) {
      // Group A bats first
      return new AlternatingBattingLineup(groupAOrderList, groupBOrderList);
    } else {
      // Group B bats first
      return new AlternatingBattingLineup(groupBOrderList, groupAOrderList);
    }
  }
  
  @Override
  public BattingLineup getIntitialLineup() {
    BattingLineup someLineup = new AlternatingBattingLineup(groupA, groupB);
    // TODO: Sort by batting average, that's an okay first guess
    return someLineup;
  }

  @Override
  public long size() {
	  return this.size;
  }
}
