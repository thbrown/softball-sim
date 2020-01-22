package com.github.thbrown.softballsim.server;

import com.github.thbrown.softballsim.datasource.network.NetworkHelper;

/**
 * Implement this interface to specify code that should be run in response to the different status
 * updates from the server.
 * 
 * This implementation should be passes as an argument to
 * {@link com.github.thbrown.softballsim.server.Server}
 */
public interface ServerCommandHooks {

  public default boolean onReady(ServerReady data, NetworkHelper network) throws Exception {
    return false;
  }

  public default boolean onInProgress(ServerInProgress inProgressOptimizationCommand, NetworkHelper network) {
    return false;
  }

  public default boolean onComplete(ServerComplete data, NetworkHelper network) throws Exception {
    return true;
  }

  public default boolean onError(ServerError errorOptimizationCommand, NetworkHelper network) {
    return true;
  }

}
