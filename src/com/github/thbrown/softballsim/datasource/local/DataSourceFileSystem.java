package com.github.thbrown.softballsim.datasource.local;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.Msg;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.datasource.DataSource;
import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;
import com.google.gson.Gson;

public class DataSourceFileSystem implements DataSource {

  // The stats path could very well be used by a human
  public final static String STATS_PATH = "p";

  // Paths mostly intended for during automation by a machine
  public final static String FLAGS_PATH = "x";
  public final static String CACHE_PATH = "z";

  // Default paths (not always applicable)
  public final static String STATS_DATA_FILE_PATH_DEFAULT = "./stats/exampleData.json";
  public final static String CACHED_RESULTS_FILE_PATH = "./cache";
  public final static String CONTROL_FLAGS_FILE_PATH = "./flags";

  private final Gson gson = GsonAccessor.getInstance().getCustom();

  @Override
  public List<Option> getCommandLineOptions() {
    List<Option> options = new ArrayList<>();
    options.add(Option.builder(STATS_PATH).longOpt("stats-path")
        .desc(DataSourceEnum.FILE_SYSTEM
            + ": Read. File (or directory with a single file). Path to the stats file. Default: "
            + STATS_DATA_FILE_PATH_DEFAULT)
        .hasArg(true).required(false).build());
    options.add(Option.builder(CACHE_PATH).longOpt("cache-path").desc(DataSourceEnum.FILE_SYSTEM
        + ": Read/Write. Directory or File. Path where results should be saved as they are computed and where the application can look for cached results before actually running any optimizer. This directory includes both final results for completed optimizations as well as partial results for in-progress or interrupted optimizations.  If the provided path is a directory the application will generate the file name based on a hash of an optimization run's stats file and options. Default: "
        + CACHED_RESULTS_FILE_PATH).hasArg(true).required(false).build());
    options.add(Option.builder(FLAGS_PATH).longOpt("flags-path").desc(DataSourceEnum.FILE_SYSTEM
        + ": Read. Directory or File. Path that is periodically checked by the application, if this file contains the text 'HALT' the application will terminate. If the provided path is a directory the application will generate the file name based on a hash of an optimization run's stats file and options.")
        .hasArg(true).required(false).build());
    return options;
  }

  @Override
  public void onUpdate(CommandLine cmd, DataStats stats, ProgressTracker tracker) {
    DataSource.super.onUpdate(cmd, stats, tracker);

    // Save the most recent result to the file system so we can start the
    // optimization from this point
    // if it gets interrupted, unless we are doing an estimate only run
    Result currentResult = tracker.getCurrentResult();

    String result = gson.toJson(currentResult);
    if (!cmd.hasOption(CommandLineOptions.ESTIMATE_ONLY)) {
      String statsFileLocation = cmd.getOptionValue(CACHE_PATH, CACHED_RESULTS_FILE_PATH);
      String fileName = getFileName(cmd, gson.toJson(stats));
      File cacheFile = getFilePath(statsFileLocation, fileName);
      writeFile(result, cacheFile);
    }
  }

  @Override
  public void onComplete(CommandLine cmd, DataStats stats, Result finalResult) {
    Logger.log(finalResult);
    String resultString = gson.toJson(finalResult);
    String statsFileLocation = cmd.getOptionValue(CACHE_PATH, CACHED_RESULTS_FILE_PATH);
    String fileName = getFileName(cmd, gson.toJson(stats));
    File cacheFile = getFilePath(statsFileLocation, fileName);
    writeFile(resultString, cacheFile);
  }

  private String getFileName(CommandLine allCmd, String data) {
    return getStringsMd5(allCmd, data);
  }

  public static String getStringsMd5(CommandLine args, String data) {
    List<String> argsStringArray = Arrays.stream(args.getOptions()).map(v -> v.getOpt() + v.getValuesList())
        .collect(Collectors.toList());
    Collections.sort(argsStringArray);
    return StringUtils.calculateMd5AsHex(argsStringArray.toString() + "|" + data);
  }

  @Override
  public DataStats getData(CommandLine cmd) {
    String statsFileLocation = cmd.getOptionValue(STATS_PATH, STATS_DATA_FILE_PATH_DEFAULT);
    String json = null;
    File file = new File(statsFileLocation);
    try {
      if (file.isDirectory()) {
        File[] filesInDirectory = file.listFiles();
        List<File> filesOnly = Arrays.stream(filesInDirectory).filter(f -> f.isFile()).collect(Collectors.toList());
        if (filesOnly.size() == 1) {
          json = new String(Files.readAllBytes(Paths.get(filesOnly.get(0).getCanonicalPath())));
        } else {
          throw new RuntimeException("There were " + filesOnly.size() + " files in the stats-path directory specified ("
              + file.getAbsolutePath()
              + "), but this application expects only one, or a path directly to the stats file");
        }
      } else {
        json = new String(Files.readAllBytes(Paths.get(statsFileLocation)));
      }
    } catch (IOException e) {
      throw new RuntimeException(Msg.BAD_PATH.args(statsFileLocation), e);
    }
    return gson.fromJson(json, DataStats.class);
  }

  @Override
  public Result getCachedResult(CommandLine cmd, DataStats stats) {
    String cacheFileLocation = cmd.getOptionValue(CACHE_PATH, CACHED_RESULTS_FILE_PATH);

    String fileName = getFileName(cmd, gson.toJson(stats));
    File cacheFile = getFilePath(cacheFileLocation, fileName);

    if (cacheFile.exists()) {
      Logger.log("Info: Using Cached Result. Run using the -" + CommandLineOptions.FORCE
          + " flag to disregard this cached result.");
      try {
        String data = new String(Files.readAllBytes(Paths.get(cacheFile.getCanonicalPath())));
        return gson.fromJson(data, Result.class);
      } catch (Exception e) {
        Logger.warn("Failed to read/parse cached result file: " + cacheFile.getName() + " because " + e.getMessage()
            + ". Ignoring cached result and running a new simulation.");
      }
    }
    return null;
  }

  @Override
  public String getControlFlag(CommandLine cmd, DataStats stats) {
    String flagsFileLocation = cmd.getOptionValue(FLAGS_PATH, CONTROL_FLAGS_FILE_PATH);
    if (flagsFileLocation == null) {
      return null;
    }

    String fileName = getFileName(cmd, gson.toJson(stats));
    File flagsFile = getFilePath(flagsFileLocation, fileName);

    if (flagsFile.exists()) {
      try {
        return new String(Files.readAllBytes(Paths.get(flagsFile.getCanonicalPath())));
      } catch (Exception e) {
        Logger.warn("Failed to read flag file: " + flagsFile.getName() + " because " + e.getMessage());
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  // TODO: might be good to memoize this somehow
  private File getFilePath(String path, String fileNameIfDirectory) {
    File flagsPath = new File(path);

    // Generate a file name if one was not specified
    if (flagsPath.isDirectory()) {
      return new File(path + File.separatorChar + fileNameIfDirectory);
    }
    return flagsPath;
  }

  private void writeFile(String value, File file) {
    try {
      if (!file.exists()) {
        // Make any parent directories
        File parent = file.getParentFile();
        if (parent != null) {
          parent.mkdirs();
        }
        // Make the file
        file.createNewFile();
      }

      FileWriter fw = new FileWriter(file.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(value);
      bw.close();
    } catch (IOException e) {
      Logger.error("There was a problem writing to " + file.getAbsolutePath() + ". " + e.toString());
    }
  }
}
