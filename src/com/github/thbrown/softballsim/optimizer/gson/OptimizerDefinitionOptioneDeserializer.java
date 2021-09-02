package com.github.thbrown.softballsim.optimizer.gson;

import java.lang.reflect.Type;
import java.util.Arrays;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class OptimizerDefinitionOptioneDeserializer implements JsonDeserializer<OptimizerDefinitionOption> {

  private static final String[] validShortLabels = {
      "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
      "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

  // The key value in the json that determines which concrete class this object is deserialised into
  public final String INPUT_TYPE = "type";
  public final String SHORT_LABEL = "shortLabel";

  @Override
  public OptimizerDefinitionOption deserialize(final JsonElement json, final Type typeOfT,
      final JsonDeserializationContext context)
      throws JsonParseException {

    // Figure out what type of input we were given data for
    JsonObject jsonObject = json.getAsJsonObject();
    JsonElement optimizationType = jsonObject.get(INPUT_TYPE);
    OptimizerDefinitionOptionsEnum type =
        OptimizerDefinitionOptionsEnum.getEnumByJsonType(optimizationType.getAsString());

    // Validate shortLabel
    String shortLabel = jsonObject.get(SHORT_LABEL).getAsString();
    if (!Arrays.asList(validShortLabels).contains(shortLabel)) {
      throw new RuntimeException("Invalid shortLable. Value must be a single lower-case letter but was " + shortLabel);
    }

    // Deserialize that data based on the type
    OptimizerDefinitionOption result = context.deserialize(json, type.getDeserializationTarget());
    return result;
  }

}
