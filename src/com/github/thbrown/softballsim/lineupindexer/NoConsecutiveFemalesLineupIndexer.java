package com.github.thbrown.softballsim.lineupindexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.util.Pair;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.StandardBattingLineup;
import com.github.thbrown.softballsim.util.CombinatoricsUtil;
import com.github.thbrown.softballsim.util.Logger;
import org.apache.commons.math3.util.Pair;


public class NoConsecutiveFemalesLineupIndexer implements BattingLineupIndexer<StandardBattingLineup> {

  private List<DataPlayer> men = new ArrayList<>();
  private List<DataPlayer> women = new ArrayList<>();

  private final long size;
  private final long slotCombinations;
  private final long cutoff;
  private final long malePermutationCount;
  private final long femalePermutationCount;

  private final int maleCount;
  private final int femaleCount;
  private final int totalCount;

  public NoConsecutiveFemalesLineupIndexer(DataStats stats, List<String> players) {
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
     * For every permutation of males we want to insert every permutation of females into the slots
     * between the males (including zero indexed slot but excluding the last slot -- if a female were
     * added to both the zero indexed slot and the last slot there would be back to back female
     * batters). Therefore, we must separately add all the lineups in which there is a female batter in
     * the last lineup slot.
     */

    this.malePermutationCount = CombinatoricsUtil.factorial(maleCount);
    this.femalePermutationCount = CombinatoricsUtil.factorial(femaleCount);

    this.slotCombinations =
        CombinatoricsUtil.binomial(maleCount, femaleCount) + CombinatoricsUtil.binomial(maleCount - 1, femaleCount - 1);

    // Cutoff indicates the point at which the slotCombination goes from female last to no female last
    this.cutoff = CombinatoricsUtil.binomial(maleCount - 1, femaleCount - 1);

    this.size = malePermutationCount * femalePermutationCount * this.slotCombinations;
  }

  // Use this code for a unit test
  /*
   * private boolean bothPlayersAreGroupB(Player A, Player B) { return groupB.contains(A) &&
   * groupB.contains(B); }
   * 
   * private boolean isValidLineup(List<Player> lineup) { if (lineup.size() < 2) { return false; } if
   * (bothPlayersAreGroupB(lineup.get(0), lineup.get(lineup.size() - 1))) { return false; } else { for
   * (int i = 0; i < lineup.size() - 1; i++) { if (bothPlayersAreGroupB(lineup.get(i), lineup.get(i +
   * 1))) { return false; } } } return true; }
   */

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
    long femaleCombinationIndex = (cumulativeSize2 == 0 ? 0 : (long) Math.floor(index / cumulativeSize2) /*
                                                                                                          * no need for
                                                                                                          * mod here,
                                                                                                          * since this
                                                                                                          * is the last
                                                                                                          * group (would
                                                                                                          * have been
                                                                                                          * this.
                                                                                                          * slotCombinations)
                                                                                                          */);

    int[] maleOrder = CombinatoricsUtil.getIthPermutation(males, malePermutationIndex);
    List<DataPlayer> maleLineup = CombinatoricsUtil.mapListToArray(men, maleOrder);

    int[] femaleOrder = CombinatoricsUtil.getIthPermutation(females, femalePermutationIndex);
    List<DataPlayer> femaleLineup = CombinatoricsUtil.mapListToArray(women, femaleOrder);

    int[] femalePositions = null;
    if (femaleCombinationIndex < this.cutoff) {
      // Female positions before the cutoff indicate which spots between the males the females should
      // occupy, excluding the first and last lineup slots (because we'll force a female batter last).
      // Here we'll find places for all females, except the last one (who will be added to the end).
      femalePositions = CombinatoricsUtil.getIthCombination(females - 1, femaleCombinationIndex);

      // Add one to each, since this is before the cutoff we are excluding the first slot
      femalePositions = Arrays.stream(femalePositions).map(i -> i + 1).toArray();

      // There is always a female batting last, add her to the end
      femalePositions = Arrays.copyOf(femalePositions, femalePositions.length + 1);
      femalePositions[femalePositions.length - 1] = males;
    } else {
      femalePositions = CombinatoricsUtil.getIthCombination(females, femaleCombinationIndex - cutoff);
    }

    // Convert to lineup indexes - add the number of females inserted before them to their value.
    for (int i = 0; i < femalePositions.length; i++) {
      femalePositions[i] = femalePositions[i] + i;
    }

    // These three parameters are all we need to define a noConsecutiveFemale lineup, merge them
    List<DataPlayer> mergedLineup = buildLineup(maleLineup, femaleLineup, femalePositions);

    return new StandardBattingLineup(mergedLineup);
  }

  @Override
  public Pair<Long, StandardBattingLineup> getRandomNeighbor(long index) {

    // Get the current order
    StandardBattingLineup lineup = getLineup(index);

    // Split the lineup into the males and females
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
    // Logger.log("LINEUP " + index + " " + listLineup);

    // Get the permutations of both the males and the females
    int[] orderA = CombinatoricsUtil.getOrdering(this.men, men);
    int[] orderB = CombinatoricsUtil.getOrdering(this.women, women);

    // Get the combination that represents the slots females occupy in the lineup
    int[] currentWomenSlots = getWomenSlots(listLineup);
    int[] newWomenSlots = null;

    long totalNumberOfPossiblePermSwaps =
        CombinatoricsUtil.binomial(men.size(), 2) + CombinatoricsUtil.binomial(women.size(), 2);

    // The number of combination neighbors is different depending on whether or not a female is batting
    // in the first or last spot or not
    boolean femaleFirst = false;
    boolean femaleLast = false;
    if (listLineup.get(0).getGender().equals("F")) {
      femaleFirst = true;
    } else if (listLineup.get(lineup.size() - 1).getGender().equals("F")) {
      femaleLast = true;
    }

    long totalNumberOfPossibleCombSwaps;
    if (femaleFirst || femaleLast) {
      totalNumberOfPossibleCombSwaps =
          (women.size() - 1) * (men.size() - women.size()) + (1) * (men.size() + 1 - women.size());
    } else {
      totalNumberOfPossibleCombSwaps = (women.size()) * (men.size() + 1 - women.size());
    }

    // Logger.log("PERM " + totalNumberOfPossiblePermSwaps + " COMBOS " +
    // totalNumberOfPossibleCombSwaps);
    if (ThreadLocalRandom.current()
        .nextLong(totalNumberOfPossiblePermSwaps + totalNumberOfPossibleCombSwaps) < totalNumberOfPossiblePermSwaps) {
      // Swap two random elements in either the male list or female list (longer lists are more likely to
      // be swapped)
      if (ThreadLocalRandom.current().nextLong(totalNumberOfPossiblePermSwaps) < CombinatoricsUtil.binomial(men.size(),
          2)) {
        int randomOne = ThreadLocalRandom.current().nextInt(men.size());
        int randomTwo = 0;
        do {
          randomTwo = ThreadLocalRandom.current().nextInt(men.size());
        } while (randomOne == randomTwo);
        CombinatoricsUtil.swap(randomOne, randomTwo, orderA);
      } else {
        int randomOne = ThreadLocalRandom.current().nextInt(women.size());
        int randomTwo = 0;
        do {
          randomTwo = ThreadLocalRandom.current().nextInt(women.size());
        } while (randomOne == randomTwo);
        CombinatoricsUtil.swap(randomOne, randomTwo, orderB);
      }

      // Slots unchanged
      newWomenSlots = currentWomenSlots;
    } else {

      int randomSlotToRemove;
      int[] invertedSlots;
      if (femaleFirst || femaleLast) {
        // Select a random female slot to remove
        // We need a higher chance of selecting the first or last slot to remove,
        // because there are more alterantive slots that can take it's place.
        int swapsForFirstOrLast = (women.size() - 1) * (men.size() - women.size());
        int swapsForMiddle = (1) * (men.size() + 1 - women.size());

        if (ThreadLocalRandom.current().nextLong(swapsForFirstOrLast + swapsForMiddle) < swapsForFirstOrLast) {
          // Remove the first or last slot
          if (femaleFirst) {
            randomSlotToRemove = 0;
          } else if (femaleLast) {
            randomSlotToRemove = this.men.size();
          } else {
            // Should be impossible.
            throw new RuntimeException("Bad data for given lineup indexer");
          }
          // Choose replacement options - we can choose any other available slot
          invertedSlots = this.invertArray(currentWomenSlots, 0, this.men.size());
        } else {
          // Remove a middle slot
          if (femaleFirst) {
            // Choose a random middle slot (this assumes currentWomenSlots is sorted asc)
            int randomIndex = ThreadLocalRandom.current().nextInt(this.women.size() - 1) + 1;
            randomSlotToRemove = currentWomenSlots[randomIndex];
            // Choose replacement options - can't put a female in the last slot
            invertedSlots = this.invertArray(currentWomenSlots, 0, this.men.size() - 1);
          } else if (femaleLast) {
            // Choose a random middle slot (this assumes currentWomenSlots is sorted asc)
            int randomIndex = ThreadLocalRandom.current().nextInt(this.women.size() - 1);
            randomSlotToRemove = currentWomenSlots[randomIndex];
            // Choose replacement options - can't put a female in the first slot
            invertedSlots = this.invertArray(currentWomenSlots, 1, this.men.size());
          } else {
            // Should be impossible.
            throw new RuntimeException("Bad data for given lineup indexer");
          }
        }
      } else {
        // Select a random female slot to remove
        int randomIndex = ThreadLocalRandom.current().nextInt(currentWomenSlots.length);
        randomSlotToRemove = currentWomenSlots[randomIndex];
        // Choose replacement options - we can choose any other available slot
        invertedSlots = this.invertArray(currentWomenSlots, 0, this.men.size());
      }

      // Randomly choose one of the available options
      int randomNewIndex = ThreadLocalRandom.current().nextInt(invertedSlots.length);
      int randomSlotToAdd = invertedSlots[randomNewIndex];

      // Add everything to the new array except for the slot we are removing
      newWomenSlots = new int[currentWomenSlots.length];
      int counter = 0;
      for (int i = 0; i < newWomenSlots.length; i++) {
        if (currentWomenSlots[i] != randomSlotToRemove) {
          newWomenSlots[counter] = currentWomenSlots[i];
          counter++;
        }
      }

      // Also add the newly selected slot
      newWomenSlots[counter] = randomSlotToAdd;

      // Sort it
      Arrays.sort(newWomenSlots);
    }

    // Convert to slots to spots - add the number of females inserted before them to their value.
    int[] newWomenSpots = new int[newWomenSlots.length];
    for (int i = 0; i < newWomenSpots.length; i++) {
      newWomenSpots[i] = newWomenSlots[i] + i;
    }

    // These three parameters are all we need to define a noConsecutiveFemale lineup, merge them
    List<DataPlayer> newMenList = CombinatoricsUtil.mapListToArray(this.men, orderA);
    List<DataPlayer> newWomenList = CombinatoricsUtil.mapListToArray(this.women, orderB);
    List<DataPlayer> merged = buildLineup(newMenList, newWomenList, newWomenSpots);
    StandardBattingLineup newLineup = new StandardBattingLineup(merged);
    long newIndex = getIndex(newLineup);
    return Pair.create(newIndex, newLineup);
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

    // Calculate which slots (spots between the men) the females occupy
    int[] womenSlots = getWomenSlots(listLineup);

    // Is the last batter female or male? This tells us if we are before or after the cutoff
    if (listLineup.get(listLineup.size() - 1).getGender().equals("F")) {
      // Before the cutoff - a women is always batting last
      int[] womenSlotsTrimmed = Arrays.copyOf(womenSlots, womenSlots.length - 1); // A women is always batting in the
                                                                                  // last slot here, so we only care
                                                                                  // about the other slots
      // Subtract one from every element - Since we can't have a women batting first here, the first
      // available lineup position is 2nd spot (or index 1). The subtraction reflects this.
      womenSlotsTrimmed = Arrays.stream(womenSlotsTrimmed).map(i -> i - 1).toArray();
      long womenCombinationIndex = CombinatoricsUtil.getCombinationIndex(womenSlotsTrimmed);
      return womenCombinationIndex * this.femalePermutationCount * this.malePermutationCount
          + womenPermutationIndex * this.malePermutationCount + menPermiutationIndex;
    } else {
      // After the cutoff - a women is never batting last
      long womenCombinationIndex = CombinatoricsUtil.getCombinationIndex(womenSlots);
      return (cutoff + womenCombinationIndex) * this.femalePermutationCount * this.malePermutationCount
          + womenPermutationIndex * this.malePermutationCount + menPermiutationIndex;
    }

  }

  private int[] getWomenSlots(List<DataPlayer> listLineup) {
    // Calculate which spots in the lineup are filled with females
    List<Integer> womenSpotList = new ArrayList<>();
    for (int i = 0; i < listLineup.size(); i++) {
      if (listLineup.get(i).getGender().equals("F")) {
        womenSpotList.add(i);
      }
    }

    int[] womenSpots = womenSpotList.stream().mapToInt(i -> i).toArray();
    int[] womenSlots = new int[womenSpots.length];

    // Map the female spots in the lineup to slots
    int womenSlotsIndex = 0;
    int slotCounter = 0;
    Boolean previousPlayerMale = true;
    for (DataPlayer player : listLineup) {
      if (player.getGender().equals("M")) {
        if (previousPlayerMale != null && previousPlayerMale == true) {
          slotCounter++;
        }
        previousPlayerMale = true;
      } else if (player.getGender().equals("F")) {
        womenSlots[womenSlotsIndex] = slotCounter;
        womenSlotsIndex++;
        slotCounter++;
        previousPlayerMale = false;
      }
    }

    return womenSlots;
  }

  private List<DataPlayer> buildLineup(List<DataPlayer> maleLineup, List<DataPlayer> femaleLineup, int[] womenSpots) {
    List<DataPlayer> mergedLineup = new ArrayList<>(totalCount);
    int femaleSpotsIndex = 0;
    int femaleOrderIndex = 0;
    int maleOrderIndex = 0;
    for (int i = 0; i < this.totalCount; i++) {

      // If a female is at the Ith position, add her. Otherwise add a dude.
      if (femaleSpotsIndex < womenSpots.length && i == womenSpots[femaleSpotsIndex]) {
        DataPlayer toAdd = femaleLineup.get(femaleOrderIndex);
        mergedLineup.add(toAdd);
        femaleSpotsIndex++;
        femaleOrderIndex++;
      } else {
        DataPlayer toAdd = maleLineup.get(maleOrderIndex);
        mergedLineup.add(toAdd);
        maleOrderIndex++;
      }
    }
    return mergedLineup;
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
