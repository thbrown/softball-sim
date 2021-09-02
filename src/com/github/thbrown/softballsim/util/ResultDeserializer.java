package com.github.thbrown.softballsim.util;

import java.lang.reflect.Type;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

/**
 * Determines what subclass of Result a cached file in json format saved in the cached directory
 * should be deserialized into. This is based on the "optimizer" field in the top level of the json
 * object.
 */
public class ResultDeserializer implements JsonDeserializer<Result> {

  public final static String RESULT_TYPE = "optimizer";

  @Override
  public Result deserialize(final JsonElement json, final Type typeOfT,
      final JsonDeserializationContext context) throws JsonParseException {

    // Figure out what type of result we were given data for
    JsonObject jsonObject = json.getAsJsonObject();
    String resultType = jsonObject.get(RESULT_TYPE).getAsString();
    OptimizerEnum type = OptimizerEnum.getEnumFromIdOrName(resultType);

    // Deserialize that data based on the type
    JsonObject data = jsonObject.getAsJsonObject();
    Type targetClass = type.getResultClass();

    // Avoid stack overflow caused by recursion: use the default deserializer if the optimizer is using
    // the base result class
    if (targetClass == Result.class) {
      return GsonAccessor.getInstance().getCustomWithExclusions(Result.class).fromJson(jsonObject, Result.class);
    } else {
      return context.deserialize(data, targetClass);
    }
  }

}
