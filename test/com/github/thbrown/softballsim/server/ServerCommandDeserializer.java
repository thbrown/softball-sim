package com.github.thbrown.softballsim.server;

import java.lang.reflect.Type;
import com.github.thbrown.softballsim.datasource.network.DataSourceNetworkCommandDeserializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Similar to {@link DataSourceNetworkCommandDeserializer} but since commands received by the server
 * desierialize to subclasses of ServerCommand, we need a different deserializer.
 */
public class ServerCommandDeserializer implements JsonDeserializer<ServerCommand> {

  @Override
  public ServerCommand deserialize(final JsonElement json, final Type typeOfT,
      final JsonDeserializationContext context) throws JsonParseException {

    // Figure out what type of command we were given data for
    JsonObject jsonObject = json.getAsJsonObject();
    JsonElement optimizationType = jsonObject.get(DataSourceNetworkCommandDeserializer.JSON_COMMAND_TYPE);
    ServerCommandEnum type = ServerCommandEnum.getEnumFromApiValue(optimizationType.getAsString());

    // Deserialize that data based on the type
    JsonObject data = jsonObject.getAsJsonObject();
    return context.deserialize(data, type.getDeserializationTarget());
  }

}
