package com.github.thbrown.softballsim;

import com.github.thbrown.softballsim.gson.BaseOptimizationData;
import com.github.thbrown.softballsim.gson.MonteCarloExaustiveData;

public enum OptimizationTypeEnum {
  MONTE_CARLO_EXAUSTIVE(0, MonteCarloExaustiveData.class);
  
  private final int apiValue;
  private Class<? extends BaseOptimizationData> deserializationTarget;
  
  OptimizationTypeEnum(int apiValue, Class<? extends BaseOptimizationData> deserializationTarget) {
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

  public Class<? extends BaseOptimizationData> getDeserializationTarget() {
    return this.deserializationTarget;
  }
  
}
