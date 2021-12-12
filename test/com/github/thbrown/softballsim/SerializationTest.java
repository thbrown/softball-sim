package com.github.thbrown.softballsim;

import java.io.IOException;
import org.apache.commons.cli.MissingArgumentException;
import org.junit.Assert;
import org.junit.Test;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SerializationTest {

  @Test
  public void serializeResult() throws IOException, MissingArgumentException {
    final int INNINGS = 7;
    final int DURATION = 5;
    final int LINEUP_TYPE = 0;
    final String LINEUP = "1OiRCCmrn16iyK,Nelly,1CV6WRyspDjA7Z,1MPJ24EEyS0g6p";
    String[] args = {"-v", "-o", "MONTE_CARLO_ANNEALING", "-l", LINEUP, "-I",
        String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE), "-D", String.valueOf(DURATION), "-f"};
    Result result = SoftballSim.mainInternal(args);
    Assert.assertNotNull("Expected the optimization to produce a result, but it produced null", result);
    String json = GsonAccessor.getInstance().getCustom().toJson(result);
    System.out.println("OUTPUT");
    System.out.println(json);
    JsonObject jsonObject = (JsonObject) JsonParser.parseString(json);
    Assert.assertNotNull("Serialized result is missing an expected field " + jsonObject,
        jsonObject.get(Result.HUMAN_READABLE));
    Assert.assertNotNull("Serialized result is missing an expected field " + jsonObject,
        jsonObject.get(Result.FLAT_LINEUP));
  }

  @Test
  public void serializeResultSubclass() throws IOException, MissingArgumentException {
    final int INNINGS = 7;
    final int LINEUP_TYPE = 0;
    final int GAMES = 1;
    final String LINEUP = "1OiRCCmrn16iyK,Nelly,1CV6WRyspDjA7Z,1MPJ24EEyS0g6p";
    String[] args = {"-o", "MONTE_CARLO_EXHAUSTIVE", "-l", LINEUP, "-I",
        String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE), "-G", String.valueOf(GAMES), "-f"};

    Result result = SoftballSim.mainInternal(args);
    Assert.assertNotNull("Expected the simulation to produce a result, but it produced null", result);
    String json = GsonAccessor.getInstance().getCustom().toJson(result);
    JsonObject jsonObject = (JsonObject) JsonParser.parseString(json);
    Assert.assertNotNull("Serialized result is missing an expected field", jsonObject.get(Result.HUMAN_READABLE));
    Assert.assertNotNull("Serialized result is missing an expected field", jsonObject.get(Result.FLAT_LINEUP));
  }
}
