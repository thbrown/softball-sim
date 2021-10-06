package com.github.thbrown.softballsim.cloud;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

public class BashEscape {
  public static final Escaper SHELL_ESCAPE;
  static {
    final Escapers.Builder builder = Escapers.builder();
    builder.addEscape('\'', "'\"'\"'");
    SHELL_ESCAPE = builder.build();
  }
}
