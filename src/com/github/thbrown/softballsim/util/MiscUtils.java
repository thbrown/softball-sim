package com.github.thbrown.softballsim.util;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineupindexer.BattingLineupIndexer;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.HitGenerator;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloGameSimulation;

public class MiscUtils {

  public static String getRandomString(int targetStringLength) {
    int leftLimit = 97; // letter 'a'
    int rightLimit = 122; // letter 'z'

    Random random = new Random();

    String generatedString = random.ints(leftLimit, rightLimit + 1)
        .limit(targetStringLength)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();

    return generatedString;
  }

  /**
   * Calculate summary statistics of a sampling of lineups from an lineup indexer.
   */
  public static SummaryStatistics getSummaryStatisticsForIndexer(BattingLineupIndexer indexer, long sampleSize,
      long games, int innings) {
    SummaryStatistics result = new SummaryStatistics();
    for (int i = 0; i < sampleSize; i++) {
      long randomIndex = ThreadLocalRandom.current().nextLong(0, indexer.size());
      BattingLineup randomLineup = indexer.getLineup(randomIndex);
      HitGenerator hitGenerator = new HitGenerator(randomLineup.asList());
      SummaryStatistics lineupSummaryStatistics = new SummaryStatistics();
      for (int j = 0; j < games; j++) {
        double score = MonteCarloGameSimulation.simulateGame(randomLineup, innings, hitGenerator);
        lineupSummaryStatistics.addValue(score);
      }
      result.addValue(lineupSummaryStatistics.getMean());
    }
    return result;
  }

}
