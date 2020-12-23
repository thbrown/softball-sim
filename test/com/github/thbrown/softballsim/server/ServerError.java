package com.github.thbrown.softballsim.server;

import com.github.thbrown.softballsim.datasource.network.DataSourceNetworkCommandError;

public class ServerError extends DataSourceNetworkCommandError implements ServerCommand {

  public ServerError(String message, String trace) {
    super(message, trace);
  }

  @Override
  public boolean process(ServerCommandHooks ps, ServerNetworkHelper network) throws Exception {
    return ps.onError(this, network);
  }

}
