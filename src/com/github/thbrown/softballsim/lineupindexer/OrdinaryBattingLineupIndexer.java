package com.github.thbrown.softballsim.lineupindexer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.util.Pair;
import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.OrdinaryBattingLineup;
import com.github.thbrown.softballsim.util.CombinatoricsUtil;

public class OrdinaryBattingLineupIndexer implements BattingLineupIndexer {
  private List<DataPlayer> players = new ArrayList<>();
  private long size;

  public OrdinaryBattingLineupIndexer(DataStats stats, List<String> players) {
    // Get the DataPlayers by id
    for (String playerId : players) {
      this.players.add(stats.getPlayerById(playerId));
    }
    try {
      this.size = CombinatoricsUtil.factorial(players.size());
    } catch (RuntimeException r) {
      throw new RuntimeException("Unable to index all possible lineups with that many players ("
          + stats.getPlayers().size()
          + "). If you have not specified a list of players, do so using the '" + CommandLineOptions.LINEUP
          + "' flag. Otherwise, don't specify more than 20 players", r);
    }
  }

  @Override
  public long size() {
    return this.size;
  }

  @Override
  public BattingLineup getLineup(long index) {
    if (index >= this.size) {
      return null;
    }
    int[] order = CombinatoricsUtil.getIthPermutation(players.size(), index);
    List<DataPlayer> lineup = CombinatoricsUtil.mapListToArray(players, order);
    return new OrdinaryBattingLineup(lineup);
  }

  @Override
  public Pair<Long, BattingLineup> getRandomNeighbor(long index) {
    // Get the current order
    int[] order = CombinatoricsUtil.getIthPermutation(players.size(), index);

    // If there is only one player in the lineup, there are no neighbors
    // TODO: Make sure there is a test case for this
    if (players.size() == 1) {
      return null;
    }

    // Swap any two elements
    int randomOne = ThreadLocalRandom.current().nextInt(players.size());
    int randomTwo = 0;
    do {
      randomTwo = ThreadLocalRandom.current().nextInt(players.size());
    } while (randomOne == randomTwo);
    CombinatoricsUtil.swap(randomOne, randomTwo, order);

    // Build the Pair
    long newIndex = CombinatoricsUtil.getPermutationIndex(order);
    List<DataPlayer> playersInLineup = CombinatoricsUtil.mapListToArray(players, order);
    OrdinaryBattingLineup lineup = new OrdinaryBattingLineup(playersInLineup);
    return Pair.create(newIndex, lineup);
  }

  @Override
  public long getIndex(BattingLineup lineup) {
    List<DataPlayer> listLineup = lineup.asList();
    int[] order = CombinatoricsUtil.getOrdering(listLineup, players);
    return CombinatoricsUtil.getPermutationIndex(order);
  }

}
