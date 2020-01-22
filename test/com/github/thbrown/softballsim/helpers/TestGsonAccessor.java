package com.github.thbrown.softballsim.helpers;

import com.github.thbrown.softballsim.server.ServerCommand;
import com.github.thbrown.softballsim.server.ServerCommandDeserializer;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.google.gson.GsonBuilder;

/**
 * This GSON accessor registers additional serializers/deserializers that only exist in the test
 * directory
 */
public class TestGsonAccessor extends GsonAccessor {

  private static GsonAccessor instance = new TestGsonAccessor();

  public static GsonAccessor getInstance() {
    return instance;
  }

  @Override
  protected void register(GsonBuilder gsonBuilder) {
    super.register(gsonBuilder);
    gsonBuilder.registerTypeAdapter(ServerCommand.class, new ServerCommandDeserializer());
  }
}
