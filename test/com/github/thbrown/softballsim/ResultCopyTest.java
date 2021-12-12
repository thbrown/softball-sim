package com.github.thbrown.softballsim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.StandardBattingLineup;
import com.github.thbrown.softballsim.optimizer.impl.montecarloannealing.MonteCarloAnnealingResult;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloExhaustiveResult;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ResultCopyTest {

  @Test
  public void copyResultWithNewStatus() throws IOException {
    String json = new String(Files.readAllBytes(Paths.get("./stats/exampleData.json")));
    DataStats dataStats = GsonAccessor.getInstance().getCustom().fromJson(json, DataStats.class);
    List<DataPlayer> players =
        Arrays.asList(dataStats.getPlayers().get(0), dataStats.getPlayers().get(1), dataStats.getPlayers().get(2));
    BattingLineup lineup = new StandardBattingLineup(players);

    String testString = "WOO HOO NO ERRORS";
    Result r =
        new MonteCarloAnnealingResult(lineup, 10, 100, 50, 1000, ResultStatusEnum.IN_PROGRESS);
    Result rCopy = r.copyWithNewStatus(ResultStatusEnum.COMPLETE, testString);

    // Verify a copy was made and updated fields are correct
    assertNotEquals(r, rCopy);
    assertEquals(rCopy.getStatus(), ResultStatusEnum.COMPLETE);
    assertEquals(rCopy.getStatusMessage(), testString);

    // Verify stats get populated for players in the batting lineup
    assertNotEquals(rCopy.getLineup().getBatter(0).getBattingAverage(), 0);
  }

  @Test
  public void copyResultWithNewTimeEstimation() throws IOException {
    String json = new String(Files.readAllBytes(Paths.get("./stats/exampleData.json")));
    DataStats dataStats = GsonAccessor.getInstance().getCustom().fromJson(json, DataStats.class);
    List<DataPlayer> players =
        Arrays.asList(dataStats.getPlayers().get(0), dataStats.getPlayers().get(1), dataStats.getPlayers().get(2));
    BattingLineup lineup = new StandardBattingLineup(players);

    Long testValue = 999L;
    Result r =
        new MonteCarloAnnealingResult(lineup, 10, 100, 50, 1000, ResultStatusEnum.IN_PROGRESS);
    Result rCopy = r.copyWithNewEstimatedTimeRemainingMs(testValue);

    // Verify a copy was made and updated fields are correct
    assertNotEquals(r, rCopy);
    assertEquals(rCopy.getEstimatedTimeRemainingMs(), testValue);

    // Verify stats get populated for players in the batting lineup
    assertNotEquals(rCopy.getLineup().getBatter(0).getBattingAverage(), 0);
  }

  @Test
  public void copyResultWithMultipleBattingLineups() throws IOException {
    String json = new String(Files.readAllBytes(Paths.get("./stats/exampleData.json")));
    DataStats dataStats = GsonAccessor.getInstance().getCustom().fromJson(json, DataStats.class);
    List<DataPlayer> bestPlayers =
        Arrays.asList(dataStats.getPlayers().get(0), dataStats.getPlayers().get(1), dataStats.getPlayers().get(2));
    BattingLineup bestLineup = new StandardBattingLineup(bestPlayers);

    Map<Long, Long> histogram = Map.ofEntries(
        java.util.Map.entry(10L, 2L),
        java.util.Map.entry(11L, 3L),
        java.util.Map.entry(12L, 4L));

    List<DataPlayer> worstPlayers =
        Arrays.asList(dataStats.getPlayers().get(0), dataStats.getPlayers().get(1), dataStats.getPlayers().get(2));
    BattingLineup worstLineup = new StandardBattingLineup(worstPlayers);

    String testString = "WOO HOO NO ERRORS";
    MonteCarloExhaustiveResult r = new MonteCarloExhaustiveResult(bestLineup, 10, 100, 50, 1000, histogram,
        ResultStatusEnum.IN_PROGRESS, worstLineup, 1);
    Logger.log(r.getHumanReadableDetails());
    MonteCarloExhaustiveResult rCopy =
        (MonteCarloExhaustiveResult) r.copyWithNewStatus(ResultStatusEnum.COMPLETE, testString);
    assertNotEquals(r, rCopy);

    // Verify stats get populated for players in all the batting lineups
    assertNotEquals(rCopy.getLineup().getBatter(0).getBattingAverage(), 0);
    assertNotEquals(rCopy.getOppositeOfOptimalLineup().getBatter(0).getBattingAverage(), 0);
  }
}

