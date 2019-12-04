package com.github.thbrown.softballsim.datasource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.data.gson.DataStatsDeserializer;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.Logger;
import com.google.gson.GsonBuilder;

public class DataSourceFileSystem implements DataSource {

  // DataSource - FILE_SYSTEM
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
  public void execute(CommandLine allCmd) {
    // Read stats data from file
    String statsFileLocation = allCmd.getOptionValue(FILE_PATH, FILE_PATH_DEFAULT);
    GsonBuilder gsonBldr = new GsonBuilder();
    gsonBldr.registerTypeAdapter(DataStats.class, new DataStatsDeserializer());
    String json;
    try {
      json = new String(Files.readAllBytes(Paths.get(statsFileLocation)));
    } catch (IOException e) {
      throw new RuntimeException("Unable to read the stats file from " + statsFileLocation, e);
    }
    DataStats stats = gsonBldr.create().fromJson(json, DataStats.class);

    // Get players, optimizer, and lineup type from the cmd line flags
    String optimizerString = allCmd.getOptionValue(CommandLineOptions.OPTIMIZER);// Required, no default needed
    OptimizerEnum optimizer = OptimizerEnum.getEnumFromIdOrName(optimizerString);

    String lineupTypeString =
        allCmd.getOptionValue(CommandLineOptions.LINEUP_TYPE, CommandLineOptions.TYPE_LINEUP_DEFAULT);
    LineupTypeEnum lineupType = LineupTypeEnum.getEnumFromIdOrName(lineupTypeString);

    String playerString =
        allCmd.getOptionValue(CommandLineOptions.PLAYERS_IN_LINEUP, stats.getPlayersAsCommaSeparatedString());
    List<String> players = Arrays.asList(playerString.split(",")); // TODO: should we accept names here too if they are
                                                                   // unique?

    Map<String, String> arguments = optimizer.getArgumentsAndValuesAsMap(allCmd);

    ProgressTracker tracker = new LocalProgressTracker(null); // TODO: save optimizer run results to file system
    Thread trackerThread = new Thread(tracker);
    trackerThread.start();
    Result result = optimizer.optimize(players, lineupType, stats, arguments, tracker, null);
    trackerThread.interrupt();
    Logger.log(result.toString());
  }

}
