package com.github.thbrown.softballsim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.Test;
import com.github.thbrown.softballsim.data.gson.DataGame;
import com.github.thbrown.softballsim.data.gson.DataPlateAppearance;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.data.gson.DataTeam;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.optimizer.gson.OptimizerDefinition;
import com.github.thbrown.softballsim.optimizer.impl.montecarloannealing.MonteCarloAnnealingResult;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;

public class DeserializationTest {

  @Test
  public void deserializeExampleStatsData() throws IOException {
    String json = new String(Files.readAllBytes(Paths.get("./stats/exampleData.json")));

    DataStats targetObject = GsonAccessor.getInstance().getCustom().fromJson(json, DataStats.class);

    // Spot check some fields - this is tightly coupled to the sample data
    DataPlayer somePlayer = targetObject.getPlayers().get(0);
    assertEquals(.597, somePlayer.getBattingAverage(), .001);
    assertEquals(115, somePlayer.getSingleCount());

    DataTeam someTeam = targetObject.getTeams().get(0);
    assertEquals(28, someTeam.getGames().size());
    assertEquals(1032, someTeam.getPlateAppearances().size());
    assertEquals(38, someTeam.getPlayers().size());

    DataGame someGame = targetObject.getTeams().get(0).getGames().get(0);
    assertEquals(39, someGame.getPlateAppearances().size());
    assertEquals(11, someGame.getPlayers().size());

    DataPlateAppearance somePa = targetObject.getTeams().get(0).getGames().get(0).getPlateAppearances().get(0);
    assertEquals("Fireballs", somePa.getGame().getOpponent());
    assertEquals("E", somePa.getResult());
    assertEquals("Cheryl", somePa.getPlayer().getName());
  }

  @Test
  public void deserializeJsonData() throws IOException {
    String json = new String(Files.readAllBytes(Paths.get("./docs/definitions/0.json")));

    OptimizerDefinition targetObject = GsonAccessor.getInstance().getCustom().fromJson(json, OptimizerDefinition.class);

    // TODO: assert some fields
  }

  @Test
  public void deserializeJsonDataAsMap() throws IOException {
    String json = new String(Files.readAllBytes(Paths.get("./stats/exampleData.json")));

    Map<String, String> targetObject = GsonAccessor.getInstance().getCustom().fromJson(json, Map.class);
  }

  /**
   * This test fails if an optimizer returns the abstract class "Result.class" from it's
   * "getResultClass()" method.
   * 
   * The Result class can not be deserialized and will result in a StackOverflow exception when using
   * cached results.
   * 
   * Optimizations must provide their own subclass of Result here even if it's behavior is no
   * different (See MonteCarloAnnealingResult as an axample)
   */
  @Test
  public void optimizersMustReturnResultSubtype() throws IOException {
    for (OptimizerEnum opt : OptimizerEnum.values()) {
      assertNotEquals(opt.getResultClass(), Result.class);
    }
  }

}
