package com.github.thbrown.softballsim.optimizer.gson;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public abstract class OptimizerDefinitionOption {

  private String shortLabel;
  private String longLabel;
  private String description;
  private String type;

  public String getShortLabel() {
    return shortLabel;
  }

  public String getLongLabel() {
    return longLabel;
  }

  public String getDescription() {
    return description;
  }

  public String getType() {
    return type;
  }

  public abstract Option getCommandLineOption();

  public abstract String getKey();

  public abstract String getValue(CommandLine cmd);
}
