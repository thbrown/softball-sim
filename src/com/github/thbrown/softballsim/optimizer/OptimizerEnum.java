package com.github.thbrown.softballsim.optimizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.gson.OptimizerDefinition;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloExhaustiveOptimizer;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.MonteCarloAdaptiveOptimizer;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;

/**
 * An enumeration of all the optimizer implementations.
 * 
 * This enum is used to access those implementations.
 */
public enum OptimizerEnum {
  // TODO: Can't we just use the id from the json?
  MONTE_CARLO_EXHAUSTIVE(0, new MonteCarloExhaustiveOptimizer()),
  MONTE_CARLO_ADAPTIVE(1, new MonteCarloAdaptiveOptimizer());

  private final int id;
  private final Optimizer<? extends Result> optimizerImplementation;
  private final OptimizerDefinition optimizerDefinition;

  OptimizerEnum(int id, Optimizer<? extends Result> optimizer) {
    this.id = id;
    this.optimizerImplementation = optimizer;
    try {
      String optimizerDefinitionJson =
          new String(Files.readAllBytes(Paths.get("./json/" + optimizer.getJsonDefinitionFileName())));
      this.optimizerDefinition =
          GsonAccessor.getInstance().getCustom().fromJson(optimizerDefinitionJson, OptimizerDefinition.class);
    } catch (IOException e) {
      throw new RuntimeException("Error while deserializing " + optimizer.getJsonDefinitionFileName(), e);
    }
  }

  public int getId() {
    return id;
  }

  public List<Option> getCommandLineOptions() {
    return optimizerDefinition.getArgumentsAsCommandLineOptions();
  }

  public Map<String, String> getArgumentsAndValuesAsMap(CommandLine cmd) {
    return optimizerDefinition.getArgumentsAndValuesAsMap(cmd);
  }

  /**
   * Run the optimization!
   * 
   * Using a generic method here so that the compiler will complain if this method is passed a
   * subclass of Result (or Result itself) that does not match the generic used by this implementation
   * of Optimizer. For example, MonteCarloExhaustiveOptimizer<MonteCarloExhaustiveResult> will only
   * ever accept a MonteCarloExhaustiveResult for the existingResult parameter of this method.
   * 
   * See Wildcard Capture - https://docs.oracle.com/javase/tutorial/java/generics/capture.html
   */
  @SuppressWarnings("unchecked")
  public <T extends Result> Result optimize(List<String> players, LineupTypeEnum lineupType, DataStats data,
      Map<String, String> arguments,
      ProgressTracker progressTracker, T existingResult) {

    // Cast the optimizer to itself, this gets us a reference to an Optimizer that uses a particular
    // generic (Optimizer<T>) rather than a reference with a bounded wildcard generic (Optimizer<?
    // extends Result>). With this new reference we can pass that optimizer a specific subclass of
    // result (T).
    Optimizer<T> optimizer = this.optimizerImplementation.getClass().cast(this.optimizerImplementation);

    // Print the arguments we are using for this optimization before we start
    // TODO: Use 
    Logger.log("*********************************************************************");
    Logger.log("Optimizer: " + this.optimizerDefinition.getName());
    Logger.log("*********************************************************************");
    for (String key : arguments.keySet()) {
      // TODO: Some left pad here to make the value align
      Logger.log(key + ": " + arguments.get(key));
    }
    Logger.log("*********************************************************************");

    return optimizer.optimize(players, lineupType, data, arguments, progressTracker, existingResult);
  }

  /**
   * Gets the enum with the given name. Returns null if there is no enum with that name.
   */
  public static OptimizerEnum getEnumFromName(String name) {
    return ENUM_NAME_MAP.get(name);
  }

  /**
   * Gets the enum that corresponds with the given id. Returns null if there is no corresponding enum
   * for that id.
   */
  public static OptimizerEnum getEnumFromId(int id) {
    return getEnumFromId(String.valueOf(id));
  }

  private static OptimizerEnum getEnumFromId(String id) {
    return ENUM_ID_MAP.get(id);
  }

  /**
   * Gets the enum that corresponds with the given id. Throws a RuntimeException if there is no
   * corresponding enum for that id.
   */
  public static OptimizerEnum getEnumFromIdThrowOnInvalid(int id) {
    return Optional.ofNullable(getEnumFromId(id))
        .orElseThrow(() -> new RuntimeException("Invalid id specified: " + id));
  }

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

  /**
   * Get the enum by it's name or id.
   * 
   * @throws an IllegalArgumentException if there are no enum values corresponding to the identifier.
   * @throws a RuntimeException if the return value is ambiguous because the identifier used as a name
   *         name and as an id correspond to different enum values.
   */
  public static OptimizerEnum getEnumFromIdOrName(String identifier) {
    StringUtils.trim(identifier);
    OptimizerEnum a = getEnumFromName(identifier);
    OptimizerEnum b = getEnumFromId(identifier);
    if (a == null && b == null) {
      throw new IllegalArgumentException(
          "Invalid Optimizer Provided: " + identifier + ". Valid options are " + getValuesAsString());
    }
    if (a == null || b == null || a == b) {
      return a == null ? b : a;
    }
    throw new RuntimeException("Ambiguous result for identifier " + identifier + ". This could be " + a + " or " + b);
  }

  public static String getValuesAsString() {
    List<String> valuesString = Arrays.stream(OptimizerEnum.values())
        .map(v -> String.join(" ", v.toString(), "- " + v.getId())).collect(Collectors.toList());
    return "[" + String.join(", ", valuesString) + "]";
  }

}
