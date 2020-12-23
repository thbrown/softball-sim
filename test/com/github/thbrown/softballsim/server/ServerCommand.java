package com.github.thbrown.softballsim.server;

/**
 * This interfaces allows us to add a server-side processing method to the production commands.
 * Since the server is test only code, this allows us to keep these methods out of production code.
 */
public interface ServerCommand {

  /**
   * This code is invoked when the command is received over the network.
   * 
   * @param ps contains the code that should be invoked when the command is received
   * @param network provides that code the ability to send a reply over the network
   */
  public abstract boolean process(ServerCommandHooks ps, ServerNetworkHelper network) throws Exception;
}
