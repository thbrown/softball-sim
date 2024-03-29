package com.github.thbrown.softballsim.optimizer.gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public class OptimizerDefinition {
  private String id;
  private String name;
  private String shortDescription;
  private String longDescriptionFile;
  private String img;
  private List<String> supportedLineupTypes;
  private List<OptimizerDefinitionOption> options;
  private VisibilityEnum uiVisibility;
  private boolean pausable;

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public String getLongDescriptionFile() {
    return longDescriptionFile;
  }

  public String getImageUrl() {
    return img;
  }

  public List<String> getSupportedLineupTypes() {
    return supportedLineupTypes;
  }

  public List<OptimizerDefinitionOption> getOptions() {
    return options;
  }

  public VisibilityEnum getUiVisibility() {
    return uiVisibility;
  }

  public boolean isPausable() {
    return pausable;
  }

  /**
   * @return a map that contains keys for all arguments for this optimization in the format longLabel
   *         -> value. The value is extracted from the CommandLine instance passed in as the argument
   *         to this method.
   */
  public Map<String, String> getOptionsAndValuesAsMap(CommandLine cmd) {
    Map<String, String> data = new HashMap<>();
    for (OptimizerDefinitionOption option : options) {
      String key = option.getShortLabel();
      String value = option.getValue(cmd);
      data.put(key, value);
    }
    return data;
  }

  /**
   * @return command line options that should be available for this optimizer
   */
  public List<Option> getOptionsAsCommandLineOptions() {
    List<Option> outputOptions = new ArrayList<>();
    for (OptimizerDefinitionOption option : this.options) {
      outputOptions.add(option.getCommandLineOption());
    }
    return outputOptions;
  }
}
