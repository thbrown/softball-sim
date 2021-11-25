package com.github.thbrown.softballsim.lineupindexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import org.apache.commons.math3.util.Pair;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.lineup.AlternatingBattingLineup;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.util.CombinatoricsUtil;
import com.github.thbrown.softballsim.util.Logger;
import java.util.concurrent.ThreadLocalRandom;

public class AlternatingGenderLineupIndexer implements BattingLineupIndexer<AlternatingBattingLineup> {

  private final List<DataPlayer> men = new ArrayList<>();
  private final List<DataPlayer> women = new ArrayList<>();

  private long menPermutations;
  private long womenPermutations;
  private long size;

  public AlternatingGenderLineupIndexer(DataStats stats, List<String> players) {
    // Get the DataPlayers by id, this isn't as efficient as it could be
    for (String playerId : players) {
      DataPlayer player = stats.getPlayerById(playerId);
      if (player.getGender().equals("M")) {
        men.add(player);
      } else if (player.getGender().equals("F")) {
        women.add(player);
      } else {
        throw new IllegalArgumentException("Unrecognized gender " + player.getGender());
      }
    }

    menPermutations = CombinatoricsUtil.factorial(men.size());
    womenPermutations = CombinatoricsUtil.factorial(women.size());
    this.size = menPermutations * womenPermutations * 2;
  }

  @Override
  public AlternatingBattingLineup getLineup(long index) {
    if (index >= this.size) {
      Logger.log("Size " + index + " " + this.size);
      return null;
    }

    long menIndex = index % menPermutations;
    int[] menOrder = CombinatoricsUtil.getIthPermutation(men.size(), menIndex);
    List<DataPlayer> groupAOrderList = CombinatoricsUtil.mapListToArray(men, menOrder);

    long womenIndex = (int) Math.floor(index / menPermutations);
    int[] womenOrder = CombinatoricsUtil.getIthPermutation(women.size(), womenIndex);
    List<DataPlayer> groupBOrderList = CombinatoricsUtil.mapListToArray(women, womenOrder);

    if (index < (this.size / 2)) {
      // Men bat first
      return new AlternatingBattingLineup(groupAOrderList, groupBOrderList);
    } else {
      // Women bat first
      return new AlternatingBattingLineup(groupBOrderList, groupAOrderList);
    }
  }

  @Override
  public long size() {
    return this.size;
  }

  @Override
  public Pair<Long, AlternatingBattingLineup> getRandomNeighbor(long index) {

    // Get the current order
    AlternatingBattingLineup lineup = getLineup(index);
    List<DataPlayer> groupA = lineup.getGroupA();
    List<DataPlayer> groupAInitialOrder = groupA.get(0).getGender().equals("M") ? men : women;
    int[] orderA = CombinatoricsUtil.getOrdering(groupAInitialOrder, groupA);

    List<DataPlayer> groupB = lineup.getGroupB();
    List<DataPlayer> groupBInitialOrder = groupB.get(0).getGender().equals("F") ? women : men;
    int[] orderB = CombinatoricsUtil.getOrdering(groupBInitialOrder, groupB);

    List<DataPlayer> newGroupAList = null;
    List<DataPlayer> newGroupBList = null;

    long totalNumberOfPossibleSwaps =
        CombinatoricsUtil.binomial(groupA.size(), 2) + CombinatoricsUtil.binomial(groupB.size(), 2);
    if (ThreadLocalRandom.current().nextLong(totalNumberOfPossibleSwaps + 1L) == 0) {
      // Swap groupA and groupB (same probabibility of any other swap)
      newGroupAList = groupB;
      newGroupBList = groupA;
    } else {
      // Swap two random elements in either the male list or female list (longer lists are more likely to
      // be swapped)
      if (ThreadLocalRandom.current().nextLong(totalNumberOfPossibleSwaps) < CombinatoricsUtil.binomial(groupA.size(),
          2)) {
        int randomOne = ThreadLocalRandom.current().nextInt(groupA.size());
        int randomTwo = 0;
        do {
          randomTwo = ThreadLocalRandom.current().nextInt(groupA.size());
        } while (randomOne == randomTwo);
        CombinatoricsUtil.swap(randomOne, randomTwo, orderA);
      } else {
        int randomOne = ThreadLocalRandom.current().nextInt(groupB.size());
        int randomTwo = 0;
        do {
          randomTwo = ThreadLocalRandom.current().nextInt(groupB.size());
        } while (randomOne == randomTwo);
        CombinatoricsUtil.swap(randomOne, randomTwo, orderB);
      }
      newGroupAList = CombinatoricsUtil.mapListToArray(groupAInitialOrder, orderA);
      newGroupBList = CombinatoricsUtil.mapListToArray(groupBInitialOrder, orderB);
    }

    // Build the Pair

    AlternatingBattingLineup newLineup = new AlternatingBattingLineup(newGroupAList, newGroupBList);
    long newIndex = getIndex(newLineup);
    return Pair.create(newIndex, newLineup);
  }

  @Override
  public long getIndex(AlternatingBattingLineup lineup) {

    List<DataPlayer> groupA = lineup.getGroupA();
    List<DataPlayer> groupAInitialOrder = groupA.get(0).getGender().equals("M") ? men : women;
    int[] orderA = CombinatoricsUtil.getOrdering(groupAInitialOrder, groupA);
    long groupAIndex = CombinatoricsUtil.getPermutationIndex(orderA);


    List<DataPlayer> groupB = lineup.getGroupB();
    List<DataPlayer> groupBInitialOrder = groupB.get(0).getGender().equals("F") ? women : men;
    int[] orderB = CombinatoricsUtil.getOrdering(groupBInitialOrder, groupB);
    long groupBIndex = CombinatoricsUtil.getPermutationIndex(orderB);

    if (groupA.get(0).getGender().equals("M")) {
      // Men bat first
      return groupBIndex * menPermutations + groupAIndex;
    } else {
      // Women bat first
      return (groupAIndex * menPermutations + groupBIndex) + this.size / 2;
    }

  }

}
