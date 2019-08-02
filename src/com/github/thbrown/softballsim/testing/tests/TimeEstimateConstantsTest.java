package com.github.thbrown.softballsim.testing.tests;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.github.thbrown.softballsim.CombinatoricsUtil;
import com.github.thbrown.softballsim.StatsUtil;
import com.github.thbrown.softballsim.testing.helpers.MonteCarloSimulationDataBuilder;
import com.github.thbrown.softballsim.testing.helpers.PlayerBuilder;
import com.github.thbrown.softballsim.testing.helpers.ServerMethods;
import com.github.thbrown.softballsim.testing.helpers.TestServer;

/**
 * This isn't a classical unit test.
 * 
 * It's used to generate the constants required by clients that wish to estimate
 * the expected time of their simulations based of changing simulation parameters.
 */
public class TimeEstimateConstantsTest {

  @Test
  public void singleSituation() {
    MonteCarloSimulationDataBuilder mcsdb = new MonteCarloSimulationDataBuilder()
        .withInnings(7)
        .withIterations(10000)
        .withLineupType(1);
   
    final int playerCount = 6;
    PlayerBuilder[] players = new PlayerBuilder[playerCount];
    
    /*
    for (int k = 0; k < playerCount; k++) {
      players[k] = new PlayerBuilder().withId("player" + k).withOuts(10).withHomeruns(17);
    }
    //*/
    
    ///*
    players[0] = new PlayerBuilder().withId("player" + 0).withOuts(11).withSingles(13).withDoubles(5).withTriples(3).withHomeruns(1);
    players[1] = new PlayerBuilder().withId("player" + 1).withOuts(11).withSingles(2).withDoubles(8).withTriples(1).withHomeruns(4);
    players[2] = new PlayerBuilder().withId("player" + 2).withOuts(8).withSingles(12).withDoubles(2).withTriples(2).withHomeruns(0);
    players[3] = new PlayerBuilder().withId("player" + 3).withOuts(13).withSingles(8).withDoubles(0).withTriples(0).withHomeruns(0);
    players[4] = new PlayerBuilder().withId("player" + 4).withOuts(13).withSingles(11).withDoubles(7).withTriples(3).withHomeruns(0);
    players[5] = new PlayerBuilder().withId("player" + 5).withOuts(8).withSingles(20).withDoubles(6).withTriples(1).withHomeruns(1);
    //*/
    
    mcsdb.withPlayers(players);

    TestServer.runSimulationOverNetwork(new ServerMethods() {
      @Override
      public void onReady(PrintWriter out) {
        String json;
        json = mcsdb.toString();
        json = json.replace("\n", "").replace("\r", "");
        out.println(json);
      }

      @Override
      public void onComplete(Map<String, String> data) throws IOException {}
    });

  }

  @Test
  public void standardDeviationAndTeamAverage() throws IOException {
    
    String logFileName = "stdDevAndAvg.log";
    Files.deleteIfExists(Paths.get(logFileName));

    MonteCarloSimulationDataBuilder mcsdb = new MonteCarloSimulationDataBuilder()
        .withInnings(7)
        .withIterations(100000)
        .withLineupType(1)
        .withThreadCount(1);

    final int PLAYER_COUNT = 6;
    final int PA_COUNT = 5;
    final int RUNS_MAX = PLAYER_COUNT * PA_COUNT;

    final int NUM_AVERAGES = 100;
    final int NUM_STD_DEV = 100;

    List<List<List<Integer>>> allDistributions = CombinatoricsUtil.getPartitions(RUNS_MAX, PA_COUNT, PLAYER_COUNT);
    List<List<Integer>> emptyList = new ArrayList<>();
    emptyList.add(Collections.emptyList());
    allDistributions.add(emptyList);
    
    // Iterate over different averages and standard deviations to get a feel for
    // how they affect simulation time
    int AVG_ITERATIONS = Math.min(NUM_AVERAGES, RUNS_MAX);
    for (int i = 0; i < AVG_ITERATIONS; i++) {
      int numRuns = transform(i,0,AVG_ITERATIONS,0,RUNS_MAX);
      List<List<Integer>> targetDistribution = allDistributions.get(numRuns);

      // Pad with zeros and sort by std dev
      for (int j = 0; j < targetDistribution.size(); j++) {
        while (targetDistribution.get(j).size() < PLAYER_COUNT) {
          targetDistribution.get(j).add(0);
        }
      }
      Collections.sort(targetDistribution, StatsUtil.getStdDevComparator());
      
      int STD_DEV_ITERATIONS = Math.min(NUM_STD_DEV, targetDistribution.size());
      for (int j = 0; j < STD_DEV_ITERATIONS; j++) {
        int stdDevIndex = transform(j,0,STD_DEV_ITERATIONS,0,targetDistribution.size());

        final double avg = (double) numRuns / (double) RUNS_MAX;
        System.out.println("Hit count: " + numRuns + " " + avg);
        final double stedev = StatsUtil.stdev(targetDistribution.get(stdDevIndex));
        System.out.println(
            "Distribution index: " + targetDistribution.get(stdDevIndex) + " " + stedev);

        PlayerBuilder[] players = new PlayerBuilder[PLAYER_COUNT];
        for (int k = 0; k < PLAYER_COUNT; k++) {
          players[k] = new PlayerBuilder()
              .withId("player" + k)
              .withOuts(PA_COUNT - targetDistribution.get(stdDevIndex).get(k))
              .withSingles(targetDistribution.get(stdDevIndex).get(k));
        }
        mcsdb.withPlayers(players);
        
        TestServer.runSimulationOverNetwork(new ServerMethods() {
          @Override
          public void onReady(PrintWriter out) {
            String json;
            json = mcsdb.toString();
            json = json.replace("\n", "").replace("\r", "");
            out.println(json);
          }

          @Override
          public void onComplete(Map<String, String> data) throws IOException {
            PrintWriter writer = null;
            try {
              writer = new PrintWriter(new FileWriter(logFileName, true), true);
              writer.println("DATA: " + String.valueOf(data.get("elapsedTimeMs")) + " " + avg + " " + stedev);
            } finally {
              writer.close();
            }

            System.out.println("DATA: " + String.valueOf(data.get("elapsedTimeMs")) + " " + avg + " " + stedev );
            System.out.println(data);
          }
        });
      }
    }
  }
  
  @Test
  public void lineupCounts() throws IOException {
    String logFileName = "lineupCount.log";
    Files.deleteIfExists(Paths.get(logFileName));
    
    MonteCarloSimulationDataBuilder mcsdb = new MonteCarloSimulationDataBuilder()
        .withInnings(7)
        .withIterations(1000)
        .withLineupType(1)
        .withThreadCount(1);

    for (int lineupType = 1; lineupType <= 3; lineupType++) {
      mcsdb.withLineupType(lineupType);

      for (int playerCount = 5; playerCount <= 9; playerCount++) {

        PlayerBuilder[] players = new PlayerBuilder[playerCount];
        for (int k = 0; k < playerCount; k++) {
          players[k] = new PlayerBuilder().withId("player" + k).withGender(k%2 == 0 ? "M" : "F").withOuts(5).withSingles(5);
        }

        mcsdb.withPlayers(players);

        final int lineupTypeFinal = lineupType;

        TestServer.runSimulationOverNetwork(new ServerMethods() {
          @Override
          public void onReady(PrintWriter out) {
            String json;
            json = mcsdb.toString();
            json = json.replace("\n", "").replace("\r", "");
            out.println(json);
          }

          @Override
          public void onComplete(Map<String, String> data) throws IOException {
            PrintWriter writer = null;
            try {
              writer = new PrintWriter(new FileWriter(logFileName, true), true);
              writer.println("DATA: " + String.valueOf(data.get("elapsedTimeMs")) + " " + lineupTypeFinal + " "
                  + String.valueOf(data.get("total")));
            } finally {
              writer.close();
            }

            System.out.println("DATA: " + String.valueOf(data.get("elapsedTimeMs")) + " " + lineupTypeFinal + " "
                + String.valueOf(data.get("total")));
            System.out.println(data);
          }
        });
      }
    }
  }

  @Test
  public void threadCount() throws IOException {
    String logFileName = "threadCount.log";
    Files.deleteIfExists(Paths.get(logFileName));

    MonteCarloSimulationDataBuilder mcsdb = new MonteCarloSimulationDataBuilder()
        .withInnings(7)
        .withIterations(1000)
        .withLineupType(1);
    
    final int playerCount = 8;
    PlayerBuilder[] players = new PlayerBuilder[playerCount];
    for (int k = 0; k < playerCount; k++) {
      players[k] = new PlayerBuilder().withId("player" + k).withOuts(5).withSingles(5);
    }
    mcsdb.withPlayers(players);

    for (int threadCount = 1; threadCount <= 20; threadCount++) {
      mcsdb.withThreadCount(threadCount);
      
      final int threadCountFinal = threadCount;

      TestServer.runSimulationOverNetwork(new ServerMethods() {
        @Override
        public void onReady(PrintWriter out) {
          String json;
          json = mcsdb.toString();
          json = json.replace("\n", "").replace("\r", "");
          out.println(json);
        }

        @Override
        public void onComplete(Map<String, String> data) throws IOException {
          PrintWriter writer = null;
          try {
            writer = new PrintWriter(new FileWriter(logFileName, true), true);
            writer.println("DATA: " + String.valueOf(data.get("elapsedTimeMs")) + " " + threadCountFinal);
          } finally {
            writer.close();
          }

          System.out.println("DATA: " + String.valueOf(data.get("elapsedTimeMs")) + " " + threadCountFinal);
          System.out.println(data);
        }
      });
    }

  }
  
  /**
   * Takes a number (value) between oldMin and oldMax and maps linearly to a new value between
   * newMin and newMax
   */
  private int transform(int value, int oldMin, int oldMax, int newMin, int newMax) {
    double x = (double) value;
    double a = (double) oldMin;
    double b = (double) oldMax;
    double c = (double) newMin;
    double d = (double) newMax;
    return (int)((x - a)*(d - c)/(b - a) + c);
  }

}
