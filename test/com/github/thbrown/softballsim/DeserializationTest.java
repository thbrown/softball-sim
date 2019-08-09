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
import com.github.thbrown.softballsim.helpers.TimeEstimationConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DeserializationTest {
  @Test
  public void deserializeMonteCarloExaustiveData() throws IOException {
      String json = new String(Files.readAllBytes(Paths.get("./testData/monteCarloExaustiveData.json")));
      
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
  
  @Test
  public void serializeTimeEstimationConfig() throws IOException {
      TimeEstimationConfig config = new TimeEstimationConfig();
      config.setInnings(7);
      config.setIterations(10000);
      config.setThreads(8);
      config.setLineupType(1);
      config.setCoefficients(new double[] {1,2,3,4,5});
      config.setErrorAdjustments(new double[] {0,1,1.1,1.2,1.3,1.4,1.5,1.6,1.7});
      Gson g = new Gson();
      Logger.log(g.toJson(config));
  }

}
