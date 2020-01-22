package com.github.thbrown.softballsim.server;

import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.datasource.network.DataSourceNetworkCommandInProgress;

public class ServerInProgress extends DataSourceNetworkCommandInProgress implements ServerCommand {

  public ServerInProgress(Result result) {
    super(result);
  }

  @Override
  public boolean process(ServerCommandHooks ps, ServerNetworkHelper network) throws Exception {
    return ps.onInProgress(this, network);
  }

}
