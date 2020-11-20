package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.statstransform;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;

/**
 * SummaryStatistics child class that adjusts the mean by some value before returning it
 */
@SuppressWarnings("serial")
public class MeanAdjustedSummaryStatistics extends StatisticalSummaryValues {

  private double adjustmentValue;

  public MeanAdjustedSummaryStatistics(StatisticalSummary a, double adjustmentValue) {
    super(a.getMean(), a.getVariance(), a.getN(), a.getMax(), a.getMin(), a.getSum());
    this.adjustmentValue = adjustmentValue;
  }

  @Override
  public double getMean() {
    return super.getMean() + this.adjustmentValue;
  }

}
