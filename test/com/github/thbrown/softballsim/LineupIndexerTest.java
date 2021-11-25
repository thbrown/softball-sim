package com.github.thbrown.softballsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.StandardBattingLineup;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.*;
import com.github.thbrown.softballsim.lineupindexer.*;
import com.github.thbrown.softballsim.datasource.*;
import org.apache.commons.cli.*;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.apache.commons.math3.util.Pair;


public class LineupIndexerTest {

  @Test
  public void lineupIndexRoundTrip() throws IOException, InterruptedException {
    // Get data from file system
    CommandLineOptions commandLineOptions = CommandLineOptions.getInstance();
    Options commonOptions = commandLineOptions.getOptionsForFlags(DataSourceEnum.FILE_SYSTEM, null);
    CommandLine commonCmd = commandLineOptions.parse(commonOptions, new String[0], true);
    DataStats stats = DataSourceEnum.FILE_SYSTEM.getData(commonCmd);

    List<String> players = new ArrayList<>();
    players.add("Paul");
    players.add("Dora");
    players.add("Keenan");
    players.add("Nelly");
    players.add("Devon");
    players.add("Jordyn");
    players.add("Brianna");
    players.add("Alexa");
    players.add("Ivan");
    // players.add("Tina");
    players = stats.convertPlayersListToIds(players);

    for (LineupTypeEnum lineupType : LineupTypeEnum.values()) {
      BattingLineupIndexer indexer = lineupType.getLineupIndexer(stats, players);
      Logger.log(lineupType + " (" + indexer.size() + ")");

      for (int i = 0; i < indexer.size(); i++) {
        // long index = i;// ThreadLocalRandom.current().nextLong(indexer.size());

        // BattingLineup lineup = indexer.getLineup(index);

        // long roundTripIndex = indexer.getIndex(lineup);

        // BattingLineup lineup2 = indexer.getLineup(roundTripIndex);

        // assertEquals("The index before getLineup/getIndex lookup did not match the index after for " +
        // lineupType
        // + " on index " + index, index, roundTripIndex);
      }
    }
  }


  @Test
  public void lineupRandomNeighborTest() throws IOException, InterruptedException {
    // Get data from file system
    CommandLineOptions commandLineOptions = CommandLineOptions.getInstance();
    Options commonOptions = commandLineOptions.getOptionsForFlags(DataSourceEnum.FILE_SYSTEM, null);
    CommandLine commonCmd = commandLineOptions.parse(commonOptions, new String[0], true);
    DataStats stats = DataSourceEnum.FILE_SYSTEM.getData(commonCmd);

    List<String> players = new ArrayList<>();
    players.add("Paul");
    players.add("Dora");
    players.add("Keenan");
    players.add("Nelly");
    players.add("Devon");
    players.add("Jordyn");
    // players.add("Brianna");
    players.add("Alexa");
    players.add("Ivan");
    // players.add("Tina");
    players = stats.convertPlayersListToIds(players);

    for (LineupTypeEnum lineupType : LineupTypeEnum.values()) {
      BattingLineupIndexer indexer = lineupType.getLineupIndexer(stats, players);
      Logger.log(lineupType);
      for (int i = 0; i < indexer.size(); i++) {
        BattingLineup lineup = indexer.getLineup(i);
        Pair<Long, BattingLineup> comparisonPair = indexer.getRandomNeighbor(indexer.getIndex(lineup));
        BattingLineup comparisonLineup = comparisonPair.getSecond();
        assertNotEquals("Random neighbor returned the exact same lineup " + lineupType, lineup, comparisonLineup);
      }
    }
  }
}
