package com.github.thbrown.softballsim.optimizer.gson;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public class OptimizerDefinitionInputString extends OptimizerDefinitionArgument {
  private String defaultValue;
  private String pattern;

  public Option getCommandLineOption() {
    return Option.builder(super.getShortLabel())
        .longOpt(super.getLongLabel())
        .desc(super.getDescription())
        .hasArg(true)
        .required(false)
        .build();
  }

  @Override
  public String getKey() {
    return super.getLongLabel();
  }

  @Override
  public String getValue(CommandLine cmd) {
    String value = cmd.getOptionValue(super.getShortLabel(), defaultValue);
    if (pattern == null || value.matches(pattern)) {
      return value;
    }
    throw new RuntimeException("The argument " + value + " did not validate again the regex pattern " + pattern);
  }

}
