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
  public void serializeResult() throws Exception {
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
  public void serializeResultSubclass() throws Exception {
    final int INNINGS = 7;
    final int LINEUP_TYPE = 0;
    final int GAMES = 1;
    final String LINEUP = "1OiRCCmrn16iyK,Nelly,1CV6WRyspDjA7Z,1MPJ24EEyS0g6p";
    String[] args = {"-o", "MONTE_CARLO_EXHAUSTIVE", "-l", LINEUP, "-I",
        String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE), "-G", String.valueOf(GAMES), "-f"};

    Result result = SoftballSim.mainInternal(args);
    Assert.assertNotNull("Expected the simulation to produce a result, but it produced null", result);
    String json = GsonAccessor.getInstance().getCustom().toJson(result);
    Logger.log(json);
    JsonObject jsonObject = (JsonObject) JsonParser.parseString(json);
    Assert.assertNotNull("Serialized result is missing an expected field", jsonObject.get(Result.HUMAN_READABLE));
    Assert.assertNotNull("Serialized result is missing an expected field", jsonObject.get(Result.FLAT_LINEUP));
  }

  @Test
  public void updateStatusTest() throws Exception {
    String testResultString =
        "{\"histogram\":{\"128\":158,\"129\":147,\"130\":134,\"131\":121,\"132\":111,\"133\":105,\"134\":98,\"135\":85,\"136\":79,\"137\":62,\"138\":55,\"139\":43,\"140\":35,\"141\":38,\"142\":26,\"143\":20,\"144\":22,\"145\":14,\"146\":4,\"147\":7,\"148\":3,\"149\":1,\"93\":3,\"94\":5,\"95\":18,\"96\":30,\"97\":41,\"98\":55,\"99\":60,\"100\":100,\"101\":108,\"102\":129,\"103\":126,\"104\":115,\"105\":160,\"106\":169,\"107\":181,\"108\":169,\"109\":170,\"110\":187,\"111\":211,\"112\":184,\"113\":202,\"114\":208,\"115\":211,\"116\":200,\"117\":207,\"118\":192,\"119\":210,\"120\":212,\"121\":206,\"122\":203,\"123\":168,\"124\":157,\"125\":173,\"126\":167,\"127\":142},\"oppositeOfOptimalScore\":0.0,\"optimizer\":\"MONTE_CARLO_EXHAUSTIVE\",\"lineup\":{\"players\":[{\"id\":\"0000000000000i\",\"name\":\"Joe\",\"gender\":\"M\"},{\"id\":\"00000000000002\",\"name\":\"Benjamin\",\"gender\":\"M\"},{\"id\":\"0000000000000f\",\"name\":\"Geoff\",\"gender\":\"M\"},{\"id\":\"00000000000001\",\"name\":\"Thomas\",\"gender\":\"M\"},{\"id\":\"00000000000003\",\"name\":\"Lauren\",\"gender\":\"F\"},{\"id\":\"0000000000000S\",\"name\":\"Becca\",\"gender\":\"F\"},{\"id\":\"00000000000004\",\"name\":\"Katelyn\",\"gender\":\"F\"},{\"id\":\"00000000000005\",\"name\":\"Katie\",\"gender\":\"F\"}],\"size\":8,\"lineupType\":\"StandardBattingLineup\"},\"lineupScore\":14.9295,\"countTotal\":40320,\"countCompleted\":6447,\"elapsedTimeMs\":37404,\"status\":\"PAUSED\",\"estimatedTimeRemainingMs\":183484,\"humanReadableDetails\":\"Histogram:\\n Runs | # Lineups | Histogram\\n 9.3 | 3 | \\n 9.4 | 5 | \\n 9.5 | 18 | ██\\n 9.6 | 30 | ███\\n 9.7 | 41 | ███\\n 9.8 | 55 | █████\\n 9.9 | 60 | █████\\n 10.0 | 100 | ████████\\n 10.1 | 108 | █████████\\n 10.2 | 129 | ███████████\\n 10.3 | 126 | ███████████\\n 10.4 | 115 | ██████████\\n 10.5 | 160 | ██████████████\\n 10.6 | 169 | ██████████████\\n 10.7 | 181 | ███████████████\\n 10.8 | 169 | ██████████████\\n 10.9 | 170 | ██████████████\\n 11.0 | 187 | ████████████████\\n 11.1 | 211 | ██████████████████\\n 11.2 | 184 | ████████████████\\n 11.3 | 202 | █████████████████\\n 11.4 | 208 | ██████████████████\\n 11.5 | 211 | ██████████████████\\n 11.6 | 200 | █████████████████\\n 11.7 | 207 | ██████████████████\\n 11.8 | 192 | ████████████████\\n 11.9 | 210 | ██████████████████\\n 12.0 | 212 | ██████████████████\\n 12.1 | 206 | █████████████████\\n 12.2 | 203 | █████████████████\\n 12.3 | 168 | ██████████████\\n 12.4 | 157 | █████████████\\n 12.5 | 173 | ███████████████\\n 12.6 | 167 | ██████████████\\n 12.7 | 142 | ████████████\\n 12.8 | 158 | █████████████\\n 12.9 | 147 | ████████████\\n 13.0 | 134 | ███████████\\n 13.1 | 121 | ██████████\\n 13.2 | 111 | █████████\\n 13.3 | 105 | █████████\\n 13.4 | 98 | ████████\\n 13.5 | 85 | ███████\\n 13.6 | 79 | ███████\\n 13.7 | 62 | █████\\n 13.8 | 55 | █████\\n 13.9 | 43 | ████\\n 14.0 | 35 | ███\\n 14.1 | 38 | ███\\n 14.2 | 26 | ██\\n 14.3 | 20 | ██\\n 14.4 | 22 | ██\\n 14.5 | 14 | █\\n 14.6 | 4 | \\n 14.7 | 7 | █\\n 14.8 | 3 | \\n 14.9 | 1 |\\n\\n\",\"flatLineup\":[\"0000000000000i\",\"00000000000002\",\"0000000000000f\",\"00000000000001\",\"00000000000003\",\"0000000000000S\",\"00000000000004\",\"00000000000005\"]}";
    testResultString =
        Result.copyWithNewStatusStringOnly(testResultString, ResultStatusEnum.ALLOCATING_RESOURCES, "Go Broncos");
    Result r = GsonAccessor.getInstance().getCustom().fromJson(testResultString, Result.class);
    Assert.assertEquals(r.getStatus(), ResultStatusEnum.ALLOCATING_RESOURCES);
  }
}
