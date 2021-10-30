package com.github.thbrown.softballsim.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.cloud.MapWrapper;
import com.github.thbrown.softballsim.cloud.MapWrapperDeserializer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.data.gson.DataStatsDeserializer;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.BattingLineupSerializerDeserializer;
import com.github.thbrown.softballsim.optimizer.gson.OptimizerDefinitionOption;
import com.github.thbrown.softballsim.optimizer.gson.OptimizerDefinitionOptioneDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class provides a consistent way to retrieve and re-use GSON objects
 */
public class GsonAccessor {

  private static GsonAccessor instance = new GsonAccessor();

  private Gson customGson;

  private Gson defaultGson;

  public static GsonAccessor getInstance() {
    return instance;
  }

  /**
   * @return a GSON instance with all the custom serializers/deserailizers registered for this
   *         application
   */
  public Gson getCustom() {
    if (this.customGson == null) {
      GsonBuilder gsonBuilder = new GsonBuilder();
      register(gsonBuilder);
      this.customGson = gsonBuilder.create();
    }
    return this.customGson;
  }

  protected void register(GsonBuilder gsonBuilder, Class<?>... exclusions) {
    Set<Class<?>> lookup = new HashSet<>();
    lookup.addAll(Arrays.asList(exclusions));

    // Deserializers
    if (!lookup.contains(DataStats.class)) {
      gsonBuilder.registerTypeAdapter(DataStats.class, new DataStatsDeserializer());
    }
    if (!lookup.contains(OptimizerDefinitionOption.class)) {
      gsonBuilder.registerTypeAdapter(OptimizerDefinitionOption.class, new OptimizerDefinitionOptioneDeserializer());
    }
    if (!lookup.contains(MapWrapper.class)) {
      gsonBuilder.registerTypeAdapter(MapWrapper.class, new MapWrapperDeserializer());
    }

    // Serializers & Deserializers
    if (!lookup.contains(BattingLineup.class)) {
      gsonBuilder.registerTypeAdapter(BattingLineup.class, new BattingLineupSerializerDeserializer());
    }
    if (!lookup.contains(Result.class)) {
      gsonBuilder.registerTypeAdapter(Result.class, new ResultSerializerDeserializer());
    }

    // Allow NaN
    gsonBuilder.serializeSpecialFloatingPointValues();
  }

  /**
   * @return the default GSON instance, with no registered custom serializers/deserailizers
   */
  public Gson getDefault() {
    if (this.defaultGson == null) {
      GsonBuilder gsonBuilder = new GsonBuilder();
      gsonBuilder.serializeSpecialFloatingPointValues();
      this.defaultGson = gsonBuilder.create();
    }
    return this.defaultGson;
  }

  /**
   * @return a GSON instance with all the custom serializers/deserailizers registered for this
   *         application except for the ones specified by the exclusions parameter. Excluded types
   *         will use their default serializers.deserializers
   */
  public Gson getCustomWithExclusions(Class<?>... exclusions) {
    GsonBuilder gsonBuilder = new GsonBuilder();
    register(gsonBuilder, exclusions);
    return gsonBuilder.create();
  }


}
