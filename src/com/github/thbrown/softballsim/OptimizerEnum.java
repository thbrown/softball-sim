package com.github.thbrown.softballsim;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.thbrown.softballsim.gson.BaseOptimizationDefinition;
import com.github.thbrown.softballsim.gson.MonteCarloExhaustiveOptimizatonDefinition;
import com.github.thbrown.softballsim.util.StringUtils;

/**
 * An enumeration of all the optimizer implementations.
 *  
 * This enum is used to register those implementations with the application.
 */
public enum OptimizerEnum {
	MONTE_CARLO_EXHAUSTIVE(0, MonteCarloExhaustiveOptimizatonDefinition.class);

	private static final Map<String, OptimizerEnum> ENUM_NAME_MAP;
	private static final Map<String, OptimizerEnum> ENUM_ID_MAP;

	static {
		Map<String, OptimizerEnum> nameMap = new HashMap<>();
		Map<String, OptimizerEnum> idMap = new HashMap<>();
		for (OptimizerEnum instance : OptimizerEnum.values()) {
			nameMap.put(instance.name(), instance);
			idMap.put(String.valueOf(instance.getId()), instance);
		}
		ENUM_NAME_MAP = Collections.unmodifiableMap(nameMap);
		ENUM_ID_MAP = Collections.unmodifiableMap(idMap);
	}

	private final int id;
	private Class<? extends BaseOptimizationDefinition> deserializationTarget;

	OptimizerEnum(int apiValue, Class<? extends BaseOptimizationDefinition> deserializationTarget) {
		this.id = apiValue;
		this.deserializationTarget = deserializationTarget;
	}

	public int getId() {
		return id;
	}

	public Class<? extends BaseOptimizationDefinition> getDeserializationTarget() {
		return this.deserializationTarget;
	}
	
	/**
	 * Gets the enum with the given name. Returns null of there is no enum with that name.
	 */
	public static OptimizerEnum getEnumFromName(String name) {
		return ENUM_NAME_MAP.get(name);
	}

	/**
	 * Gets the enum that corresponds with the given id. Returns null if there is no corresponding enum for that id.
	 */
	public static OptimizerEnum getEnumFromId(int id) {
		return getEnumFromId(String.valueOf(id));
	}

	private static OptimizerEnum getEnumFromId(String id) {
		return ENUM_ID_MAP.get(id);
	}
	
	/**
	 * Gets the enum that corresponds with the given id. Throws a RuntimeException if there is no corresponding enum for that id.
	 */
	public static OptimizerEnum getEnumFromIdThrowOnInvalid(int id) {
		return Optional.ofNullable(getEnumFromId(id))
				.orElseThrow(() -> new RuntimeException("Invalid id specified: " + id));
	}

	/**
	 * Get the enum by it's name or id.
	 * @throws an IllegalArgumentException if there are no enum values corresponding to the identifier.
	 * @throws a RuntimeException if the return value is ambiguous because the identifier used as a name 
	 * name and as an id correspond to different enum values.
	 */
	public static OptimizerEnum getEnumFromIdOrName(String identifier) {
		StringUtils.trim(identifier);
		OptimizerEnum a = getEnumFromName(identifier);
		OptimizerEnum b = getEnumFromId(identifier);
		if (a == null && b == null) {
			throw new IllegalArgumentException("Invalid Optimizer Provided: " + identifier + ". Valid options are " + getValuesAsString());
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
