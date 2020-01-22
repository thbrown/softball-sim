package com.github.thbrown.softballsim.server;

import com.github.thbrown.softballsim.datasource.network.DataSourceNetworkCommandReady;

public class ServerReady extends DataSourceNetworkCommandReady implements ServerCommand {

  public ServerReady(String optimizationId) {
    super(optimizationId);
  }

  @Override
  public boolean process(ServerCommandHooks ps, ServerNetworkHelper network) throws Exception {
    return ps.onReady(this, network);
  }

}
