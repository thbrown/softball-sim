package com.github.thbrown.softballsim.datasource.network;

import com.github.thbrown.softballsim.Result;

public class DataSourceNetworkCommandComplete extends DataSourceNetworkCommand {

  public static String getType() {
    return "COMPLETE";
  }

  private Result result;

  public DataSourceNetworkCommandComplete(Result finalResult) {
    super(DataSourceNetworkCommandComplete.getType());
    this.result = finalResult;
  }

  public Result getResult() {
    return result;
  }

}
