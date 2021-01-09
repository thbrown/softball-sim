package com.github.thbrown.softballsim.optimizer.gson;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines which subclass OptimizerDefinitionInput elements should be deserialised into
 */
public enum OptimizerDefinitionOptionsEnum {
  BOOLEAN("Boolean", OptimizerDefinitionOptionBoolean.class),
  ENUMERATION("Enumeration", OptimizerDefinitionOptionEnumeration.class),
  NUMBER("Number", OptimizerDefinitionOptionNumber.class),
  STRING("String", OptimizerDefinitionOptionString.class);

  private String jsonType;
  private Class<? extends OptimizerDefinitionOption> deserializationTarget;

  OptimizerDefinitionOptionsEnum(String jsonType, Class<? extends OptimizerDefinitionOption> deserializationTarget) {
    this.jsonType = jsonType;
    this.deserializationTarget = deserializationTarget;
  }

  public Class<? extends OptimizerDefinitionOption> getDeserializationTarget() {
    return deserializationTarget;
  }

  public String getJsonType() {
    return jsonType;
  }

  private static final Map<String, OptimizerDefinitionOptionsEnum> ENUM_TYPE_MAP;
  static {
    Map<String, OptimizerDefinitionOptionsEnum> nameMap = new HashMap<>();
    for (OptimizerDefinitionOptionsEnum instance : OptimizerDefinitionOptionsEnum.values()) {
      nameMap.put(instance.getJsonType(), instance);
    }
    ENUM_TYPE_MAP = Collections.unmodifiableMap(nameMap);
  }

  public static OptimizerDefinitionOptionsEnum getEnumByJsonType(String jsonType) {
    OptimizerDefinitionOptionsEnum enumValue = ENUM_TYPE_MAP.get(jsonType);
    if (enumValue == null) {
      throw new RuntimeException("Unrecognized optimizer option type: " + jsonType + " choices are "
          + Arrays.asList(OptimizerDefinitionOptionsEnum.values()));
    }
    return enumValue;
  }

}
