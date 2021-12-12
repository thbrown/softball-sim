package com.github.thbrown.softballsim.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultSerializer;
import com.github.thbrown.softballsim.cloud.MapWrapper;
import com.github.thbrown.softballsim.cloud.MapWrapperDeserializer;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.data.gson.DataStatsDeserializer;
import com.github.thbrown.softballsim.data.gson.helpers.DataPlayerLookup;
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
      register(gsonBuilder, null);
      this.customGson = gsonBuilder.create();
    }
    return this.customGson;
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
   *         application AND that has DataPlayerLookup that can be used to deserialize BattingLineups
   *         and populate the DataPlayer classes with statistics after deserialization. Attempting to
   *         deserialize a BattingLineup without a Gson instance from this method will result in a
   *         RuntimeException
   */
  public Gson getCustomWithStatsLookup(DataPlayerLookup dataPlayerLookup) {
    GsonBuilder gsonBuilder = new GsonBuilder();
    register(gsonBuilder, dataPlayerLookup);
    return gsonBuilder.create();
  }

  protected void register(GsonBuilder gsonBuilder, DataPlayerLookup statsLookup, Class<?>... exclusions) {
    Set<Class<?>> lookup = new HashSet<>();
    lookup.addAll(Arrays.asList(exclusions));

    // Deserializers
    gsonBuilder.registerTypeAdapter(DataStats.class, new DataStatsDeserializer());
    gsonBuilder.registerTypeAdapter(OptimizerDefinitionOption.class, new OptimizerDefinitionOptioneDeserializer());
    gsonBuilder.registerTypeAdapter(MapWrapper.class, new MapWrapperDeserializer());
    gsonBuilder.registerTypeAdapter(Result.class, new ResultDeserializer());

    // Serializers & Deserializers
    gsonBuilder.registerTypeAdapter(BattingLineup.class, new BattingLineupSerializerDeserializer(statsLookup));

    // Other
    gsonBuilder.registerTypeAdapterFactory(new ResultSerializer());

    // Allow NaN
    gsonBuilder.serializeSpecialFloatingPointValues();
  }

}
