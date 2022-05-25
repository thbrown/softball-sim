package com.github.thbrown.softballsim;

import java.util.HashMap;
import java.util.Map;

public class Scorer {

  public Map<String, Normalizer> executionTimeNormalizers = new HashMap<>();
  public Map<String, Normalizer> runsScoredNormalizers = new HashMap<>();

  public void add(String optimizer, String lineupType, int lineupLength, long executionTime, double runsScored) {
    // lineupType-lineupLength exists, add it to that, otherwise, create a new one

    String normKey = String.valueOf(lineupType) + "|" + String.valueOf(lineupLength);
    String resultKey =
        String.valueOf(lineupType) + "|" + String.valueOf(lineupLength) + "|" + String.valueOf(optimizer);
    if (!executionTimeNormalizers.containsKey(normKey)) {
      executionTimeNormalizers.put(normKey, new Normalizer());
    }
    executionTimeNormalizers.get(normKey).add(resultKey, executionTime);

    if (!runsScoredNormalizers.containsKey(normKey)) {
      runsScoredNormalizers.put(normKey, new Normalizer());
    }
    runsScoredNormalizers.get(normKey).add(resultKey, runsScored);
  }

  public double getSpeedScore(String optimizer, String lineupType, int lineupLength) {
    String normKey = String.valueOf(lineupType) + "|" + String.valueOf(lineupLength);
    String resultKey =
        String.valueOf(lineupType) + "|" + String.valueOf(lineupLength) + "|" + String.valueOf(optimizer);
    return 1 - executionTimeNormalizers.get(normKey).getNormalizedValue(resultKey); // TODO: handle NPE on key present?
  }

  public double getQualityScore(String optimizer, String lineupType, int lineupLength) {
    String normKey = String.valueOf(lineupType) + "|" + String.valueOf(lineupLength);
    String resultKey =
        String.valueOf(lineupType) + "|" + String.valueOf(lineupLength) + "|" + String.valueOf(optimizer);
    return runsScoredNormalizers.get(normKey).getNormalizedValue(resultKey); // TODO: handle NPE on key present?
  }



}
