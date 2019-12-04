package com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive;

import java.util.HashMap;
import java.util.Map;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.lineup.BattingLineup;

public class MonteCarloExhaustiveResult extends Result {

  private final Map<Long, Long> histogram = new HashMap<>();

  public MonteCarloExhaustiveResult(BattingLineup lineup, double lineupScore, long countTotal, long countCompleted,
      long elapsedTimeMs, Map<Long, Long> histogram) {
    super(lineup, lineupScore, countTotal, countCompleted, elapsedTimeMs);
  }

  public Map<Long, Long> getHistogram() {
    return histogram;
  }

}
