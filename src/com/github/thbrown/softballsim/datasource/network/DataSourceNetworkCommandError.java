package com.github.thbrown.softballsim.datasource.network;

public class DataSourceNetworkCommandError extends DataSourceNetworkCommand {

  public static String getType() {
    return "ERROR";
  }

  private String message;
  private String trace;

  public DataSourceNetworkCommandError(String message, String trace) {
    super(DataSourceNetworkCommandError.getType());
    this.message = message;
    this.trace = trace;
  }

  public String getMessage() {
    return message;
  }

  public String getTrace() {
    return trace;
  }

}
