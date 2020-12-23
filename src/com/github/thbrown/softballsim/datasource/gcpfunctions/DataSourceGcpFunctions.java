package com.github.thbrown.softballsim.datasource.gcpfunctions;

import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.datasource.DataSourceFunctions;
import com.github.thbrown.softballsim.datasource.local.DataSourceFileSystem;

public class DataSourceGcpFunctions extends DataSourceFileSystem {

  public final static String ID = "I";

  @Override
  protected DataSourceEnum getDataSourceEnum() {
    return DataSourceEnum.GCP_FUNCTIONS;
  }

  @Override
  protected String getFileName(CommandLine allCmd) {
    return allCmd.getOptionValue(ID);
  }

  @Override
  protected DataSourceFunctions getFunctions(String fileName) {
    return new DataSourceFunctionsGcpFunctions(fileName);
  }

  @Override
  public List<Option> getCommandLineOptions() {
    List<Option> options = super.getCommandLineOptions();
    options.add(Option.builder(ID)
        .longOpt("Id")
        .desc(DataSourceEnum.FILE_SYSTEM
            + ": Required. An arbitrary id associted with this request. The same id can be used to query for intermediate results.")
        .hasArg(true)
        .required(false)
        .build());
    return options;
  }

}
