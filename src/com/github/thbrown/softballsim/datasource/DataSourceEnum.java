package com.github.thbrown.softballsim.datasource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.cli.Option;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.datasource.local.DataSourceFileSystem;
import com.github.thbrown.softballsim.datasource.network.DataSourceNetwork;
import com.github.thbrown.softballsim.datasource.gcpfunctions.DataSourceGcpFunctions;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.StringUtils;

/**
 * Defines where the application gets its stats data to feed to an optimizer and what it does with
 * optimization results
 * 
 * TODO: Rename IOMode?
 */
public enum DataSourceEnum {
  FILE_SYSTEM(new DataSourceFileSystem()),
  NETWORK(new DataSourceNetwork()),
  GCP_FUNCTIONS(new DataSourceGcpFunctions());

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

  public Result execute(String[] args, LineupTypeEnum lineupType, List<String> players, OptimizerEnum optimizer) {
    return dataSource.execute(args, lineupType, players, optimizer);
  }
}
