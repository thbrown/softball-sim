package com.github.thbrown.softballsim.datasource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.thbrown.softballsim.util.StringUtils;

public enum DataSourceEnum {
	FILE_SYSTEM, 
	NETWORK;
	
  /**
   * Gets the enum with the given name. 
   * @throws IllegalArgumentException indicating available options if the name does not correspond to a enum value.
   */
  public static DataSourceEnum getEnumFromName(String name) {
	try {
	  StringUtils.trim(name);
	  return DataSourceEnum.valueOf(name);
	} catch (IllegalArgumentException | NullPointerException e) {
	  throw new IllegalArgumentException("Invalid Data Source Provided: " + name + ". Valid options are " + getValuesAsString(), e);
	}
  }
  
  public static String getValuesAsString() {
	List<String> valuesString = Arrays.stream(DataSourceEnum.values()).map(v -> v.toString()).collect(Collectors.toList());
	return "[" + String.join(", ", valuesString) + "]";
  }
	  
}
