package com.github.thbrown.softballsim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.StandardBattingLineup;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.*;
import com.github.thbrown.softballsim.lineupindexer.*;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.HitGenerator;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloGameSimulation;
import com.github.thbrown.softballsim.datasource.*;
import com.github.thbrown.softballsim.helpers.LineupTypeTestInfo;
import org.apache.commons.cli.*;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.apache.commons.math3.util.Pair;

/**
 * This test is intended to evaluate the performance of optimizers for comparison.
 * 
 * This test generates a file named optimizers.tsv w/ optimizer info.
 * 
 * It takes a long time to run so it's @test annotation is commented out by default.
 */
public class OptimizerTest {

  // @Test
  public void generateOptimizerFile() throws Exception {
    // Get data from file system
    CommandLineOptions commandLineOptions = CommandLineOptions.getInstance();
    Options commonOptions = commandLineOptions.getOptionsForFlags(DataSourceEnum.FILE_SYSTEM, null);
    CommandLine commonCmd = commandLineOptions.parse(commonOptions, new String[0], true);
    DataStats stats = DataSourceEnum.FILE_SYSTEM.getData(commonCmd);

    List<String> results = new ArrayList<>();

    for (OptimizerEnum optimizer : OptimizerEnum.values()) {
      Logger.log(optimizer.name());

      // TODO: skip hidden optimizers
      if (optimizer == OptimizerEnum.EXPECTED_VALUE) {
        continue;
      }

      for (LineupTypeEnum lineupType : LineupTypeEnum.values()) {
        Logger.log("\t" + lineupType.name());

        // Test
        final int MIN_TEST = 6;
        final int MAX_TEST = 10;
        for (int i = MIN_TEST; i <= MAX_TEST; i++) {
          Logger.log("\t\tPlayers: " + i);
          List<String> players = LineupTypeTestInfo.getInstance().getTestLineup(lineupType, i, stats);

          String[] args = {"-o", optimizer.name(), "-f", "-l", String.join(",", players), "-t", lineupType.name()};
          Result result = SoftballSim.mainInternal(args);
          Logger.log("\t\t\t" + result.getElapsedTimeMs());

          // Run the lineup through monte carlo for 1M iterations so we have fair
          // comparison
          HitGenerator hitGenerator = new HitGenerator(result.getLineup().asList());
          final double COUNT = 1000000;
          double sum = 0;
          for (int g = 0; g < COUNT; g++) {
            sum += MonteCarloGameSimulation.simulateGame(result.getLineup(), 7, hitGenerator);
          }
          double estimatedScore = sum / COUNT;

          // Save the results in memory
          results.add(optimizer + "\t" + lineupType + "\t" + i + "\t" + result.getElapsedTimeMs() + "\t"
              + estimatedScore);

          // TODO: add estimate accuracy metric?
        }

      }
    }

    // Log results to the console
    for (String s : results) {
      Logger.log(s);
    }

    // Also write them to the filesystem
    try {
      File myObj = new File("optimizers.tsv");
      if (myObj.createNewFile()) {
        System.out.println("File created: " + myObj.getName());
      } else {
        System.out.println("File already exists.");
      }
      FileWriter myWriter = new FileWriter("optimizers.tsv");
      for (String s : results) {
        myWriter.write(s);
        myWriter.write("\n");
      }
      myWriter.close();
      System.out.println("Successfully wrote to the file.");
    } catch (IOException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
  }

}
