package com.github.thbrown.softballsim.datasource.network;

public class DataSourceNetworkCommandReady extends DataSourceNetworkCommand {

  @SuppressWarnings("unused") // Serialized
  private String optimizationId;

  public DataSourceNetworkCommandReady(String optimizationId) {
    super(DataSourceNetworkCommandReady.getType());
    this.optimizationId = optimizationId;
  }

  public static String getType() {
    return "READY";
  }

}
