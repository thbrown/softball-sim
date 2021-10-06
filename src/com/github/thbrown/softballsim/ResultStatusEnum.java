package com.github.thbrown.softballsim;

public enum ResultStatusEnum {
  NOT_STARTED(false),
  IN_PROGRESS(false),
  PAUSED(true),
  ESTIMATE(true),
  COMPLETE(true),
  ERROR(true);

  private final boolean isTerminal;

  private ResultStatusEnum(boolean isTerminal) {
    this.isTerminal = isTerminal;
  }

  public boolean isTerminal() {
    return isTerminal;
  }

}
