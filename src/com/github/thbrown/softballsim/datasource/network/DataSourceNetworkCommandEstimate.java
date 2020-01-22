package com.github.thbrown.softballsim.datasource.network;

public class DataSourceNetworkCommandEstimate extends DataSourceNetworkCommand {

  long estimatedSecondsToRun;

  public static String getType() {
    return "ESTIMATE";
  }

  public DataSourceNetworkCommandEstimate(long estimatedSecondsToRun) {
    super(DataSourceNetworkCommandEstimate.getType());
    this.estimatedSecondsToRun = estimatedSecondsToRun;
  }

}
