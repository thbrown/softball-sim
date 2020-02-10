package com.github.thbrown.softballsim.datasource.local;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.Msg;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.data.gson.DataStatsDeserializer;
import com.github.thbrown.softballsim.datasource.DataSource;
import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.datasource.DataSourceFunctions;
import com.github.thbrown.softballsim.datasource.EstimateOnlyExecutionWrapper;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.StringUtils;
import com.google.gson.GsonBuilder;

public class DataSourceFileSystem implements DataSource {

  public final static String FILE_PATH = "F";

  public final static String FILE_PATH_DEFAULT = "./stats/exampleData.json";

  @Override
  public List<Option> getCommandLineOptions() {
    List<Option> options = new ArrayList<>();
    options.add(Option.builder(FILE_PATH)
        .longOpt("File-path")
        .desc(DataSourceEnum.FILE_SYSTEM + ": Path to the stats files. This can be a directory or file. Default: "
            + FILE_PATH_DEFAULT)
        .hasArg(true)
        .required(false)
        .build());
    return options;
  }

  @Override
  public void execute(String[] args, LineupTypeEnum lineupType, List<String> players, OptimizerEnum optimizer) {
    // Parse command line arguments
    CommandLineOptions commandLineOptions = CommandLineOptions.getInstance();
    Options allOptions = commandLineOptions.getOptionsForFlags(DataSourceEnum.FILE_SYSTEM, optimizer);
    CommandLine allCmd = commandLineOptions.parse(allOptions, args, false);

    // Read stats data from file
    String statsFileLocation = allCmd.getOptionValue(FILE_PATH, FILE_PATH_DEFAULT);
    String json;
    try {
      json = new String(Files.readAllBytes(Paths.get(statsFileLocation)));
    } catch (IOException e) {
      throw new RuntimeException(Msg.BAD_STATS_FILE_PATH.args(statsFileLocation), e);
    }
    DataStats stats = GsonAccessor.getInstance().getCustom().fromJson(json, DataStats.class);

    // Convert arguments list to map
    Map<String, String> arguments = optimizer.getArgumentsAndValuesAsMap(allCmd);

    // We accept both ids and names for this argument, but the optimizers expect only ids. This resolves
    // any names to ids.
    validatePlayersList(players, stats);
    players = stats.convertPlayersListToIds(players);

    // TODO: save optimizer run results to file system
    DataSourceFunctions functions = new DataSourceFunctionsFileSystem();
    ProgressTracker tracker = new ProgressTracker(new Result(null, 0, 0, 0, 0), functions);
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

  /**
   * Validates the players list. Right now this just checks to see if the list is empty or filled with
   * blanks and prints the available options.
   */
  private void validatePlayersList(List<String> players, DataStats stats) {
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
              + CommandLineOptions.PLAYERS_IN_LINEUP + " flag. Avaliable players " + System.lineSeparator()
              + playersAsString);
    }

  }

}