package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.HitGenerator;

/**
 * POJO that allows us to keep track of a lineup and it's associated objects in a single class.
 */
public class LineupComposite {
  private final SummaryStatistics stats;
  private final BattingLineup lineup;
  private final HitGenerator hitGenerator;
  private final Long lineupIndex;

  public LineupComposite(BattingLineup lineup, HitGenerator hitGenerator, Long lineupIndex) {
    this.stats = new SummaryStatistics();
    this.lineup = lineup;
    this.hitGenerator = hitGenerator;
    this.lineupIndex = lineupIndex;
  }

  public void addSample(double value) {
    stats.addValue(value);
  }

  public SummaryStatistics getStats() {
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
    return String.valueOf(this.hashCode());
  }

}
