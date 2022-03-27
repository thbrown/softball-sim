package com.github.thbrown.softballsim;

public enum ResultStatusEnum {
  NOT_STARTED(false, false),
  IN_PROGRESS(false, true),
  PAUSED(true, false),
  ESTIMATE(true, false),
  COMPLETE(true, false),
  ERROR(true, false),
  ALLOCATING_RESOURCES(false, true);

  private final boolean isTerminal;
  private final boolean isActive;

  private ResultStatusEnum(boolean isTerminal, boolean isActive) {
    this.isTerminal = isTerminal;
    this.isActive = isActive;
  }

  public boolean isTerminal() {
    return isTerminal;
  }

  public boolean isActive() {
    return isActive;
  }

}
