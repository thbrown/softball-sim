package com.github.thbrown.softballsim.optimizer.gson;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines which subclass OptimizerDefinitionInput elements should be deserialised into
 */
public enum OptimizerDefinitionInputEnum {
  BOOLEAN("Boolean", OptimizerDefinitionInputBoolean.class),
  STRING("String", OptimizerDefinitionInputString.class);

  private String jsonType;
  private Class<? extends OptimizerDefinitionArgument> deserializationTarget;

  OptimizerDefinitionInputEnum(String jsonType, Class<? extends OptimizerDefinitionArgument> deserializationTarget) {
    this.jsonType = jsonType;
    this.deserializationTarget = deserializationTarget;
  }

  public Class<? extends OptimizerDefinitionArgument> getDeserializationTarget() {
    return deserializationTarget;
  }

  public String getJsonType() {
    return jsonType;
  }

  private static final Map<String, OptimizerDefinitionInputEnum> ENUM_TYPE_MAP;
  static {
    Map<String, OptimizerDefinitionInputEnum> nameMap = new HashMap<>();
    for (OptimizerDefinitionInputEnum instance : OptimizerDefinitionInputEnum.values()) {
      nameMap.put(instance.getJsonType(), instance);
    }
    ENUM_TYPE_MAP = Collections.unmodifiableMap(nameMap);
  }

  public static OptimizerDefinitionInputEnum getEnumByJsonType(String jsonType) {
    return ENUM_TYPE_MAP.get(jsonType);
  }

}
