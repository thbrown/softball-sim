package com.github.thbrown.softballsim.lineup;

import java.lang.reflect.Type;
import com.github.thbrown.softballsim.data.gson.helpers.DataPlayerLookup;
import com.github.thbrown.softballsim.util.Logger;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class BattingLineupSerializerDeserializer
    implements JsonDeserializer<BattingLineup>, JsonSerializer<BattingLineup> {

  public static final String JSON_COMMAND_TYPE = "lineupType";
  public final DataPlayerLookup statsLookup;

  public BattingLineupSerializerDeserializer(DataPlayerLookup statsLookup) {
    this.statsLookup = statsLookup;
  }

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
    BattingLineup deserialized = context.deserialize(data, type.getDeserializationTarget());

    if (statsLookup == null) {
      Logger.log(
          "WARNING: A batting lineup was deserialized by a gson object without a DataPlayerLookup, players will have not associated stats");
    } else {
      deserialized.populateStats(statsLookup);
    }
    return deserialized;
  }

  @Override
  public JsonElement serialize(BattingLineup src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = (JsonObject) context.serialize(src);
    obj.addProperty(BattingLineupSerializerDeserializer.JSON_COMMAND_TYPE, src.getLineupType());
    return obj;
  }

}


