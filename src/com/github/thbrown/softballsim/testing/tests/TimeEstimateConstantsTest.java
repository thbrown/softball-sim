package com.github.thbrown.softballsim.testing.tests;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.github.thbrown.softballsim.CombinatoricsUtil;
import com.github.thbrown.softballsim.Logger;
import com.github.thbrown.softballsim.SoftballSim;
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
  public void standardDeviationAndTeamAverage() throws IOException {

    MonteCarloSimulationDataBuilder mcsdb = new MonteCarloSimulationDataBuilder()
        .withInnings(7)
        .withIterations(100000)
        .withLineupType(1);

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

        Logger.log(mcsdb.toString());
        
        TestServer.runSimulationOverNetwork(new ServerMethods() {
          @Override
          public void onReady(PrintWriter out) {
            String json;
            try {
              json = mcsdb.toString();

              json = json.replace("\n", "").replace("\r", "");
              out.println(json);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          @Override
          public void onComplete(Map<String, String> data) {
            // TODO Auto-generated method stub
            PrintWriter writer = null;
            try {
              writer = new PrintWriter(new FileWriter("standardDev.log", true), true);
              writer.println("DATA: " + String.valueOf(data.get("elapsedTimeMs")) + " " + avg + " " + stedev );
            } catch (IOException e) {
              e.printStackTrace();
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
