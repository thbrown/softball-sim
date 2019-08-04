package com.github.thbrown.softballsim;

import com.github.thbrown.softballsim.gson.BaseOptimizationDefinition;
import com.github.thbrown.softballsim.gson.MonteCarloExaustiveOptimizatonDefinition;

public enum OptimizationTypeEnum {
  MONTE_CARLO_EXAUSTIVE(0, MonteCarloExaustiveOptimizatonDefinition.class);
  
  private final int apiValue;
  private Class<? extends BaseOptimizationDefinition> deserializationTarget;
  
  OptimizationTypeEnum(int apiValue, Class<? extends BaseOptimizationDefinition> deserializationTarget) {
    this.apiValue = apiValue;
    this.deserializationTarget = deserializationTarget;
  }
  
  public static OptimizationTypeEnum getEnumFromApiValue(int apiValue) {
    for(OptimizationTypeEnum v : values()){
      if( v.getApiValue() == apiValue){
          return v;
      }
    }
    throw new RuntimeException("Invalid api value specified: " + apiValue);
  }
  
  public int getApiValue() {
    return apiValue;
  }

  public Class<? extends BaseOptimizationDefinition> getDeserializationTarget() {
    return this.deserializationTarget;
  }
  
}
