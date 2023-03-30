package com.github.thbrown.softballsim.cloud;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

// https://stackoverflow.com/questions/5187242/encode-a-string-to-be-used-as-shell-argument
public class BashEscape {
  public static final Escaper SHELL_ESCAPE;
  static {
    final Escapers.Builder builder = Escapers.builder();
    builder.addEscape('\'', "'\"'\"'");
    SHELL_ESCAPE = builder.build();
  }
}
