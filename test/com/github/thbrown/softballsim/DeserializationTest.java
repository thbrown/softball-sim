package com.github.thbrown.softballsim;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import com.github.thbrown.softballsim.data.gson.DataGame;
import com.github.thbrown.softballsim.data.gson.DataPlateAppearance;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.data.gson.DataTeam;
import com.github.thbrown.softballsim.optimizer.gson.OptimizerDefinition;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;

public class DeserializationTest {
  // TODO: once network is implemented
  /*
   * @Test public void deserializeMonteCarloExaustiveData() throws IOException { String json = new
   * String(Files.readAllBytes(Paths.get("./testData/monteCarloExaustiveData.json")));
   * 
   * GsonBuilder gsonBldr = new GsonBuilder();
   * gsonBldr.registerTypeAdapter(BaseOptimizationDefinition.class, new
   * OptimizationDefinitionDeserializer()); BaseOptimizationDefinition targetObject =
   * gsonBldr.create().fromJson(json, BaseOptimizationDefinition.class);
   * 
   * MonteCarloExhaustiveOptimizatonDefinition data = (MonteCarloExhaustiveOptimizatonDefinition)
   * targetObject; assertEquals(7, data.getInnings()); assertEquals(10000000, data.getIterations());
   * assertEquals(0, data.getStartIndex()); assertEquals(1, data.getLineupType()); assertEquals(null,
   * data.getInitialHistogram()); assertEquals(null, data.getInitialScore());
   * 
   * Logger.log(targetObject); }
   */

  @Test
  public void deserializeExampleStatsData() throws IOException {
    String json = new String(Files.readAllBytes(Paths.get("./stats/exampleData.json")));

    DataStats targetObject = GsonAccessor.getInstance().getCustom().fromJson(json, DataStats.class);

    // Spot check some fields - this is tightly coupled to the sample data
    DataPlayer somePlayer = targetObject.getPlayers().get(0);
    assertEquals(.609, somePlayer.getBattingAverage(), .001);
    assertEquals(115, somePlayer.getSingleCount());

    DataTeam someTeam = targetObject.getTeams().get(0);
    assertEquals(28, someTeam.getGames().size());
    assertEquals(1032, someTeam.getPlateApperances().size());
    assertEquals(38, someTeam.getPlayers().size());

    DataGame someGame = targetObject.getTeams().get(0).getGames().get(0);
    assertEquals(39, someGame.getPlateAppearances().size());
    assertEquals(11, someGame.getPlayers().size());

    DataPlateAppearance somePa = targetObject.getTeams().get(0).getGames().get(0).getPlateAppearances().get(0);
    assertEquals("Fireballs", somePa.getGame().getOpponent());
    assertEquals("E", somePa.getResult());
    assertEquals("Cheryl", somePa.getPlayer().getName());

    Logger.log(targetObject);
  }

  @Test
  public void deserializeJsonData() throws IOException {
    String json = new String(Files.readAllBytes(Paths.get("./json/monte-carlo-exhaustive.json")));


    OptimizerDefinition targetObject = GsonAccessor.getInstance().getCustom().fromJson(json, OptimizerDefinition.class);

    // TODO: assert some fields

    Logger.log(targetObject);
  }

}
