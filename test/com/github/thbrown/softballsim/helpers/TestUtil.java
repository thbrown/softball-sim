package com.github.thbrown.softballsim.helpers;

import static org.junit.Assert.assertThat;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
}
