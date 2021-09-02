package com.github.thbrown.softballsim.cloud;

import java.lang.reflect.Type;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Deserializer that forces the entire serialized json object into a Map<String,String>
 */
public class MapWrapperDeserializer implements JsonDeserializer<MapWrapper> {

  @Override
  public MapWrapper deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {

    MapWrapper result = new MapWrapper();
    JsonObject jsonObject = json.getAsJsonObject();
    for (String key : jsonObject.keySet()) {
      String value = jsonObject.get(key).toString();
      result.put(key, value.replaceAll("^\"|\"$", "")); // Don't put strings in double quotes
    }
    return result;
  }
}
