package com.github.thbrown.softballsim.datasource.local;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.github.thbrown.softballsim.datasource.DataSource;
import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.datasource.DataSourceFunctions;
import com.github.thbrown.softballsim.datasource.EstimateOnlyExecutionWrapper;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;

public class DataSourceFileSystem implements DataSource {

  public final static String PATH = "P";

  public final static String FILE_PATH_DEFAULT = "./stats";
  public final static String CACHED_RESULTS_FILE_PATH = "./cached";

  @Override
  public List<Option> getCommandLineOptions() {
    List<Option> options = new ArrayList<>();
    options.add(Option.builder(PATH)
        .longOpt("Path")
        .desc(DataSourceEnum.FILE_SYSTEM + ": Path to the stats files. This can be a directory or file. Default: "
            + FILE_PATH_DEFAULT)
        .hasArg(true)
        .required(false)
        .build());
    return options;
  }

  @Override
  public Result execute(String[] args, LineupTypeEnum lineupType, List<String> players, OptimizerEnum optimizer) {
    // Require an optimizer
    if (optimizer == null) {
      throw new RuntimeException(
          Msg.MISSING_OPTIMIZER.args(OptimizerEnum.getValuesAsString()));
    }

    // Parse command line arguments
    CommandLineOptions commandLineOptions = CommandLineOptions.getInstance();
    Options allOptions = commandLineOptions.getOptionsForFlags(DataSourceEnum.FILE_SYSTEM, optimizer);
    CommandLine allCmd = commandLineOptions.parse(allOptions, args, false);

    // Read stats data from file
    String statsFileLocation = allCmd.getOptionValue(PATH, FILE_PATH_DEFAULT);
    String json = null;
    File file = new File(statsFileLocation);
    try {
      if (file.isDirectory()) {
        File[] filesInDirectory = file.listFiles();
        List<File> filesOnly = Arrays.stream(filesInDirectory).filter(f -> f.isFile()).collect(Collectors.toList());
        if (filesOnly.size() == 1) {
          json = new String(Files.readAllBytes(Paths.get(filesOnly.get(0).getCanonicalPath())));
        } else {
          throw new RuntimeException(
              "There were " + filesOnly.size() + " files in the directory specified, but this application expects one");
        }
      } else {
        json = new String(Files.readAllBytes(Paths.get(statsFileLocation)));
      }
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

    // Check if there is a cached result for a run with the exact same args
    Result existingResult = null;

    String fileName = getArgsMd5(allCmd);
    File cacheFile = new File(CACHED_RESULTS_FILE_PATH + File.separatorChar + fileName);
    if (cacheFile.exists() && !allCmd.hasOption(CommandLineOptions.FORCE)) {
      Logger.log(
          "Using Cached Result. Run using the -" + CommandLineOptions.FORCE + " flag to disregard this cached result.");
      String data = null;
      try {
        data = new String(Files.readAllBytes(Paths.get(cacheFile.getCanonicalPath())));
        existingResult = GsonAccessor.getInstance().getCustom().fromJson(data, Result.class);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    DataSourceFunctions functions = new DataSourceFunctionsFileSystem(fileName);
    ProgressTracker tracker = new ProgressTracker(existingResult, functions);
    Thread trackerThread = new Thread(tracker);
    trackerThread.start();

    try {
      if (allCmd.hasOption(CommandLineOptions.ESTIMATE_ONLY)) {
        // This will terminate the application
        EstimateOnlyExecutionWrapper wrapper = new EstimateOnlyExecutionWrapper(optimizer, functions);
        wrapper.optimize(players, lineupType, stats, arguments, tracker, existingResult);
        return null; // Doesn't matter
      } else {
        Result result = optimizer.optimize(players, lineupType, stats, arguments, tracker, existingResult);
        functions.onComplete(result);
        return result;
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
              + CommandLineOptions.LINEUP + " flag. Avaliable players " + System.lineSeparator()
              + playersAsString);
    }

  }

  public static String getArgsMd5(CommandLine args) {
    List<String> argsStringArray =
        Arrays.stream(args.getOptions()).map(v -> v.getOpt() + v.getValuesList()).collect(Collectors.toList());
    Collections.sort(argsStringArray);
    return StringUtils.calculateMd5AsHex(argsStringArray.toString());
  }

}
