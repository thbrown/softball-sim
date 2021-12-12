package com.github.thbrown.softballsim;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Test;

public class AggregateTest {

  @Test
  public void deserializeExampleStatsData() throws IOException {

    SummaryStatistics s1 = new SummaryStatistics();
    s1.addValue(1);
    s1.addValue(1);
    s1.addValue(1);

    StatisticalSummaryValues i = AggregateSummaryStatistics.aggregate(Collections.singletonList(s1));

    SummaryStatistics s2 = new SummaryStatistics();
    s2.addValue(3);
    s2.addValue(3);
    s2.addValue(3);

    List<StatisticalSummary> combo = new ArrayList<>();
    combo.add(s2);
    combo.add(i);

    StatisticalSummaryValues j = AggregateSummaryStatistics.aggregate(combo);

    SummaryStatistics s3 = new SummaryStatistics();
    s3.addValue(1);
    s3.addValue(1);
    s3.addValue(1);
    s3.addValue(3);
    s3.addValue(3);
    s3.addValue(3);

    System.out.println(j.getStandardDeviation() + " " + s3.getStandardDeviation());
  }

}
