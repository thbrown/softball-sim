package com.github.thbrown.softballsim.helpers;

import com.google.gson.Gson;

abstract class SimulationDataBuilder {

  // All fields here should be marked transient so that they don't appear in the
  // optimizationData JSON
  private transient String optimizationType = "0";

  public SimulationDataBuilder withOptimizationType(int optimizationType) {
    this.optimizationType = String.valueOf(optimizationType);
    return this;
  }

  public final String toString() {
    // Not using gson here :(
    return "{\"optimizationType\":" + optimizationType + ",\"optimizationData\":" + getOptimizationData() + "}";
  }

  // OptimizationData is the serialized class
  public String getOptimizationData() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }

}
