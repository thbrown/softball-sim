package com.github.thbrown.softballsim.util;

import java.io.IOException;
import java.text.NumberFormat;

public class MathUtil {
  /**
   * Takes a number (value) between oldMin and oldMax and maps linearly to a new value between newMin
   * and newMax
   */
  public static int transform(int value, int oldMin, int oldMax, int newMin, int newMax) {
    double x = (double) value;
    double a = (double) oldMin;
    double b = (double) oldMax;
    double c = (double) newMin;
    double d = (double) newMax;
    return (int) ((x - a) * (d - c) / (b - a) + c);
  }

  public static String percentChange(double A, double B) throws IOException {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(3);
    return nf.format(Math.abs(A - B) / ((A + B) / 2) * 100);
  }

}
