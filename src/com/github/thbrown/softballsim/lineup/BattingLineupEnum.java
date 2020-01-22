package com.github.thbrown.softballsim.lineup;

public enum BattingLineupEnum {
  ORDINARY(OrdinaryBattingLineup.getType(), OrdinaryBattingLineup.class),
  ALTERNATING(AlternatingBattingLineup.getType(), AlternatingBattingLineup.class);

  private final String apiValue;
  private Class<? extends BattingLineup> deserializationTarget;

  private BattingLineupEnum(String apiValue, Class<? extends BattingLineup> deserializationTarget) {
    this.apiValue = apiValue;
    this.deserializationTarget = deserializationTarget;
  }

  public static BattingLineupEnum getEnumFromApiValue(String apiValue) {
    for (BattingLineupEnum v : values()) {
      if (v.getApiValue().equals(apiValue)) {
        return v;
      }
    }
    throw new RuntimeException("Invalid api value specified: " + apiValue);
  }

  public String getApiValue() {
    return apiValue;
  }

  public Class<? extends BattingLineup> getDeserializationTarget() {
    return this.deserializationTarget;
  }
}
