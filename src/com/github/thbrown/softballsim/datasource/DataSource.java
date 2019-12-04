package com.github.thbrown.softballsim.datasource;

import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public interface DataSource {

  public List<Option> getCommandLineOptions();

  public void execute(CommandLine allCmd);

}
