package com.github.thbrown.softballsim.datasource.network;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.Msg;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.datasource.DataSourceFunctions;
import com.github.thbrown.softballsim.datasource.EstimateOnlyExecutionWrapper;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.CollectionUtils;

public class DataSourceNetworkCommandData extends DataSourceNetworkCommand {

  public static String getType() {
    return "DATA";
  }

  public DataStats stats;
  public Map<String, String> args;

  public DataSourceNetworkCommandData(DataStats dataStats, Map<String, String> args) {
    super(DataSourceNetworkCommandData.getType());
    this.stats = dataStats;
    this.args = args;
  }

  @Override
  public void process(String[] args, LineupTypeEnum lineupType, List<String> players, OptimizerEnum optimizer,
      NetworkHelper network) {
    // This implementation allows arguments to come in over the network as well. Here we'll merge the
    // flags from the CLI with the arguments from the network and re-parse.

    Set<String> keys = new HashSet<>(this.args.keySet());
    for (String s : keys) {
      CollectionUtils.renameMapKey(this.args, s, "--" + s);
    }
    String[] networkArgs = CollectionUtils.flattenMap(this.args);
    args = CollectionUtils.concatenate(args, networkArgs);

    // Now that we have the arguments from the network, we can parse all the args together
    CommandLineOptions commandLineOptions = CommandLineOptions.getInstance();

    Options commonOptions = commandLineOptions.getOptionsForFlags(null, null);
    CommandLine commonCmd = commandLineOptions.parse(commonOptions, args, true);

    if (commonCmd.hasOption(CommandLineOptions.OPTIMIZER)) {
      String optimizerString = commonCmd.getOptionValue(CommandLineOptions.OPTIMIZER);
      optimizer = OptimizerEnum.getEnumFromIdOrName(optimizerString);
    }

    // Require an optimizer
    if (optimizer == null) {
      throw new RuntimeException(
          Msg.MISSING_OPTIMIZER.args(OptimizerEnum.getValuesAsString()));
    }

    Options allOptions = commandLineOptions.getOptionsForFlags(DataSourceEnum.NETWORK, optimizer);
    CommandLine allCmd = commandLineOptions.parse(allOptions, args, false);

    String lineupTypeString =
        allCmd.getOptionValue(CommandLineOptions.LINEUP_TYPE, CommandLineOptions.TYPE_LINEUP_DEFAULT);
    lineupType = LineupTypeEnum.getEnumFromIdOrName(lineupTypeString);

    String playerString =
        allCmd.getOptionValue(CommandLineOptions.LINEUP, ""); // TODO: make this required?
    players = Arrays.asList(playerString.split(","));

    // Convert arguments list to map
    Map<String, String> arguments = optimizer.getArgumentsAndValuesAsMap(allCmd);

    // We accept both ids and names for this argument, but the optimizers expect only ids. This resolves
    // any names to ids. If there are duplicate args, the first arg in the string will be used.
    players = stats.convertPlayersListToIds(players);

    DataSourceFunctions functions = new DataSourceFunctionsNetwork(network);
    ProgressTracker tracker = new ProgressTracker(new Result(null, null, 0, 0, 0, 0), functions);
    Thread trackerThread = new Thread(tracker);
    trackerThread.start();

    try {
      if (allCmd.hasOption(CommandLineOptions.ESTIMATE_ONLY)) {
        // This will terminate the application
        EstimateOnlyExecutionWrapper wrapper = new EstimateOnlyExecutionWrapper(optimizer, functions);
        wrapper.optimize(players, lineupType, stats, arguments, tracker, null);
      } else {
        Result result = optimizer.optimize(players, lineupType, stats, arguments, tracker, null);
        functions.onComplete(result);
      }
    } finally {
      trackerThread.interrupt();
    }
  }
}
