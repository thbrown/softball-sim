package com.github.thbrown.softballsim;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import com.github.thbrown.softballsim.gson.BaseOptimizationDefinition;
import com.github.thbrown.softballsim.gson.MonteCarloExhaustiveOptimizatonDefinition;
import com.github.thbrown.softballsim.gson.OptimizationDefinitionDeserializer;
import com.github.thbrown.softballsim.util.Logger;
import com.google.gson.GsonBuilder;

public class DeserializationTest {
  @Test
  public void deserializeMonteCarloExaustiveData() throws IOException {
      String json = new String(Files.readAllBytes(Paths.get("./testData/monteCarloExaustiveData.json")));
      
      GsonBuilder gsonBldr = new GsonBuilder();
      gsonBldr.registerTypeAdapter(BaseOptimizationDefinition.class, new OptimizationDefinitionDeserializer());
      BaseOptimizationDefinition targetObject = gsonBldr.create().fromJson(json, BaseOptimizationDefinition.class);
   
      MonteCarloExhaustiveOptimizatonDefinition data = (MonteCarloExhaustiveOptimizatonDefinition) targetObject;
      assertEquals(7, data.getInnings());
      assertEquals(10000000, data.getIterations());
      assertEquals(0, data.getStartIndex());
      assertEquals(1, data.getLineupType());
      assertEquals(null, data.getInitialHistogram());
      assertEquals(null, data.getInitialScore());
      
      Logger.log(targetObject);
  }
  
}
