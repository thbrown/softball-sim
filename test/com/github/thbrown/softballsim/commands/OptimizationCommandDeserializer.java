package com.github.thbrown.softballsim.commands;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class OptimizationCommandDeserializer implements JsonDeserializer<BaseOptimizationCommand> {

	public final String JSON_COMMAND_TYPE = "command";
	
	@Override
	public BaseOptimizationCommand deserialize(final JsonElement json, final Type typeOfT,
			final JsonDeserializationContext context) throws JsonParseException {

		// Figure out what type of command we were given data for
		JsonObject jsonObject = json.getAsJsonObject();
		JsonElement optimizationType = jsonObject.get(JSON_COMMAND_TYPE);
		OptimizationCommandEnum type = OptimizationCommandEnum.getEnumFromApiValue(optimizationType.getAsString());

		// Deserialize that data based on the type
		JsonObject data = jsonObject.getAsJsonObject();
		return context.deserialize(data, type.getDeserializationTarget());
	}
	
}
