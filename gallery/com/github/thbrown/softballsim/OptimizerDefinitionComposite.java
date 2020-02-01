package com.github.thbrown.softballsim;

import com.github.thbrown.softballsim.optimizer.gson.OptimizerDefinition;

/**
 * POJO so we can keep a list that contains both the OptimizerDefinition and it's file name;
 */
public class OptimizerDefinitionComposite {
  private OptimizerDefinition definition;
  private String fileName;

  public OptimizerDefinitionComposite(OptimizerDefinition definition, String fileName) {
    this.definition = definition;
    this.fileName = fileName;
  }

  public OptimizerDefinition getDefinition() {
    return definition;
  }

  public String getFileName() {
    return fileName;
  }
}
