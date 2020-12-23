package com.github.thbrown.softballsim.datasource.network;

import java.lang.reflect.Type;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Determines what subclass of DataSourceNetworkCommand a command in json format coming in over the
 * network should be deserialized into. This is based on the "command" field in the top level of the
 * json object.
 */
public class DataSourceNetworkCommandDeserializer implements JsonDeserializer<DataSourceNetworkCommand> {

  public final static String JSON_COMMAND_TYPE = "type";

  @Override
  public DataSourceNetworkCommand deserialize(final JsonElement json, final Type typeOfT,
      final JsonDeserializationContext context) throws JsonParseException {

    // Figure out what type of command we were given data for
    JsonObject jsonObject = json.getAsJsonObject();
    JsonElement optimizationType = jsonObject.get(JSON_COMMAND_TYPE);
    DataSourceNetworkCommandEnum type =
        DataSourceNetworkCommandEnum.getEnumFromApiValue(optimizationType.getAsString());

    // Deserialize that data based on the type
    JsonObject data = jsonObject.getAsJsonObject();
    return context.deserialize(data, type.getDeserializationTarget());
  }

}
