package com.github.thbrown.softballsim.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * General logger implementation.
 * 
 * @author thbrown
 */
public class Logger {

  // Config
  private final static boolean SHOW_TIMESTAMPS = false;
  private final static boolean SHOW_FILE_AND_LINE = false;
  private final static boolean APPEND_MODE = false;
  private final static boolean WRITE_LOG_TO_FILE = true;

  private final static Object lock = new Object();

  static PrintWriter writer;
  static {
    try {
      synchronized (lock) {
        writer = new PrintWriter(new FileWriter("java.log", APPEND_MODE), true);
        writer.println("Logging started");
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // Runtime.getRuntime().addShutdownHook(new LoggerShuterDowner());
  }

  public static void closeLogger() {
    synchronized (lock) {
      writer.close();
    }
  }

  public static void log(Object toWrite) {
    if (SHOW_TIMESTAMPS) {
      toWrite = appendTime(toWrite.toString());
    }
    if (SHOW_FILE_AND_LINE) {
      toWrite = appendFileAndLineNumber(toWrite.toString());
    }
    if (WRITE_LOG_TO_FILE) {
      synchronized (lock) {
        writer.println(toWrite);
      }
    }
    System.out.println(toWrite);
  }

  public static void log(Exception e) {
    synchronized (lock) {
      writer.println(e);
    }
    e.printStackTrace();
  }

  /**
   * Logs the exception
   */
  public static void log(Object string, Exception e) {
    RuntimeException rte = new RuntimeException(string.toString(), e);
    Logger.log(rte);
  }

  /**
   * Hackish and not robust, but useful for cleaning up log lines during development.
   * 
   * @param s
   * @return
   */
  private static String appendFileAndLineNumber(String s) {
    StackTraceElement[] ste = Thread.currentThread().getStackTrace();
    String loggerLocation = null;
    for (int i = 1; i < ste.length; i++) {
      if (ste[i].toString().contains(Logger.class.getSimpleName() + ".java")) {
        continue;
      }
      loggerLocation = ste[i].toString();
      break;
    }
    // Eclipse will not hyperlink properly if there is no space before loggerLocation
    return s + "\tat " + loggerLocation;
  }

  private static String appendTime(String s) {
    return System.currentTimeMillis() + "\t" + s;
  }

}
