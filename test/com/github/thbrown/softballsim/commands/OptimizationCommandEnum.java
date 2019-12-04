package com.github.thbrown.softballsim.commands;

public enum OptimizationCommandEnum {
  READY("READY", ReadyOptimizationCommand.class),
  COMPLETE("COMPLETE", CompleteOptimizationCommand.class),
  IN_PROGRES("IN_PROGRESS", InProgressOptimizationCommand.class),
  ERROR("ERROR", ErrorOptimizationCommand.class);

  private final String apiValue;
  private Class<? extends BaseOptimizationCommand> deserializationTarget;

  OptimizationCommandEnum(String apiValue, Class<? extends BaseOptimizationCommand> deserializationTarget) {
    this.apiValue = apiValue;
    this.deserializationTarget = deserializationTarget;
  }

  public static OptimizationCommandEnum getEnumFromApiValue(String apiValue) {
    for (OptimizationCommandEnum v : values()) {
      if (v.getApiValue().equals(apiValue)) {
        return v;
      }
    }
    throw new RuntimeException("Invalid api value specified: " + apiValue);
  }

  public String getApiValue() {
    return apiValue;
  }

  public Class<? extends BaseOptimizationCommand> getDeserializationTarget() {
    return this.deserializationTarget;
  }
}
