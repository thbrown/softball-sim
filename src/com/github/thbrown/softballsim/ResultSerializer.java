package com.github.thbrown.softballsim;

import com.github.thbrown.softballsim.util.GsonAccessor;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

/**
 * We need a TypeAdapter here instead of a custom Serializer because we need to modify the
 * serialization of all objects that inherit from Result.
 * 
 * This class currently only affects Result, but can be renamed and modified to affect other classes
 * if needed.
 */
public class ResultSerializer implements TypeAdapterFactory {

  @SuppressWarnings("unchecked")
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {

    final ResultSerializer self = this;

    // If the class that type token represents is a subclass of Base
    // then return your special adapter
    if (Result.class.isAssignableFrom(typeToken.getRawType())) {
      return (TypeAdapter<T>) new TypeAdapter<Result>() {
        @Override
        public void write(JsonWriter out, Result src) throws IOException {

          final TypeAdapter<Result> delegate = (TypeAdapter<Result>) gson.getDelegateAdapter(self, typeToken);

          // First, serialize normally
          final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
          JsonObject jsonObject = (JsonObject) delegate.toJsonTree(src);

          // Then, add any derived fields
          jsonObject.addProperty(Result.HUMAN_READABLE, src.getHumanReadableDetails());
          jsonObject.addProperty(Result.FLAT_LINEUP,
              GsonAccessor.getInstance().getDefault().toJson(src.getFlatLineup()));

          // Write the final object
          elementAdapter.write(out, jsonObject);
        }

        @Override
        public Result read(JsonReader in) throws IOException {
          final TypeAdapter<Result> delegate = (TypeAdapter<Result>) gson.getDelegateAdapter(self, typeToken);
          return delegate.read(in);
        }
      };
    }
    return null;
  }

}
