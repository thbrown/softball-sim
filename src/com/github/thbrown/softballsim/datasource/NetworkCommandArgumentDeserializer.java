package com.github.thbrown.softballsim.datasource;

import java.lang.reflect.Type;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class NetworkCommandArgumentDeserializer {

  // These names are hard coded in the test data and also defined in softball app
  public final String JSON_OPTIMIZATION_TYPE = "optimizationType";
  public final String JSON_OPTIMIZATION_DATA = "optimizationData";

  public NetworkCommandArgumentDeserializer deserialize(final JsonElement json, final Type typeOfT,
      final JsonDeserializationContext context)
      throws JsonParseException {

    // Figure out what type of optimization we were given data for
    JsonObject jsonObject = json.getAsJsonObject();
    JsonElement optimizationType = jsonObject.get(JSON_OPTIMIZATION_TYPE);
    OptimizerEnum type = OptimizerEnum.getEnumFromIdThrowOnInvalid(optimizationType.getAsInt());

    /*
     * { optimizationType: "blah", optimizationData: "blah", lineup: ["blah", "blah", "blah"], args: {
     * arg1: arg2: arg3: arg4: arg5 }
     * 
     */

    // Deserialize that data based on the type
    /*
     * JsonObject data = jsonObject.getAsJsonObject(); BaseOptimizationDefinition result =
     * context.deserialize(data.getAsJsonObject(JSON_OPTIMIZATION_DATA),
     * type.getDeserializationTarget()); return result;
     */
    return null;

  }

}
