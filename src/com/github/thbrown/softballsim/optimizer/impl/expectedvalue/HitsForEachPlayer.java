package com.github.thbrown.softballsim.optimizer.impl.expectedvalue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.lineup.BattingLineup;

/**
 * An object from this class is used to get an array of hit values, by an index, in order of
 * likelihood.
 */
public class HitsForEachPlayer {

  private final List<List<Integer>> playersHits;

  public HitsForEachPlayer(BattingLineup lineup) {
    this.playersHits = new ArrayList<>(lineup.size());
    for (int i = 0; i < lineup.size(); i++) {
      DataPlayer player = lineup.getBatter(i);
      SortedMap<Double, Integer> hits = new TreeMap<>();

      double totalOuts = player.getOutCount();
      if (totalOuts > 0) {
        hits.put(totalOuts, 0);
      }
      double totalSingles = player.getSingleCount() + player.getWalkCount();
      if (totalSingles > 0) {
        hits.put(totalSingles, 1);
      }
      double totalDoubles = player.getDoubleCount();
      if (totalDoubles > 0) {
        hits.put(totalDoubles, 2);
      }
      double totalTriples = player.getTripleCount();
      if (totalTriples > 0) {
        hits.put(totalTriples, 3);
      }
      double homeruns = player.getHomerunCount();
      if (homeruns > 0) {
        hits.put(homeruns, 4);
      }

      // TODO: What if players have no data??

      playersHits.add(new ArrayList<>(hits.values()));
    }
  }

  public List<Integer> getHitsByIndex(long index, int maxOuts, int maxPlateAppearances) {
    List<Integer> result = new ArrayList<>();
    BigInteger denominator = BigInteger.ONE;
    int playerIndex = 0;
    int outs = 0;
    int plateAppearances = 0;
    while (plateAppearances < maxPlateAppearances && outs < maxOuts) {
      int hitIndex = BigInteger.valueOf(index).divide(denominator)
          .mod(BigInteger.valueOf(this.playersHits.get(playerIndex).size())).intValueExact();
      int hitValue = this.playersHits.get(playerIndex).get(hitIndex);
      result.add(hitValue);
      if (hitValue == 0) {
        outs++;
      }
      plateAppearances++;

      denominator = denominator.multiply(BigInteger.valueOf(this.playersHits.get(playerIndex).size()));
      playerIndex = (playerIndex + 1) % this.playersHits.size();
    }
    return result;
  }

}
