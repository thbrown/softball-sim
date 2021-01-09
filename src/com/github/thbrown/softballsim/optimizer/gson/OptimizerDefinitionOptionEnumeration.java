package com.github.thbrown.softballsim.optimizer.gson;

import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public class OptimizerDefinitionOptionEnumeration extends OptimizerDefinitionOption {

  private String defaultValue;
  private Set<String> values;

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
    if (value == null) {
      return value;
    }

    if (values.contains(value)) {
      throw new RuntimeException("The value provided for argurment " + super.getLongLabel() + " is " + value
          + " which not in the list of allowed values " + values);
    }

    return value;
  }

}
