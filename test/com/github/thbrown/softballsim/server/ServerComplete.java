package com.github.thbrown.softballsim.server;

import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.datasource.network.DataSourceNetworkCommandComplete;

public class ServerComplete extends DataSourceNetworkCommandComplete implements ServerCommand {

  public ServerComplete(Result finalResult) {
    super(finalResult);
  }

  @Override
  public boolean process(ServerCommandHooks ps, ServerNetworkHelper network) throws Exception {
    return ps.onComplete(this, network);
  }

}
