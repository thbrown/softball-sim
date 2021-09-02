package com.github.thbrown.softballsim;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;

public class SoftballSim {

  public static void main(String[] args) throws ParseException {
    try {
      mainInternal(args);
    } catch (Exception e) {
      Logger.error(e.getMessage());
      e.printStackTrace();
    }
  }

  public static Result mainInternal(String[] args) throws MissingArgumentException {
    // The valid command line flags change based on which optimizer and data source are supplied.
    CommandLineOptions commandLineOptions = CommandLineOptions.getInstance();

    // If there were no arguments supplied, show help with only the common options (i.e. no additional
    // flags for optimizer, and the default flags for data source)
    if (args.length == 0) {
      Options availableOptions =
          commandLineOptions.getOptionsForFlags(DataSourceEnum.getEnumFromName(CommandLineOptions.DATA_SOURCE_DEFAULT),
              null);
      HelpFormatter formatter = CommandLineOptions.getInstance().getHelpFormatter();
      formatter.printHelp(
          CommandLineOptions.APPLICATION_NAME + " [OPTIONS]" + System.lineSeparator() + System.lineSeparator(),
          CommandLineOptions.HELP_HEADER_1, availableOptions,
          CommandLineOptions.HELP_FOOTER);
      return null;
    }

    // Some arguments have been supplied, parse only the common arguments for now (so we can identify
    // the data source)
    Options commonOptions = commandLineOptions.getOptionsForFlags(null, null);
    CommandLine commonCmd = commandLineOptions.parse(commonOptions, args, true);

    String dataSourceString =
        commonCmd.getOptionValue(CommandLineOptions.DATA_SOURCE, CommandLineOptions.DATA_SOURCE_DEFAULT);
    DataSourceEnum dataSource = DataSourceEnum.getEnumFromName(dataSourceString);

    // Now consider both common options and data source options
    commonOptions = commandLineOptions.getOptionsForFlags(dataSource, null);
    commonCmd = commandLineOptions.parse(commonOptions, args, true);

    // Concatenate any additional options that originate from the data source
    String[] additionalOptions = dataSource.getAdditionalOptions(commonCmd);
    if (additionalOptions != null) {
      args = Stream.concat(Arrays.stream(args), Arrays.stream(additionalOptions))
          .toArray(String[]::new);
      Logger.log("Info: Additional args added, full argumets are: " + Arrays.toString(args));
      commonCmd = commandLineOptions.parse(commonOptions, args, true);
    }

    // Get players, lineup type, and optimizer from the cmd line flags
    String lineupTypeString =
        commonCmd.getOptionValue(CommandLineOptions.LINEUP_TYPE, CommandLineOptions.TYPE_LINEUP_DEFAULT);
    LineupTypeEnum lineupType = LineupTypeEnum.getEnumFromIdOrName(lineupTypeString);

    String playerString =
        commonCmd.getOptionValue(CommandLineOptions.LINEUP, ""); // TODO: make this required?
    List<String> players = Arrays.asList(playerString.split(","));

    OptimizerEnum optimizer = null;
    if (commonCmd.hasOption(CommandLineOptions.OPTIMIZER)) {
      String optimizerString = commonCmd.getOptionValue(CommandLineOptions.OPTIMIZER);
      optimizer = OptimizerEnum.getEnumFromIdOrName(optimizerString);
    }

    // Help will show the valid flags based on what optimizer and dataSource are supplied.
    Options availableOptions = commandLineOptions.getOptionsForFlags(dataSource, optimizer);
    if (commonCmd.hasOption(CommandLineOptions.HELP)) {
      String helpHeader =
          (optimizer == null) ? CommandLineOptions.HELP_HEADER_1 : CommandLineOptions.HELP_HEADER_2 + optimizer;
      HelpFormatter formatter = CommandLineOptions.getInstance().getHelpFormatter();
      formatter.printHelp(CommandLineOptions.APPLICATION_NAME, helpHeader, availableOptions,
          CommandLineOptions.HELP_FOOTER);
      return null;
    }

    // Require that an optimizer is specified
    if (optimizer == null) {
      throw new RuntimeException(
          Msg.MISSING_OPTIMIZER.args(OptimizerEnum.getValuesAsString()));
    }

    // Parse command line arguments
    CommandLine allCmd = commandLineOptions.parse(availableOptions, args, false);

    // Read stats data from the source
    DataStats stats = dataSource.getData(allCmd);

    // Convert arguments list to map
    Map<String, String> arguments = optimizer.getArgumentsAndValuesAsMap(allCmd);

    // We accept both ids and names for this argument, but the optimizers expects ids only. This
    // resolves
    // any names to ids.
    validatePlayersList(players, stats);
    players = stats.convertPlayersListToIds(players);

    // Check if there is a cached result for a run with the exact same args
    Result existingResult = dataSource.getCachedResult(allCmd, stats);

    ProgressTracker tracker = new ProgressTracker(existingResult, dataSource, allCmd, stats);
    Thread trackerThread = new Thread(tracker);
    trackerThread.start();

    // Finally, call the actual optimizer
    try {
      Result result = optimizer.optimize(players, lineupType, stats, arguments, tracker, existingResult);
      dataSource.onComplete(allCmd, stats, result);
      return result;
    } catch (Exception e) {
      Result errorResult = new Result(tracker.getCurrentResult(), ResultStatusEnum.ERROR, e.getMessage());
      dataSource.onComplete(allCmd, stats, errorResult);
      throw e;
    } finally {
      trackerThread.interrupt();
    }

  }

  /**
   * Validates the players list. Right now this just checks to see if the list is empty or filled with
   * blanks and prints the available options.
   */
  private static void validatePlayersList(List<String> players, DataStats stats) {
    boolean effectivlyBlank = true;
    for (String s : players) {
      if (!StringUtils.isBlank(s)) {
        effectivlyBlank = false;
      }
    }
    if (effectivlyBlank) {
      String playersAsString = stats.getPlayers().stream().map(v -> v.getName() + " - " + v.getId())
          .collect(Collectors.joining(System.lineSeparator()));
      throw new RuntimeException(
          "No players were specified. Please specify a comma separated list of player names or ids using the -"
              + CommandLineOptions.LINEUP + " flag. Avaliable players " + System.lineSeparator()
              + playersAsString);
    }
  }

}
