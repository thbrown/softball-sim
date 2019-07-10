package com.github.thbrown.softballsim.lineupgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.github.thbrown.softballsim.CombinatoricsUtil;
import com.github.thbrown.softballsim.Player;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.OrdinaryBattingLineup;

public class NoConsecutiveFemalesLineupGenerator implements LineupGenerator {
    
  List<Player> groupA = new ArrayList<>();
  List<Player> groupB = new ArrayList<>();
  
  private long size;
  private long slotCombinations;
  private long cuttoff;

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
   
   List<Player> players = new ArrayList<>();
   players.addAll(groupA);
   players.addAll(groupB);
   
   if(groupA.size() < groupB.size()) {
     throw new RuntimeException("The number of males must be greater than or equal to the number of females. Males: " + groupA.size() + " Females: " + groupB.size());
   }
   
   /*
    * For ever permutation of males we want to insert every permutation of females into the slots 
    * between the males (excluding the last slot because if both the first and the last slot were
    * selected there would be back to back female batters). Therefore, we must add back all the
    * lineups in which there is a female batter in the last lineup slot.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     last.
    */
   int males = groupA.size();
   int females = groupB.size();
   
   long malePermutations = CombinatoricsUtil.factorial(males);
   long femalePermutations = CombinatoricsUtil.factorial(females);
   
   this.slotCombinations = CombinatoricsUtil.binomial(males, females) + CombinatoricsUtil.binomial(males-1, females-1);
   this.cuttoff = CombinatoricsUtil.binomial(males, females);
   
   this.size = malePermutations * femalePermutations * this.slotCombinations;
   
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
    int males = groupA.size();
    int females = groupB.size();
        
    // Derive the indexes for each permutation/combination from the main index
    long malePermutationIndex = index % CombinatoricsUtil.factorial(males);
    long cumulativeSize1 = CombinatoricsUtil.factorial(males);
    long femalePermutationIndex = (cumulativeSize1 == 0 ? 0 : (long) Math.floor(index/cumulativeSize1) % CombinatoricsUtil.factorial(females));
    long cumulativeSize2 = CombinatoricsUtil.factorial(males)*CombinatoricsUtil.factorial(females);
    long femaleCombinationIndex = (cumulativeSize2 == 0 ? 0 : (long) Math.floor(index/cumulativeSize2) /* no need for mod here, since this is the last group (would have been this.slotCombinations)*/);
    
    int[] maleOrder = CombinatoricsUtil.getIthPermutation(males, malePermutationIndex);
    List<Player> maleLineup = CombinatoricsUtil.mapListToArray(groupA, maleOrder);
    
    int[] femaleOrder = CombinatoricsUtil.getIthPermutation(females, femalePermutationIndex);
    List<Player> femaleLineup = CombinatoricsUtil.mapListToArray(groupB, femaleOrder);
    
    int[] femalePositions = null;
    if(femaleCombinationIndex < cuttoff) {
      femalePositions = CombinatoricsUtil.getIthCombination(females, femaleCombinationIndex);
      // Female positions before the cutoff indicate spots between the males. To convert them to lineup indices add the number of females inserted before the to their value.
      for(int i = 0; i < femalePositions.length; i++) {
        femalePositions[i] = femalePositions[i] + i;
      }
    } else {
      femalePositions = CombinatoricsUtil.getIthCombination(females - 1, femaleCombinationIndex - cuttoff);
      // Female positions after the cutoff indicate spots between the males starting at position 2. To convert them to lineup indices add the number of females inserted before the to their value plus 1.
      for(int i = 0; i < femalePositions.length; i++) {
        femalePositions[i] = femalePositions[i] + i + 1;
      }
      
      // For this second set of iterations, there is always a female batting last (TODO: Maybe we should do these first?)
      int[] arrayWithLastFemaleAdded = new int[females];
      for (int i=0; i < femalePositions.length; i++){
        arrayWithLastFemaleAdded[i] = femalePositions[i];
      }
      arrayWithLastFemaleAdded[femalePositions.length] = males + females - 1;
      femalePositions = arrayWithLastFemaleAdded;
    }

    // These three parameters are all we need to define a noConsecutiveFemale lineup, merge them
    int mergedLineupSize = males + females;
    List<Player> mergedLineup = new ArrayList<>(mergedLineupSize);
    int femalePositionsIndex = 0;
    int femaleOrderIndex = 0;
    int maleOrderIndex = 0;
    for(int i = 0; i < mergedLineupSize; i++) {
      // If a female is at the Ith position, add her. Otherwise add a dude.
      if(femalePositionsIndex < femalePositions.length && i == femalePositions[femalePositionsIndex]) {
        Player toAdd = femaleLineup.get(femaleOrderIndex);
        mergedLineup.add(toAdd);
        femalePositionsIndex++;
        femaleOrderIndex++;
      } else {
        Player toAdd = maleLineup.get(maleOrderIndex);
        mergedLineup.add(toAdd);
        maleOrderIndex++;
      }
    }
    
    if(index < size) {
      return new OrdinaryBattingLineup(mergedLineup);
    } else {
      return null;
    }
  }
	
  @Override
  public long size() {
	  return size;
  }
}
