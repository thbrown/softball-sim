package com.github.thbrown.softballsim;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import com.github.thbrown.softballsim.Logger;
import com.github.thbrown.softballsim.gson.BaseOptimizationDefinition;
import com.github.thbrown.softballsim.gson.MonteCarloExaustiveOptimizatonDefinition;
import com.github.thbrown.softballsim.gson.OptimizationDefinitionDeserializer;
import com.google.gson.GsonBuilder;

public class DeserializationTest {
  @Test
  public void givenJsonHasNonMatchingFields_whenDeserializingWithCustomDeserializer_thenCorrect() throws IOException {
      String json = new String(Files.readAllBytes(Paths.get("./testData/monteCarloExaustiveData")));
      
      GsonBuilder gsonBldr = new GsonBuilder();
      gsonBldr.registerTypeAdapter(BaseOptimizationDefinition.class, new OptimizationDefinitionDeserializer());
      BaseOptimizationDefinition targetObject = gsonBldr.create().fromJson(json, BaseOptimizationDefinition.class);
   
      MonteCarloExaustiveOptimizatonDefinition data = (MonteCarloExaustiveOptimizatonDefinition) targetObject;
      assertEquals(7, data.getInnings());
      assertEquals(10000000, data.getIterations());
      assertEquals(0, data.getStartIndex());
      assertEquals(1, data.getLineupType());
      assertEquals(null, data.getInitialHistogram());
      assertEquals(null, data.getInitialScore());
      
      Logger.log(targetObject);
  }

}
