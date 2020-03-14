package com.github.thbrown.softballsim.lineup;

import java.lang.reflect.Type;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class BattingLineupSerializer implements JsonSerializer<BattingLineup> {

  @Override
  public JsonElement serialize(BattingLineup src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject obj = (JsonObject) context.serialize(src);
    obj.addProperty(BattingLineupDeserializer.JSON_COMMAND_TYPE, src.getLineupType());
    return obj;
  }

}
