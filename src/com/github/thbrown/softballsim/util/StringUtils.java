package com.github.thbrown.softballsim.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

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

  public static String calculateMd5AsHex(String input) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
      md.update(input.getBytes());
      byte[] digest = md.digest();
      String hexHash = StringUtils.bytesToHex(digest);
      return hexHash;
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static String escapeJson(String input) {
    JsonPrimitive json = new JsonPrimitive(input);
    return new Gson().toJson(json);
  }

  public static String unescapeJson(String input) {
    Gson gson = new GsonBuilder().setLenient().create();
    JsonElement data = gson.fromJson(input.trim(), JsonElement.class);
    return data.getAsJsonPrimitive().getAsString();
  }

  /**
   * Crack a command line.
   *
   * @param toProcess the command line to process
   * @return the command line broken into strings. An empty or null toProcess parameter results in a
   *         zero sized array
   * 
   *         From Apache Ant:
   *         https://commons.apache.org/proper/commons-exec/apidocs/src-html/org/apache/commons/exec/CommandLine.html
   */
  public static String[] translateCommandline(final String toProcess) {
    if (toProcess == null || toProcess.length() == 0) {
      // no command? no string
      return new String[0];
    }

    // parse with a simple finite state machine

    final int normal = 0;
    final int inQuote = 1;
    final int inDoubleQuote = 2;
    int state = normal;
    final StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
    final ArrayList<String> list = new ArrayList<String>();
    StringBuilder current = new StringBuilder();
    boolean lastTokenHasBeenQuoted = false;

    while (tok.hasMoreTokens()) {
      final String nextTok = tok.nextToken();
      switch (state) {
        case inQuote:
          if ("\'".equals(nextTok)) {
            lastTokenHasBeenQuoted = true;
            state = normal;
          } else {
            current.append(nextTok);
          }
          break;
        case inDoubleQuote:
          if ("\"".equals(nextTok)) {
            lastTokenHasBeenQuoted = true;
            state = normal;
          } else {
            current.append(nextTok);
          }
          break;
        default:
          if ("\'".equals(nextTok)) {
            state = inQuote;
          } else if ("\"".equals(nextTok)) {
            state = inDoubleQuote;
          } else if (" ".equals(nextTok)) {
            if (lastTokenHasBeenQuoted || current.length() != 0) {
              list.add(current.toString());
              current = new StringBuilder();
            }
          } else {
            current.append(nextTok);
          }
          lastTokenHasBeenQuoted = false;
          break;
      }
    }

    if (lastTokenHasBeenQuoted || current.length() != 0) {
      list.add(current.toString());
    }

    if (state == inQuote || state == inDoubleQuote) {
      throw new IllegalArgumentException("Unbalanced quotes in " + toProcess);
    }

    final String[] args = new String[list.size()];
    return list.toArray(args);
  }
}
