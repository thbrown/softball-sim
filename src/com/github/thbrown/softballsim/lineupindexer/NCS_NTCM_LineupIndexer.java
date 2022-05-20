package com.github.thbrown.softballsim.lineupindexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.util.Pair;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.StandardBattingLineup;
import com.github.thbrown.softballsim.util.CombinatoricsUtil;
import com.github.thbrown.softballsim.util.Logger;

public class NCS_NTCM_LineupIndexer implements BattingLineupIndexer<StandardBattingLineup> {

  private List<DataPlayer> men = new ArrayList<>();
  private List<DataPlayer> women = new ArrayList<>();

  private final long size;
  private final long doubleMaleSlots;
  private final long cutoff;
  private final long malePermutationCount;
  private final long femalePermutationCount;

  private final int maleCount;
  private final int femaleCount;
  private final int totalCount;

  public NCS_NTCM_LineupIndexer(DataStats stats, List<String> players) {
    // Get the DataPlayers by id, this isn't as efficient as it could be
    for (DataPlayer player : stats.getPlayers()) {
      if (players.contains(player.getId())) {
        if (player.getGender().equals("M")) {
          men.add(player);
        } else if (player.getGender().equals("F")) {
          women.add(player);
        } else {
          throw new IllegalArgumentException("Unrecognized gender " + player.getGender());
        }
      }
    }

    maleCount = men.size();
    femaleCount = women.size();
    totalCount = maleCount + femaleCount;

    if (femaleCount < 2) {
      throw new RuntimeException("You must have at least two females in the batting lineup. Males: "
          + men.size() + " Females: " + women.size());
    }

    if (maleCount < 2) {
      throw new RuntimeException("You must have at least two males in the batting lineup. Males: "
          + men.size() + " Females: " + women.size());
    }

    if (maleCount < femaleCount) {
      throw new RuntimeException("The number of males must be greater than or equal to the number of females. Males: "
          + men.size() + " Females: " + women.size());
    }

    /*
     * For every permutation of females we want to insert every permutation of males into the slots
     * between the males (including zero indexed slot but excluding the last slot -- if a female were
     * added to both the zero indexed slot and the last slot there would be back to back female
     * batters). Therefore, we must separately add all the lineups in which there is a male batter in
     * the last lineup slot.
     * 
     * These slots must contain at least one male (so there are not two consecutive females) and at most
     * two males (so there are never more than three consecutive males). The number of slots that must
     * contain two makes is equal to #Females-#Males. So we'll do binomial(#Females,#Females-#Males) to
     * determin which slots contain two males.
     */

    this.malePermutationCount = CombinatoricsUtil.factorial(maleCount);
    this.femalePermutationCount = CombinatoricsUtil.factorial(femaleCount);

    this.doubleMaleSlots =
        CombinatoricsUtil.binomial(femaleCount, maleCount - femaleCount);

    // Cutoff indicates the point at which the slotCombination goes from male first to female first
    this.cutoff = this.doubleMaleSlots;

    this.size = malePermutationCount * femalePermutationCount * this.doubleMaleSlots * 2;
  }

  @Override
  public long size() {
    return size;
  }

  @Override
  public StandardBattingLineup getLineup(long index) {
    if (index >= size) {
      return null;
    }

    int males = men.size();
    int females = women.size();

    // Derive the indexes for each permutation/combination from the main index
    long malePermutationIndex = index % CombinatoricsUtil.factorial(males);
    long cumulativeSize1 = CombinatoricsUtil.factorial(males);
    long femalePermutationIndex =
        (cumulativeSize1 == 0 ? 0 : (long) Math.floor(index / cumulativeSize1) % CombinatoricsUtil.factorial(females));
    long cumulativeSize2 = CombinatoricsUtil.factorial(males) * CombinatoricsUtil.factorial(females);
    long maleCombinationIndex = (cumulativeSize2 == 0 ? 0 : (long) Math.floor(index / cumulativeSize2));

    int[] maleOrder = CombinatoricsUtil.getIthPermutation(males, malePermutationIndex);
    List<DataPlayer> maleLineup = CombinatoricsUtil.mapListToArray(men, maleOrder);

    int[] femaleOrder = CombinatoricsUtil.getIthPermutation(females, femalePermutationIndex);
    List<DataPlayer> femaleLineup = CombinatoricsUtil.mapListToArray(women, femaleOrder);

    List<DataPlayer> mergedLineup = new ArrayList<>();


    /*
     * Set<String> dupDetector = new HashSet<>(); int length = 8; int numberToChoose = 4; for (int i =
     * 0; i < CombinatoricsUtil.binomial(length, numberToChoose); i++) { String result =
     * Arrays.toString(CombinatoricsUtil.getIthCombination(numberToChoose, i));
     * System.out.println(result); boolean isSuccess = dupDetector.add(result); if (!isSuccess) { throw
     * new RuntimeException("Duplicate value detected" + result); } }
     */

    if (maleCombinationIndex < this.cutoff) {
      // Male first
      int maleIndex = 0;
      int femaleIndex = 0;
      int doubleMaleIndex = 0;
      int[] doubleMaleSlots = CombinatoricsUtil.getIthCombination(males - females, maleCombinationIndex);
      // Logger.log(index + " " + Arrays.toString(maleOrder) + " " + Arrays.toString(femaleOrder) + " " +
      // Arrays.toString(doubleMaleSlots) + " " + 0);

      for (int betweenFemaleSlot = 0; betweenFemaleSlot < femaleLineup.size(); betweenFemaleSlot++) {
        if (doubleMaleIndex < doubleMaleSlots.length && betweenFemaleSlot == doubleMaleSlots[doubleMaleIndex]) {
          doubleMaleIndex++;
          // Logger.log(maleIndex + " " + maleLineup);
          mergedLineup.add(maleLineup.get(maleIndex));
          maleIndex++;
          mergedLineup.add(maleLineup.get(maleIndex));
          maleIndex++;
        } else {
          mergedLineup.add(maleLineup.get(maleIndex));
          maleIndex++;
        }
        mergedLineup.add(femaleLineup.get(femaleIndex));
        femaleIndex++;
      }
    } else {
      // Female first
      int maleIndex = 0;
      int femaleIndex = 0;
      int doubleMaleIndex = 0;
      int[] doubleMaleSlots = CombinatoricsUtil.getIthCombination(males - females, maleCombinationIndex - this.cutoff);
      // Logger.log(index + " " + Arrays.toString(maleOrder) + " " + Arrays.toString(femaleOrder) + " " +
      // Arrays.toString(doubleMaleSlots) + " " + 1);

      for (int betweenFemaleSlot = 0; betweenFemaleSlot < femaleLineup.size(); betweenFemaleSlot++) {
        mergedLineup.add(femaleLineup.get(femaleIndex));
        femaleIndex++;
        if (doubleMaleIndex < doubleMaleSlots.length && betweenFemaleSlot == doubleMaleSlots[doubleMaleIndex]) {
          doubleMaleIndex++;
          mergedLineup.add(maleLineup.get(maleIndex));
          maleIndex++;
          mergedLineup.add(maleLineup.get(maleIndex));
          maleIndex++;
        } else {
          mergedLineup.add(maleLineup.get(maleIndex));
          maleIndex++;
        }
      }
    }

    return new StandardBattingLineup(mergedLineup);
  }

  @Override
  public Pair<Long, StandardBattingLineup> getRandomNeighbor(long index) {
    final int SPREAD = 5;

    // Don't return the same index that was passed in, that's not a neighbor
    long offset = 0;
    do {
      offset = (long) (SPREAD * 2 * Math.random());
    } while (offset == 0);

    long newIndex = (index + offset) % this.size;
    newIndex = newIndex < 0 ? this.size - newIndex : newIndex; // We want negatives to overflow here
    return Pair.create(newIndex, this.getLineup(newIndex));
  }

  @Override
  public long getIndex(StandardBattingLineup lineup) {
    // Split the lineup in the males and females
    List<DataPlayer> men = new ArrayList<>();
    List<DataPlayer> women = new ArrayList<>();
    List<DataPlayer> listLineup = lineup.asList();
    for (DataPlayer player : listLineup) {
      if (player.getGender().equals("M")) {
        men.add(player);
      } else if (player.getGender().equals("F")) {
        women.add(player);
      }
    }

    // Get the indexes of the order the males and females seperately
    int[] menOrder = CombinatoricsUtil.getOrdering(this.men, men);
    long menPermiutationIndex = CombinatoricsUtil.getPermutationIndex(menOrder);

    int[] womenOrder = CombinatoricsUtil.getOrdering(this.women, women);
    long womenPermutationIndex = CombinatoricsUtil.getPermutationIndex(womenOrder);

    // Calculate which slots (spots between the females) the males occupy
    int[] doubleMaleSlots = getMaleDoubleSlots(listLineup);

    // Is the last batter female or male? This tells us if we are before or after the cutoff
    if (listLineup.get(0).getGender().equals("M")) {
      // Before the cutoff - a male is batting first
      long doubleMaleCombinationIndex = CombinatoricsUtil.getCombinationIndex(doubleMaleSlots);

      return menPermiutationIndex + womenPermutationIndex * this.malePermutationCount
          + doubleMaleCombinationIndex * this.femalePermutationCount * this.malePermutationCount;
    } else {
      // After the cutoff - a male is not batting first
      doubleMaleSlots = Arrays.stream(doubleMaleSlots).map(i -> i - 1).toArray();
      long doubleMaleCombinationIndex = CombinatoricsUtil.getCombinationIndex(doubleMaleSlots);
      // Logger.log(menPermiutationIndex + " " + womenPermutationIndex + " " + doubleMaleCombinationIndex
      // + " " + Arrays.toString(doubleMaleSlots));

      return this.size / 2 + menPermiutationIndex + womenPermutationIndex * this.malePermutationCount
          + doubleMaleCombinationIndex * this.femalePermutationCount * this.malePermutationCount;
    }

  }

  private int[] getMaleDoubleSlots(List<DataPlayer> listLineup) {
    List<Integer> doubleMaleSlots = new ArrayList<>();
    int femaleIndex = 0;
    int maleCount = 0;
    for (DataPlayer player : listLineup) {
      if (player.getGender().equals("M")) {
        maleCount++;
        if (maleCount == 2) {
          doubleMaleSlots.add(femaleIndex);
          maleCount = 0;
        }
      } else {
        femaleIndex++;
        maleCount = 0;
      }
    }
    int[] toReturn = new int[doubleMaleSlots.size()];
    for (int i = 0; i < doubleMaleSlots.size(); i++)
      toReturn[i] = doubleMaleSlots.get(i);
    return toReturn;
  }

  /**
   * Input contains a list of unique numbers between minValue (inclusive) and maxValue (inclusive),
   * but some values are missing. This method returns an array of of the missing values. Method
   * ignores any values of the input array that are outside the range.
   */
  private int[] invertArray(int[] input, int minValue, int maxValue) {
    // This could be more efficient
    int arraySize = maxValue - minValue + 1 - input.length;
    int[] result = new int[arraySize];
    int counter = 0;
    for (int i = minValue; i <= maxValue; i++) {
      final int finalI = i;
      if (!Arrays.stream(input).anyMatch(j -> j == finalI)) {
        result[counter] = i;
        counter++;
      }
    }
    return result;
  }


}
