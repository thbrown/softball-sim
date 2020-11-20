package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.statstransform;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.util.Pair;

/**
 * This interface is intended to allow the modification of SummaryStatistics before they are passed
 * to a tTest for evaluation.
 * 
 * This allows the caller to change what the t-test is measuring.
 */
public interface SummaryStatisticsTransform {

  Pair<StatisticalSummary, StatisticalSummary> transform(StatisticalSummary statsA, StatisticalSummary statsB);

}


