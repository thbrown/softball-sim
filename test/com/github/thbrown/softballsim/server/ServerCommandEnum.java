package com.github.thbrown.softballsim.server;

public enum ServerCommandEnum {
  READY(ServerReady.getType(), ServerReady.class),
  COMPLETE(ServerComplete.getType(), ServerComplete.class),
  IN_PROGRES(ServerInProgress.getType(), ServerInProgress.class),
  ERROR(ServerError.getType(), ServerError.class);

  private final String apiValue;
  private Class<? extends ServerCommand> deserializationTarget;

  ServerCommandEnum(String apiValue, Class<? extends ServerCommand> deserializationTarget) {
    this.apiValue = apiValue;
    this.deserializationTarget = deserializationTarget;
  }

  public static ServerCommandEnum getEnumFromApiValue(String apiValue) {
    for (ServerCommandEnum v : values()) {
      if (v.getApiValue().equals(apiValue)) {
        return v;
      }
    }
    throw new RuntimeException("Invalid api value specified: " + apiValue);
  }

  public String getApiValue() {
    return apiValue;
  }

  public Class<? extends ServerCommand> getDeserializationTarget() {
    return this.deserializationTarget;
  }
}
