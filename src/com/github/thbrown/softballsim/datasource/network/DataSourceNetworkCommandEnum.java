package com.github.thbrown.softballsim.datasource.network;


public enum DataSourceNetworkCommandEnum {
  READY(DataSourceNetworkCommandReady.getType(), DataSourceNetworkCommandReady.class),
  DATA(DataSourceNetworkCommandData.getType(), DataSourceNetworkCommandData.class),
  COMPLETE(DataSourceNetworkCommandComplete.getType(), DataSourceNetworkCommandComplete.class),
  IN_PROGRESS(DataSourceNetworkCommandInProgress.getType(), DataSourceNetworkCommandInProgress.class),
  ESTIMATE(DataSourceNetworkCommandEstimate.getType(), DataSourceNetworkCommandEstimate.class),
  ERROR(DataSourceNetworkCommandError.getType(), DataSourceNetworkCommandError.class);

  private final String apiValue;
  private Class<? extends DataSourceNetworkCommand> deserializationTarget;

  DataSourceNetworkCommandEnum(String apiValue, Class<? extends DataSourceNetworkCommand> deserializationTarget) {
    this.apiValue = apiValue;
    this.deserializationTarget = deserializationTarget;
  }

  public static DataSourceNetworkCommandEnum getEnumFromApiValue(String apiValue) {
    for (DataSourceNetworkCommandEnum v : values()) {
      if (v.getApiValue().equals(apiValue)) {
        return v;
      }
    }
    throw new RuntimeException("Invalid api value specified: " + apiValue);
  }

  public String getApiValue() {
    return apiValue;
  }

  public Class<? extends DataSourceNetworkCommand> getDeserializationTarget() {
    return this.deserializationTarget;
  }
}
