package com.github.thbrown.softballsim.util;

/**
 * Using custom stringUtils to avoid using Apache commons to keep jar size down.
 * 
 * https://commons.apache.org/proper/commons-lang/apidocs/src-html/org/apache/commons/lang3/StringUtils.html
 */
public class StringUtils {
  public static String trim(final String str) {
    return str == null ? null : str.trim();
  }

  public static String formatDecimal(double value, int numberOfValuesAfterDecimal) {
    return String.format(java.util.Locale.US, "%." + numberOfValuesAfterDecimal + "f", value);
  }

  // http://stackoverflow.com/questions/388461/how-can-i-pad-a-string-in-java
  public static String padRight(String s, int n) {
    return String.format("%1$-" + n + "s", s);
  }

  public static boolean isBlank(final CharSequence cs) {
    int strLen;
    if (cs == null || (strLen = cs.length()) == 0) {
      return true;
    }
    for (int i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(cs.charAt(i))) {
        return false;
      }
    }
    return true;
  }

}
