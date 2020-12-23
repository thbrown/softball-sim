package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.HitGenerator;

/**
 * POJO that allows us to keep track of a lineup and it's associated objects (index, stats,
 * hitGenerator) in a single class.
 */
public class LineupComposite {
  private StatisticalSummary stats;
  private final SummaryStatistics modifiableStats;
  private final BattingLineup lineup;
  private final HitGenerator hitGenerator;
  private final Long lineupIndex;

  public LineupComposite(BattingLineup lineup, HitGenerator hitGenerator, Long lineupIndex) {
    this.modifiableStats = new SummaryStatistics();
    this.stats = new StatisticalSummaryValues(0, 0, 0, 0, 0, 0); // Empty
    this.lineup = lineup;
    this.hitGenerator = hitGenerator;
    this.lineupIndex = lineupIndex;
  }

  public LineupComposite(LineupComposite toCopy) {
    this.modifiableStats = new SummaryStatistics();
    mergeModStatsIntoStats();
    this.stats = toCopy.stats;
    this.lineup = toCopy.lineup;
    this.hitGenerator = toCopy.hitGenerator;
    this.lineupIndex = toCopy.lineupIndex;
  }

  /*
   * private LineupComposite() { this.modifiableStats = new SummaryStatistics(); }
   */

  public void addSample(double value) {
    modifiableStats.addValue(value);
  }

  public StatisticalSummary getStats() {
    mergeModStatsIntoStats();
    return stats;
  }

  public BattingLineup getLineup() {
    return lineup;
  }

  public HitGenerator getHitGenerator() {
    return hitGenerator;
  }

  public Long lineupIndex() {
    return lineupIndex;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(String.valueOf(this.hashCode()));
    builder.append("\n");
    builder.append(lineupIndex);
    builder.append("\n");
    builder.append(lineup);
    return builder.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(lineup);
  }

  /**
   * When testing for equality, we only care if the lineups are the same
   */
  @Override
  public boolean equals(Object other) {
    if (other instanceof LineupComposite) {
      if (((LineupComposite) other).lineup.equals(this.lineup)) {
        return true;
      }
    }
    return false;
  }

  public long getIndex() {
    return lineupIndex;
  }

  public void incorperateAdditionalStats(StatisticalSummary additionalData) {
    List<StatisticalSummary> combo = new ArrayList<>(3);
    combo.add(modifiableStats);
    combo.add(additionalData);
    combo.add(stats);

    this.stats = AggregateSummaryStatistics.aggregate(combo);

    modifiableStats.clear();
  }

  private void mergeModStatsIntoStats() {
    if (modifiableStats.getN() > 0) {
      List<StatisticalSummary> combo = new ArrayList<>(2);
      combo.add(modifiableStats);
      combo.add(stats);

      this.stats = AggregateSummaryStatistics.aggregate(combo);

      modifiableStats.clear();
    }
  }


}
