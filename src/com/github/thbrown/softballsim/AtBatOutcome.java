package com.github.thbrown.softballsim;

public enum AtBatOutcome {
  SINGLE("1"),
  DOUBLE("2"),
  TRIPLE("3"),
  HOME_RUN("4"),
  WALK("BB"),
  OUT("0");

  private String stringValue;

  private AtBatOutcome(String stringValue) {
    this.stringValue = stringValue;
  }

  public String getStringValue() {
    return stringValue;
  }

  public static boolean isValidAtBatOutcome(String stringValue) {
    AtBatOutcome[] values = AtBatOutcome.values();
    for (int i = 0; i < values.length; i++) {
      if (stringValue.trim().equals(values[i].stringValue)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("%s - %s", this.name(), stringValue);
  }
}
