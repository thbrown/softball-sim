package com.github.thbrown.softballsim.util;

import java.util.Comparator;
import java.util.List;

public class StatsUtil {
  public static double stdev(int[] list) {
    double sum = 0.0;
    double mean = 0.0;
    double num = 0.0;
    double numi = 0.0;

    for (int i : list) {
      sum += i;
    }
    mean = sum / list.length;

    for (int i : list) {
      numi = Math.pow((double) (i - mean), 2);
      num += numi;
    }

    return Math.sqrt(num / list.length);
  }
  
  public static double stdev(List<Integer> list) {
    return stdev(list.stream().mapToInt(i->i).toArray());
  }

  public static double mean(int[] list) {
    double sum = 0.0;
    for (int i : list) {
      sum += i;
    }
    return sum / list.length;
  }

  public static Comparator<List<Integer>> getStdDevComparator() {
    // This does a lot of re-computation of std dev
    return new Comparator<List<Integer>>() {
      @Override
      public int compare(List<Integer> o1, List<Integer> o2) {
        double diff = stdev(o2) - stdev(o1);
        if (diff == 0) {
          return 0;
        } else if (diff < 0) {
          return -1;
        } else {
          return 1;
        }
      }
    };
  }

}
