package com.github.thbrown.softballsim.lineupgen;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.thbrown.softballsim.OptimizerEnum;
import com.github.thbrown.softballsim.util.StringUtils;

public enum LineupTypeEnum implements LineupGeneratorFactory {

  // Register batting lineup generators here
  ORDINARY(1, () -> new OrdinaryBattingLineupGenerator()),
  ALTERNATING_GENDER(2, () -> new AlternatingBattingLineupGenerator()),
  NO_CONSECUTIVE_FEMALES(3, () -> new NoConsecutiveFemalesLineupGenerator());
	
  private static final Map<String, LineupTypeEnum> ENUM_NAME_MAP;
  private static final Map<String, LineupTypeEnum> ENUM_ID_MAP;

  static {
	Map<String, LineupTypeEnum> nameMap = new HashMap<>();
	Map<String, LineupTypeEnum> idMap = new HashMap<>();
	for (LineupTypeEnum instance : LineupTypeEnum.values()) {
		nameMap.put(instance.name(), instance);
		idMap.put(String.valueOf(instance.getId()), instance);
	}
	ENUM_NAME_MAP = Collections.unmodifiableMap(nameMap);
	ENUM_ID_MAP = Collections.unmodifiableMap(idMap);
  }

  final LineupGeneratorFactory lineupGeneratorFactory;
  final int id;

  private LineupTypeEnum(int id, LineupGeneratorFactory lineupGeneratorFactory) {
	this.id = id;
    this.lineupGeneratorFactory = lineupGeneratorFactory;
  }

  private int getId() {
	return this.id;
  }

  @Override
  public LineupGenerator getLineupGenerator() {
    return lineupGeneratorFactory.getLineupGenerator();
  }
  
  /**
   * Gets the enum that corresponds with the given id. Returns null if there is no corresponding enum for that id.
   */
  public static LineupTypeEnum getEnumFromId(int id) {
	return getEnumFromId(String.valueOf(id));
  }
  
  private static LineupTypeEnum getEnumFromId(String id) {
	return ENUM_ID_MAP.get(id);
  }
  
  /**
   * Gets the enum with the given name. Returns null of there is no enum with that name.
   */
  public static LineupTypeEnum getEnumFromName(String name) {
  	return ENUM_NAME_MAP.get(name);
  }

  /**
   * Get the enum by it's name or id.
   * @throws an IllegalArgumentException if there are no enum values corresponding to the identifier.
   * @throws a RuntimeException if the return value is ambiguous because the identifier used as a name 
   * name and as an id correspond to different enum values.
   */
  public static LineupTypeEnum getEnumFromIdOrName(String identifier) {
	StringUtils.trim(identifier);
	LineupTypeEnum a = getEnumFromName(identifier);
	LineupTypeEnum b = getEnumFromId(identifier);
	if (a == null && b == null) {
		throw new IllegalArgumentException("Invalid Lineup Type Provided: " + identifier + ". Valid options are " + getValuesAsString());
	}
	if (a == null || b == null || a == b) {
		return a == null ? b : a;
	}
	throw new RuntimeException("Ambiguous result for identifier " + identifier + ". This could be " + a + " or " + b);
  }	
  
  public static String getValuesAsString() {
	List<String> valuesString = Arrays.stream(OptimizerEnum.values()).map(v -> String.join(" ",v.toString(),"- " + v.getId())).collect(Collectors.toList());
	return "[" + String.join(", ", valuesString) + "]";
  }
}
