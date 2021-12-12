package com.github.thbrown.softballsim.util;

import java.util.Collections;
import java.util.Map;

public class HistogramUtils {

  private static Long getMaxValue(Map<String, Long> map) {
    return Collections.max(map.values());
  }

  private static int getMaxKeyLength(Map<String, Long> map) {
    int longest = 0;
    for (String string : map.keySet()) {
      if (string.length() > longest) {
        longest = string.length();
      }
    }
    return longest;
  }

  private static int normalize(long value, long maxValue, long normalizeTo) {
    double normalizedLong = (double) (value * normalizeTo) / maxValue;
    return (int) Math.round(normalizedLong);
  }

  private static String padString(String stringToPad, int length) {
    return String.format("%-" + length + "s", stringToPad);
  }

  public static String buildHistogram(Map<String, Long> map, long normalizeTo, String character) {
    Long maxValue = getMaxValue(map);
    int maxValueStringLength = maxValue.toString().length();
    int maxKeyStringLength = getMaxKeyLength(map);

    StringBuilder sb = new StringBuilder();

    for (Map.Entry<String, Long> entry : map.entrySet()) {
      int normalizedValue = normalize(entry.getValue(), maxValue, normalizeTo);

      sb.append(padString(entry.getKey(), maxKeyStringLength) + " | "
          + padString(entry.getValue().toString(), maxValueStringLength) + " | "
          + character.repeat(normalizedValue) + System.lineSeparator());
    }
    return sb.toString().trim();
  }

}
