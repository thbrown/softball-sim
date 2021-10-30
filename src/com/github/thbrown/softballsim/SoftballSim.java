package com.github.thbrown.softballsim;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
      // When running the application we'll want to make sure we shut down everything
      // at the end, in particular optimizer threads that may still be running
      // (perhaps because of a pause or and error). However, we want library consumers
      // to be able to accept responsibility for ending the program if they so choose,
      // so we'll pass the kill command in as a runnable to be invoked after the
      // main thread finishes executing.
      mainInternal(args, () -> {
        System.exit(0);
      });
    } catch (Exception e) {
      Logger.error(e.getMessage());
      e.printStackTrace();
    }
  }

  // Currently just a convenience method for tests :(
  public static Result mainInternal(String[] args) throws MissingArgumentException {
    return mainInternal(args, null);
  }

  public static Result mainInternal(String[] args, Runnable cleanup) throws MissingArgumentException {
    // The valid command line flags change based on which optimizer and data source
    // are supplied.
    CommandLineOptions commandLineOptions = CommandLineOptions.getInstance();

    // If there were no arguments supplied, show help with only the common options
    // (i.e. no additional
    // flags for optimizer, and the default flags for data source)
    if (args.length == 0) {
      Options availableOptions = commandLineOptions
          .getOptionsForFlags(DataSourceEnum.getEnumFromName(CommandLineOptions.DATA_SOURCE_DEFAULT), null);
      HelpFormatter formatter = CommandLineOptions.getInstance().getHelpFormatter();
      formatter.printHelp(
          CommandLineOptions.APPLICATION_NAME + " [OPTIONS]" + System.lineSeparator()
              + System.lineSeparator(),
          CommandLineOptions.HELP_HEADER_1, availableOptions, CommandLineOptions.HELP_FOOTER);
      return null;
    }

    // Some arguments have been supplied, parse only the common arguments for now
    // (so we can identify
    // the data source)
    Options commonOptions = commandLineOptions.getOptionsForFlags(null, null);
    CommandLine commonCmd = commandLineOptions.parse(commonOptions, args, true);

    String dataSourceString = commonCmd.getOptionValue(CommandLineOptions.DATA_SOURCE,
        CommandLineOptions.DATA_SOURCE_DEFAULT);
    DataSourceEnum dataSource = DataSourceEnum.getEnumFromName(dataSourceString);

    // Now consider both common options and data source options
    commonOptions = commandLineOptions.getOptionsForFlags(dataSource, null);
    commonCmd = commandLineOptions.parse(commonOptions, args, true);

    // Get players, lineup type, and optimizer from the cmd line flags
    String lineupTypeString = commonCmd.getOptionValue(CommandLineOptions.LINEUP_TYPE,
        CommandLineOptions.TYPE_LINEUP_DEFAULT);
    LineupTypeEnum lineupType = LineupTypeEnum.getEnumFromIdOrName(lineupTypeString);

    String playerString = commonCmd.getOptionValue(CommandLineOptions.LINEUP, ""); // TODO: make this required?
    List<String> players = Arrays.asList(playerString.split(","));

    final OptimizerEnum optimizer;
    if (commonCmd.hasOption(CommandLineOptions.OPTIMIZER)) {
      String optimizerString = commonCmd.getOptionValue(CommandLineOptions.OPTIMIZER);
      optimizer = OptimizerEnum.getEnumFromIdOrName(optimizerString);
    } else {
      optimizer = null;
    }

    // Help will show the valid flags based on what optimizer and dataSource are
    // supplied.
    Options availableOptions = commandLineOptions.getOptionsForFlags(dataSource, optimizer);
    if (commonCmd.hasOption(CommandLineOptions.HELP)) {
      String helpHeader = (optimizer == null) ? CommandLineOptions.HELP_HEADER_1
          : CommandLineOptions.HELP_HEADER_2 + optimizer;
      HelpFormatter formatter = CommandLineOptions.getInstance().getHelpFormatter();
      formatter.printHelp(CommandLineOptions.APPLICATION_NAME, helpHeader, availableOptions,
          CommandLineOptions.HELP_FOOTER);
      return null;
    }

    // Require that an optimizer is specified
    if (optimizer == null) {
      throw new RuntimeException(Msg.MISSING_OPTIMIZER.args(OptimizerEnum.getValuesAsString()));
    }

    // Parse command line arguments
    CommandLine allCmd = commandLineOptions.parse(availableOptions, args, false);

    // Read stats data from the source
    DataStats stats = dataSource.getData(allCmd);

    // Convert arguments list to map
    Map<String, String> arguments = optimizer.getArgumentsAndValuesAsMap(allCmd);

    // We accept both ids and names for this argument, but the optimizers expects
    // ids only. This resolves any names to ids.
    validatePlayersList(players, stats);
    final List<String> playersIdsOnly = stats.convertPlayersListToIds(players);

    // Check if there is a cached result for a run with the exact same args
    final Result existingResult;
    if (!allCmd.hasOption(CommandLineOptions.FORCE)) {
      existingResult = dataSource.getCachedResult(allCmd, stats);
    } else {
      existingResult = null;
    }

    // Prep the progress tracker class so we can pass a reference to the optimizer thread
    Thread mainThread = Thread.currentThread();
    ProgressTracker tracker = new ProgressTracker(existingResult, dataSource, allCmd, stats, optimizer);

    // Start the optimization in its own thread
    final boolean verboseFlagEnabled = commonCmd.hasOption(CommandLineOptions.VERBOSE);
    Thread optimizerThread = new Thread(() -> {
      try {
        Result result = optimizer.optimize(playersIdsOnly, lineupType, stats, arguments, tracker,
            existingResult);
        result = result.copyWithNewEstimatedTimeRemainingMs(0L); // Set estimated completion time to zero
        tracker.updateProgress(result); // Last update
        dataSource.onComplete(allCmd, stats, result);
      } catch (Exception e) {
        Result errorResult = tracker.getCurrentResult().copyWithNewStatus(ResultStatusEnum.ERROR, e.getMessage());
        tracker.updateProgress(errorResult); // Last update
        dataSource.onComplete(allCmd, stats, errorResult);
        if (verboseFlagEnabled) {
          Logger.log(e);
        }
      } finally {
        mainThread.interrupt();
      }
    });
    optimizerThread.start();

    // Finally, start the progress tracker on the main thread
    Result finalResult = tracker.run();

    // Almost done, just run the cleanup procedure supplied on invocation (if any)
    if (cleanup != null) {
      cleanup.run();
    }
    return finalResult;
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
              + CommandLineOptions.LINEUP + " flag. Available players " + System.lineSeparator()
              + playersAsString);
    }
  }

}
