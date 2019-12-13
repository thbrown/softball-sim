package com.github.thbrown.softballsim.optimizer.gson;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines which subclass OptimizerDefinitionInput elements should be deserialised into
 */
public enum OptimizerDefinitionArgumentEnum {
  BOOLEAN("Boolean", OptimizerDefinitionArgumentBoolean.class),
  STRING("String", OptimizerDefinitionArgumentString.class);

  private String jsonType;
  private Class<? extends OptimizerDefinitionArgument> deserializationTarget;

  OptimizerDefinitionArgumentEnum(String jsonType, Class<? extends OptimizerDefinitionArgument> deserializationTarget) {
    this.jsonType = jsonType;
    this.deserializationTarget = deserializationTarget;
  }

  public Class<? extends OptimizerDefinitionArgument> getDeserializationTarget() {
    return deserializationTarget;
  }

  public String getJsonType() {
    return jsonType;
  }

  private static final Map<String, OptimizerDefinitionArgumentEnum> ENUM_TYPE_MAP;
  static {
    Map<String, OptimizerDefinitionArgumentEnum> nameMap = new HashMap<>();
    for (OptimizerDefinitionArgumentEnum instance : OptimizerDefinitionArgumentEnum.values()) {
      nameMap.put(instance.getJsonType(), instance);
    }
    ENUM_TYPE_MAP = Collections.unmodifiableMap(nameMap);
  }

  public static OptimizerDefinitionArgumentEnum getEnumByJsonType(String jsonType) {
    return ENUM_TYPE_MAP.get(jsonType);
  }

}
