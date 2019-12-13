package com.github.thbrown.softballsim.optimizer.gson;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public class OptimizerDefinitionArgumentBoolean extends OptimizerDefinitionArgument {

  private static String TRUE = "true";
  private static String FALSE = "false";

  @Override
  public Option getCommandLineOption() {
    return Option.builder(super.getShortLabel())
        .longOpt(super.getLongLabel())
        .desc(super.getDescription())
        .hasArg(false)
        .required(false)
        .build();
  }

  @Override
  public String getKey() {
    return super.getLongLabel();
  }

  @Override
  public String getValue(CommandLine cmd) {
    return cmd.hasOption(super.getShortLabel()) ? TRUE : FALSE;
  }

}
