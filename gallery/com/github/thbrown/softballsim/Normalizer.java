package com.github.thbrown.softballsim;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Normalizer {

  private Map<String, Double> input = new HashMap<>();
  private Map<String, Double> output = null;

  public void add(String key, double value) {
    input.put(key, value);
  }

  public double getNormalizedValue(String key) {
    if (output == null) {
      output = new HashMap<>();

      Set<String> keys = input.keySet();
      Set<Double> values = keys.stream().map(v -> input.get(v)).collect(Collectors.toSet());
      double max = values.stream().mapToDouble(v -> v).max().getAsDouble();
      double min = values.stream().mapToDouble(v -> v).min().getAsDouble();
      for (String inputKey : keys) {
        output.put(inputKey, (input.get(inputKey) - min) / (max - min));
      }
    }
    return output.get(key);
  }

}
