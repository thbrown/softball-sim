package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.statstransform;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.Pair;

/**
 * Pushes the distributions of two SummaryStatistics away from each other by some range.
 * 
 * This is useful for performing tTests that determine if some mean is within some range of another
 * mean. Larger ranges require fewer sample points to reach the significants value.
 */
public class RangeSummaryStatisticsTransform implements SummaryStatisticsTransform {

  private final double range;

  public RangeSummaryStatisticsTransform(double range) {
    this.range = Math.abs(range);
  }

  public Pair<StatisticalSummary, StatisticalSummary> transform(StatisticalSummary a, StatisticalSummary b) {
    if (a.getMean() > b.getMean()) {
      return new Pair<StatisticalSummary, StatisticalSummary>(new MeanAdjustedSummaryStatistics(a, this.range), b);
    } else {
      return new Pair<StatisticalSummary, StatisticalSummary>(a, new MeanAdjustedSummaryStatistics(b, this.range));
    }
  }

}
