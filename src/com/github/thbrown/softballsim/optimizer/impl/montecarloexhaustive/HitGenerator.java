package com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import com.github.thbrown.softballsim.data.gson.DataPlayer;

/**
 * Class that simulates hits for a player based of their stats.
 * 
 * This class must remain immutable so the hit method can be called from multiple threads.
 */
public class HitGenerator {

  private final Map<String, Integer[]> hitValues = new HashMap<>();

  public HitGenerator(List<DataPlayer> players) {

    for (DataPlayer player : players) {
      List<Integer> resultBucket = new ArrayList<>();

      for (int i = 0; i < player.getHomerunCount(); i++) {
        resultBucket.add(4);
      }
      for (int i = 0; i < player.getTripleCount(); i++) {
        resultBucket.add(3);
      }
      for (int i = 0; i < player.getDoubleCount(); i++) {
        resultBucket.add(2);
      }
      for (int i = 0; i < (player.getSingleCount() + player.getWalkCount()); i++) {
        resultBucket.add(1);
      }
      for (int i = 0; i < (player.getOutCount() + player.getSacCount()); i++) {
        resultBucket.add(0);
      }

      // We get slightly better access performance from an array over an Arraylist
      Integer[] resultBucketArray = new Integer[resultBucket.size()];
      hitValues.put(player.getId(), resultBucket.toArray(resultBucketArray));
    }

  }

  public int hit(String playerId) {
    // TODO: Using a length that is a power of 2 is about 25% faster, there could be some optimization
    // vector here.
    Integer[] hitBucket = hitValues.get(playerId);
    int randomValue = ThreadLocalRandom.current().nextInt(hitBucket.length);
    return hitBucket[randomValue];
  }

}
