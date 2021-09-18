package com.github.thbrown.softballsim;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.StandardBattingLineup;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.HitGenerator;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloMultiGameSimulationTask;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.TaskResult;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import org.junit.Test;

public class SingleLineupTest {

  @Test
  public void simulateSingleGame() {

    // Get stats from file
    String statsFileLocation = "./stats/exampleData.json";
    String json = null;
    File file = new File(statsFileLocation);
    try {
      if (file.isDirectory()) {
        File[] filesInDirectory = file.listFiles();
        List<File> filesOnly = Arrays.stream(filesInDirectory).filter(f -> f.isFile()).collect(Collectors.toList());
        if (filesOnly.size() == 1) {
          json = new String(Files.readAllBytes(Paths.get(filesOnly.get(0).getCanonicalPath())));
        } else {
          throw new RuntimeException(
              "There were " + filesOnly.size() + " files in the stats-path directory specified ("
                  + file.getAbsolutePath()
                  + "), but this application expects only one, or a path direclty to the stats file");
        }
      } else {
        json = new String(Files.readAllBytes(Paths.get(statsFileLocation)));
      }
    } catch (IOException e) {
      throw new RuntimeException(Msg.BAD_PATH.args(statsFileLocation), e);
    }
    DataStats stats = GsonAccessor.getInstance().getCustom().fromJson(json, DataStats.class);

    // Build the lineup
    List<DataPlayer> playerList = new ArrayList<>();

    playerList.add(stats.getPlayerById("00000000000002"));
    playerList.add(stats.getPlayerById("0000000000000y"));
    playerList.add(stats.getPlayerById("00000000000001"));
    playerList.add(stats.getPlayerById("00000000000003"));
    playerList.add(stats.getPlayerById("0000000000000f"));
    playerList.add(stats.getPlayerById("00000000000004"));

    // Simulate a games
    final int GAMES = 1000000;
    BattingLineup lineup = new StandardBattingLineup(playerList);
    HitGenerator hitGenerator = new HitGenerator(playerList);

    // Run on the current thread
    MonteCarloMultiGameSimulationTask task =
        new MonteCarloMultiGameSimulationTask(lineup, GAMES, 7, hitGenerator);
    TaskResult result = task.run();
    Logger.log(result.getScore());
    Logger.log(result.getLineup());

  }

}
