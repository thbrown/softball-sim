package com.github.thbrown.softballsim.lineupgen;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import com.github.thbrown.softballsim.CombinatoricsUtil;
import com.github.thbrown.softballsim.Player;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.OrdinaryBattingLineup;

public class NoConsecutiveFemalesLineupGenerator implements LineupGenerator {
  
  private Queue<BattingLineup> allPossibleLineups = new LinkedList<>();
  
  List<Player> groupA = new ArrayList<>();
  List<Player> groupB = new ArrayList<>();
  List<List<Player>> possibleLineups;
  
  private long size;

  @Override
  public void readDataFromFile(String statsPath) {
    List<Map<String, String>> groups = LineupGeneratorUtil.readFilesFromPath(statsPath,
        2 /* numGroups */, LineupGeneratorUtil.ADD_LINE_TO_TWO_GROUPS_FUNCTION);
        
    LineupGeneratorUtil.createPlayersFromMap(groups.get(0), groupA);
    LineupGeneratorUtil.createPlayersFromMap(groups.get(1), groupB);
    
    List<Player> players = new ArrayList<>();
    players.addAll(groupA);
    players.addAll(groupB);
    
    if(groupA.size() < groupB.size()) {
      throw new RuntimeException("The number of males must be greater than or equal to the number of females. Males: " + groupA.size() + " Females: " + groupB.size());
    }
    
    // TODO: develop indexable combinations so we don't have to save all these lineups in memory
    this.possibleLineups = CombinatoricsUtil.permute(players);
    
    // Filter invalid lineups
    this.possibleLineups = possibleLineups.stream().filter( lineup -> isValidLineup(lineup)).collect(Collectors.toList());;
    
    /*
     * For ever permutation of males we want to insert every permutation of females into the slots 
     * between the males (excluding the last slot because if both the first and the last slot were
     * selected there would be back to back female batters). Therefore, we must add back all the
     * lineups in which there is a female better                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     last.
     */
    
    /*
    int males = groupA.size();
    int females = groupB.size();
    
    long malePermutations = CombinatoricsUtil.factorial(males);
    long femalePermutations = CombinatoricsUtil.factorial(groupB.size());
    
    long slotCombinations = malePermutations/(femalePermutations*CombinatoricsUtil.factorial(males- females));
    
    long availableSlots = CombinatoricsUtil.factorial(males - 1);
    long availableFemales = CombinatoricsUtil.factorial(females - 1);
    
    long additionalSlotCombinations = availableSlots/(availableFemales * CombinatoricsUtil.factorial((males-1)*(females-1)));
    
    this.size = malePermutations * (femalePermutations*slotCombinations + femalePermutations*additionalSlotCombinations);
    */
    this.size = this.possibleLineups.size();
    
    //if(this.size != possibleLineups.size()) {
    //  throw new RuntimeException("Calculated size did not match empirical size. Calculated: " + this.size + " Empirical: " + possibleLineups.size());
    //}
  }
  
  @Override
  public void readDataFromString(String data) {
    List<Map<String, String>> groups = LineupGeneratorUtil.readDataFromString(data,
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
    if(index < size) {
      return new OrdinaryBattingLineup(this.possibleLineups.get(Math.toIntExact(index)));
    } else {
      return null;
    }
  }
	
  @Override
  public long size() {
	  return size;
  }
}
