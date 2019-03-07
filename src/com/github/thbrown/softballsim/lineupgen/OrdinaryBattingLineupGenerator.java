package com.github.thbrown.softballsim.lineupgen;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import com.github.thbrown.softballsim.CombinatoricsUtil;
import com.github.thbrown.softballsim.Player;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.OrdinaryBattingLineup;

public class OrdinaryBattingLineupGenerator implements LineupGenerator {
  private List<Player> players;
  private long size;

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
  public BattingLineup getLineup(long index) {
	if(index >= this.size) {
	  return null;
	}
	int[] order = CombinatoricsUtil.getIthPermutation(players.size(), index);
	List<Player> lineup = CombinatoricsUtil.mapListToArray(players, order);
	return new OrdinaryBattingLineup(lineup);
  }

  @Override
  public void readDataFromFile(String statsPath) {
    players = new LinkedList<>();

    List<Map<String, String>> groups = LineupGeneratorUtil.readFilesFromPath(statsPath,
        1 /* numGroups */,
        ADD_LINE_TO_GROUPS_FUNCTION);
    LineupGeneratorUtil.createPlayersFromMap(groups.get(0), players);
    
    this.size = CombinatoricsUtil.factorial(players.size());
  }
  
  @Override
  public void readDataFromString(String data) {
    players = new LinkedList<>();

    List<Map<String, String>> groups = LineupGeneratorUtil.readDataFromString(data,
        1 /* numGroups */,
        ADD_LINE_TO_GROUPS_FUNCTION);
    LineupGeneratorUtil.createPlayersFromMap(groups.get(0), players);
    
    this.size = CombinatoricsUtil.factorial(players.size());
  }

  private static void validate(String[] splitLine) {
    LineupGeneratorUtil.validateHitValues(Arrays.copyOfRange(splitLine, 1, splitLine.length));
  }
  
  @Override
  public long size() {
  	return this.size;
  }

  @Override
  public BattingLineup getIntitialLineup() {  
    players.sort( (a,b) -> {
    	double diff = b.getAverageNumeric() - a.getAverageNumeric();
    	if(diff > 0) {
    		return 1;
    	} else if (diff < 0){
    		return -1;
    	} else {
    		return 0;
    	}
    } );
    
    return new OrdinaryBattingLineup(players);
  }

}
