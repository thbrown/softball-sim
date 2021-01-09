package com.github.thbrown.softballsim.datasource.network;

import java.util.List;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;

/**
 * Base class for messages sent between the server and the CLI running in network mode.
 */
public abstract class DataSourceNetworkCommand {

  @SuppressWarnings("unused") // for serialization
  private String type;

  protected DataSourceNetworkCommand(String type) {
    this.type = type;
  }

  /**
   * Code that gets executed when the CLI receives this command over the network.
   * 
   * By default, we assume that this is a command that only gets sent from the CLI to the server so
   * we'll throw an UnsupportedOperationException.
   * 
   * @param args array of arguments supplied when starting the application
   * @param optimizer
   * @param network
   */
  public Result process(String[] args, LineupTypeEnum lineupType, List<String> players, OptimizerEnum optimizer,
      NetworkHelper network) {
    throw new UnsupportedOperationException("This command should never be recieved from the server.");
  };

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

}
