package com.github.thbrown.softballsim.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

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
  private final static boolean WRITE_LOG_TO_FILE = false;

  // Color constants
  private final static String ANSI_RESET = "\u001B[0m";
  private final static String ANSI_RED = "\u001B[31m";
  private final static String ANSI_YELLOW = "\u001B[33m";

  private final static Object lock = new Object();

  private final static Map<String, PrintWriter> secondaryLogFiles = new HashMap<>();

  static PrintWriter writer;
  static {
    if (WRITE_LOG_TO_FILE) {
      try {
        synchronized (lock) {
          writer = new PrintWriter(new FileWriter("java.log", APPEND_MODE), true);
          writer.println("Logging started");
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
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

  public static void error(String error) {
    Logger.log(ANSI_RED + error + ANSI_RESET);
  }

  public static void warn(String warning) {
    Logger.log(ANSI_YELLOW + warning + ANSI_RESET);
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
   * Writes logs only to the specified file Note: this code has not been synchronized
   */
  public static void logToFile(String whatToLog, String filePath) {
    PrintWriter writer = null;
    if (secondaryLogFiles.containsKey(filePath)) {
      writer = secondaryLogFiles.get(filePath);
    } else {
      try {
        writer = new PrintWriter(new FileWriter(filePath, APPEND_MODE), true);
      } catch (IOException e) {
        e.printStackTrace();
      }
      secondaryLogFiles.put(filePath, writer);
    }
    writer.println(whatToLog);
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
