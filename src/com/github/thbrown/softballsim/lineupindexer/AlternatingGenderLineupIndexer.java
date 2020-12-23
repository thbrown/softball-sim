package com.github.thbrown.softballsim.lineupindexer;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.util.Pair;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.lineup.AlternatingBattingLineup;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.util.CombinatoricsUtil;

public class AlternatingGenderLineupIndexer implements BattingLineupIndexer {

  private List<DataPlayer> men = new ArrayList<>();
  private List<DataPlayer> women = new ArrayList<>();

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
  public BattingLineup getLineup(long index) {
    if (index >= this.size) {
      return null;
    }

    long menIndex = index % menPermutations;
    int[] menOrder = CombinatoricsUtil.getIthPermutation(men.size(), menIndex);
    List<DataPlayer> groupAOrderList = CombinatoricsUtil.mapListToArray(men, menOrder);

    long womenIndex = (int) Math.floor(index / menPermutations);
    int[] womenOrder = CombinatoricsUtil.getIthPermutation(women.size(), womenIndex);
    List<DataPlayer> groupBOrderList = CombinatoricsUtil.mapListToArray(women, womenOrder);

    if (index < this.size / 2) {
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
  public Pair<Long, BattingLineup> getRandomNeighbor(long index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getIndex(BattingLineup lineup) {
    throw new UnsupportedOperationException();
  }

}
