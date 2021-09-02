package com.github.thbrown.softballsim.cloud;

import java.util.HashMap;
import java.util.Set;

/**
 * This class is just so we can customize the deserialization behavior for json data we want in a
 * map without changing the behavior for maps in general
 */
public class MapWrapper {

  HashMap<String, String> arguments = new HashMap<>();

  public String get(String id) {
    return arguments.get(id);
  }

  public void put(String key, String value) {
    arguments.put(key, value);
  }

  public void remove(String string) {
    arguments.remove(string);
  }

  public Set<String> keySet() {
    return arguments.keySet();
  }

}
