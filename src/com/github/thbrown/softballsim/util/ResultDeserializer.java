package com.github.thbrown.softballsim.util;

import java.lang.reflect.Type;
import java.util.Optional;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Custom Deserializer for Result
 * 
 * Deserializer: Determines what subclass of Result a arbitrary serialized payload should be
 * deserialized into based on the "optimizer" field in the top level of the json object.
 */
public class ResultDeserializer implements JsonDeserializer<Result> {

  public final static String RESULT_TYPE = "optimizer";

  @Override
  public Result deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
      throws JsonParseException {

    // Figure out what type of result we were given data for
    JsonObject jsonObject = json.getAsJsonObject();
    JsonElement resultType = Optional.ofNullable(jsonObject.get(RESULT_TYPE))
        .orElseThrow(() -> new RuntimeException("Invalid result string. Must contain " + RESULT_TYPE));
    OptimizerEnum type = OptimizerEnum.getEnumFromIdOrName(resultType.getAsString());

    // Deserialize that data based on the type
    JsonObject data = jsonObject.getAsJsonObject();
    Type targetClass = type.getResultClass();

    // We'll get a StackOverflow exception here if targetClass == Result.class, we should always be
    // deserialising a subclass of Result
    return context.deserialize(data, targetClass);
  }

}
