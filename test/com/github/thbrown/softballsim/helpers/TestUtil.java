package com.github.thbrown.softballsim.helpers;

import static org.junit.Assert.assertThat;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import com.github.thbrown.softballsim.util.Logger;
import org.hamcrest.CoreMatchers;

public class TestUtil {

  public static ByteArrayOutputStream redirectStdOut() {
    ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    return outContent;
  }

  public static void asssertContainsAll(String target, String... stringsTheTargetMustContains) {
    for (String s : stringsTheTargetMustContains) {
      assertThat(target, CoreMatchers.containsString(s));
    }
  }

  /**
   * Accepts an input between floorA and ceilB and linearly maps it to a value between floorB and
   * ceilB
   */
  public static double normalize(double input, double floorA, double ceilA, double floorB, double ceilB) {
    return floorB + ((input - floorA) * (ceilB - floorB)) / (ceilA - floorA);
  };

  public static List<Long> getPredictionArray(List<Long> dataPoints) {
    if (dataPoints.size() == 0) {
      return new ArrayList<>();
    }

    List<Long> slopes = new ArrayList<>(dataPoints.size() - 1);
    for (int i = 1; i < dataPoints.size(); i++) {
      slopes.add(dataPoints.get(i) - dataPoints.get(i - 1));
    }

    List<Long> recursiveResult = getPredictionArray(slopes);
    recursiveResult.add(dataPoints.get(dataPoints.size() - 1));
    return recursiveResult;
  }
}
