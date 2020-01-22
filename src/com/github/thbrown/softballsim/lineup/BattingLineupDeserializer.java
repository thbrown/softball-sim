package com.github.thbrown.softballsim.lineup;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.github.thbrown.softballsim.datasource.network.DataSourceNetworkCommandEnum;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class BattingLineupDeserializer implements JsonDeserializer<BattingLineup> {

  public static final String JSON_COMMAND_TYPE = "lineup-type";

  @Override
  public BattingLineup deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {

    // Figure out what lineup type we were given data for
    JsonObject jsonObject = json.getAsJsonObject();
    JsonElement lineupType = jsonObject.get(JSON_COMMAND_TYPE);
    BattingLineupEnum type =
        BattingLineupEnum.getEnumFromApiValue(lineupType.getAsString());

    // Deserialize that data based on the type
    JsonObject data = jsonObject.getAsJsonObject();
    return context.deserialize(data, type.getDeserializationTarget());
  }

}


