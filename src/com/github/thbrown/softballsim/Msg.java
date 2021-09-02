package com.github.thbrown.softballsim;

import java.text.MessageFormat;

public enum Msg {

  MISSING_OPTIMIZER(
      "Optimizer (-o) is a required flag. Please specify one of the following options either as a name or as an ordinal. {0}"),
  BAD_PATH("Unable to read the file from {0}."),
  PLAYER_HAS_NO_PA(
      "Can not optimize lineup because player {0} ({1}) has no plate appearances");

  private String message;

  Msg(String message) {
    this.message = message;
  }

  public String args(Object... args) {
    return MessageFormat.format(message, args);
  }

  /**
   * Segments the message into an array of strings split at the location of their placeholders.
   */
  public String[] splitOnPlaceholders() {
    return message.split("\\{[0-9]+\\}");
  }

}
