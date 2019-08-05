package com.github.thbrown.softballsim;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.junit.Test;

import com.github.thbrown.softballsim.CombinatoricsUtil;
import com.github.thbrown.softballsim.StatsUtil;
import com.github.thbrown.softballsim.commands.CompleteOptimizationCommand;
import com.github.thbrown.softballsim.commands.ReadyOptimizationCommand;
import com.github.thbrown.softballsim.helpers.MonteCarloSimulationDataBuilder;
import com.github.thbrown.softballsim.helpers.ProcessHooks;
import com.github.thbrown.softballsim.helpers.TestServer;

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
    Player[] players = new Player[playerCount];
    
    /*
    for (int k = 0; k < playerCount; k++) {
      players[k] = new PlayerBuilder().withId("player" + k).outs(10).homeruns(17);
    }
    //*/
    
    ///*
    players[0] = new Player.Builder("player" + 0).outs(11).singles(13).doubles(5).triples(3).homeruns(1).build();
    players[1] = new Player.Builder("player" + 1).outs(11).singles(2).doubles(8).triples(1).homeruns(4).build();
    players[2] = new Player.Builder("player" + 2).outs(8).singles(12).doubles(2).triples(2).homeruns(0).build();
    players[3] = new Player.Builder("player" + 3).outs(13).singles(8).doubles(0).triples(0).homeruns(0).build();
    players[4] = new Player.Builder("player" + 4).outs(13).singles(11).doubles(7).triples(3).homeruns(0).build();
    players[5] = new Player.Builder("player" + 5).outs(8).singles(20).doubles(6).triples(1).homeruns(1).build();
    //*/
    
    mcsdb.withPlayers(players);

    TestServer.runSimulationOverNetwork(new ProcessHooks() {
      @Override
      public boolean onReady(ReadyOptimizationCommand data, PrintWriter out) {
        String json;
        json = mcsdb.toString();
        json = json.replace("\n", "").replace("\r", "");
        out.println(json);
        return false;
      }
    });

  }

  @Test
  public void standardDeviationAndTeamAverage() throws IOException {
    
    String logFileName = "stdDevAndAvg.log";
    Files.deleteIfExists(Paths.get(logFileName));

    MonteCarloSimulationDataBuilder mcsdb = new MonteCarloSimulationDataBuilder()
        .withInnings(7)
        .withIterations(10000)
        .withLineupType(1)
        .withThreadCount(1);

    final int PLAYER_COUNT = 6;
    final int PA_COUNT = 5;
    final int RUNS_MAX = PLAYER_COUNT * PA_COUNT;

    final int NUM_AVERAGES = 100;
    final int NUM_STD_DEV = 100;

    final int POLYNOMIAL_REGRESSION_DEGREE = 8;

    List<List<List<Integer>>> allDistributions = CombinatoricsUtil.getPartitions(RUNS_MAX, PA_COUNT, PLAYER_COUNT);
    List<List<Integer>> emptyList = new ArrayList<>();
    emptyList.add(Collections.emptyList());
    allDistributions.add(emptyList);

    // Collect data.
    final WeightedObservedPoints obs = new WeightedObservedPoints();
    
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

        Player[] players = new Player[PLAYER_COUNT];
        for (int k = 0; k < PLAYER_COUNT; k++) {
          players[k] = new Player.Builder("player" + k)
              .outs(PA_COUNT - targetDistribution.get(stdDevIndex).get(k))
              .singles(targetDistribution.get(stdDevIndex).get(k)).build();
        }
        mcsdb.withPlayers(players);
        
        TestServer.runSimulationOverNetwork(new ProcessHooks() {
          @Override
          public boolean onReady(ReadyOptimizationCommand data, PrintWriter out) {
            String json;
            json = mcsdb.toString();
            json = json.replace("\n", "").replace("\r", "");
            out.println(json);
            return false;
          }

          @Override
          public boolean onComplete(CompleteOptimizationCommand data, PrintWriter out) throws IOException {
            obs.add(avg, data.getElapsedTimeMs());
            appendToFile(logFileName,"DATA: " + data.getElapsedTimeMs() + " " + avg + " " + stedev);

            System.out.println("DATA: " + String.valueOf(data.getElapsedTimeMs()) + " " + avg + " " + stedev );
            System.out.println(data);
            return true;
          }
        });
      }
    }

    // Retrieve fitted parameters (coefficients of the polynomial function).
    final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(POLYNOMIAL_REGRESSION_DEGREE);
    final double[] coeff = fitter.fit(obs.toList());
    System.out.println("Coefficients (" + POLYNOMIAL_REGRESSION_DEGREE + " degree polynomial)");
    System.out.println(Arrays.toString(coeff));
    
    appendToFile(logFileName,"Coefficients (" + POLYNOMIAL_REGRESSION_DEGREE + " degree polynomial)");
    appendToFile(logFileName,Arrays.toString(coeff));
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

        Player[] players = new Player[playerCount];
        for (int k = 0; k < playerCount; k++) {
          players[k] = new Player.Builder("player" + k).gender(k%2 == 0 ? "M" : "F").outs(5).singles(5).build();
        }

        mcsdb.withPlayers(players);

        final int lineupTypeFinal = lineupType;

        TestServer.runSimulationOverNetwork(new ProcessHooks() {
          @Override
          public boolean onReady(ReadyOptimizationCommand data, PrintWriter out) {
            String json;
            json = mcsdb.toString();
            json = json.replace("\n", "").replace("\r", "");
            out.println(json);
            return false;
          }

          @Override
          public boolean onComplete(CompleteOptimizationCommand data, PrintWriter out) throws IOException {
            appendToFile(logFileName,"DATA: " + data.getElapsedTimeMs() + " " + lineupTypeFinal + " "
                + String.valueOf(data.getTotal()));

            System.out.println("DATA: " + String.valueOf(data.getElapsedTimeMs()) + " " + lineupTypeFinal + " "
                + String.valueOf(data.getTotal()));
            System.out.println(data);
            return true;
          }
        });
      }
    }
  }

  @Test
  public void threadCount() throws IOException {
    final int coresToTest = Runtime.getRuntime().availableProcessors() + 5;
    final String logFileName = "threadCount.log";
    Files.deleteIfExists(Paths.get(logFileName));

    MonteCarloSimulationDataBuilder mcsdb = new MonteCarloSimulationDataBuilder()
        .withInnings(7)
        .withIterations(100)
        .withLineupType(1);
    
    final int playerCount = 8;
    Player[] players = new Player[playerCount];
    for (int k = 0; k < playerCount; k++) {
      players[k] = new Player.Builder("player" + k).outs(5).singles(5).build();
    }
    mcsdb.withPlayers(players);
    
    Double[] errorsConstants = new Double[coresToTest];
    List<Long> wrapper = new ArrayList<>();

    for (int threadCount = 1; threadCount <= coresToTest; threadCount++) {
      mcsdb.withThreadCount(threadCount);
      
      final int threadCountFinal = threadCount;

      TestServer.runSimulationOverNetwork(new ProcessHooks() {
        @Override
        public boolean onReady(ReadyOptimizationCommand data, PrintWriter out) {
          String json;
          json = mcsdb.toString();
          json = json.replace("\n", "").replace("\r", "");
          out.println(json);
          return false;
        }

        @Override
        public boolean onComplete(CompleteOptimizationCommand data, PrintWriter out) throws IOException {
          if(wrapper.isEmpty()) {
            wrapper.add(data.getElapsedTimeMs());
          }
          long linearImprovementTime = wrapper.get(0) / threadCountFinal;
          errorsConstants[threadCountFinal - 1] = (((double)(data.getElapsedTimeMs()) / (double)linearImprovementTime));
          
          appendToFile(logFileName,"DATA: " + String.valueOf(data.getElapsedTimeMs()) + " " + threadCountFinal);

          System.out.println("DATA: " + String.valueOf(data.getElapsedTimeMs()) + " " + threadCountFinal + " " + (errorsConstants[threadCountFinal - 1])*linearImprovementTime);
          System.out.println(data);
          return true;
        }
      });
    }
    
    appendToFile(logFileName,"Error Coefficients");
    appendToFile(logFileName,Arrays.toString(errorsConstants));
    
    System.out.println("Error Coefficients");
    System.out.println(Arrays.toString(errorsConstants));
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
  
  private void appendToFile(String fileName, String toAppend) throws IOException {
    PrintWriter writer = null;
    try {
      writer = new PrintWriter(new FileWriter(fileName, true), true);
      writer.println(toAppend);
    } finally {
      writer.close();
    }
  }

}
