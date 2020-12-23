package com.github.thbrown.softballsim.datasource.network;

import com.github.thbrown.softballsim.Result;

public class DataSourceNetworkCommandInProgress extends DataSourceNetworkCommand {

  public static String getType() {
    return "IN_PROGRESS";
  }

  private Result result;

  public DataSourceNetworkCommandInProgress(Result result) {
    super(DataSourceNetworkCommandInProgress.getType());
    this.result = result;
  }

  public Result getResult() {
    return result;
  }

}
