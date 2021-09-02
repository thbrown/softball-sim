package com.github.thbrown.softballsim.datasource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.datasource.gcpfunctions.DataSourceGcpBuckets;
import com.github.thbrown.softballsim.datasource.local.DataSourceFileSystem;
import com.github.thbrown.softballsim.util.StringUtils;

public enum DataSourceEnum implements DataSource {
  FILE_SYSTEM(new DataSourceFileSystem()),
  GCP_BUCKETS(new DataSourceGcpBuckets());

  private final DataSource dataSource;

  private DataSourceEnum(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * Gets the enum with the given name.
   * 
   * @throws IllegalArgumentException indicating available options if the name does not correspond to
   *         a enum value.
   */
  public static DataSourceEnum getEnumFromName(String name) {
    try {
      StringUtils.trim(name);
      return DataSourceEnum.valueOf(name);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException(
          "Invalid Data Source Provided: " + name + ". Valid options are " + getValuesAsString(), e);
    }
  }

  public static String getValuesAsString() {
    List<String> valuesString =
        Arrays.stream(DataSourceEnum.values()).map(v -> v.toString()).collect(Collectors.toList());
    return "[" + String.join(", ", valuesString) + "]";
  }

  public List<Option> getCommandLineOptions() {
    return dataSource.getCommandLineOptions();
  }

  public Result getCachedResult(CommandLine cmd, DataStats stats) {
    return dataSource.getCachedResult(cmd, stats);
  }

  @Override
  public DataStats getData(CommandLine cmd) {
    return dataSource.getData(cmd);
  }

  @Override
  public String[] getAdditionalOptions(CommandLine cmd) {
    return dataSource.getAdditionalOptions(cmd);
  }

  @Override
  public String getControlFlag(CommandLine cmd, DataStats stats) {
    return dataSource.getControlFlag(cmd, stats);
  }

  @Override
  public void onUpdate(CommandLine cmd, DataStats stats, ProgressTracker tracker) {
    dataSource.onUpdate(cmd, stats, tracker);
  }

  @Override
  public void onComplete(CommandLine cmd, DataStats stats, Result finalResult) {
    dataSource.onComplete(cmd, stats, finalResult);
  }
}
