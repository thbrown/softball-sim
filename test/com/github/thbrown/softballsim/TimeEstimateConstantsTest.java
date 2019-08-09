package com.github.thbrown.softballsim;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.junit.Test;

import com.github.thbrown.softballsim.commands.CompleteOptimizationCommand;
import com.github.thbrown.softballsim.commands.ReadyOptimizationCommand;
import com.github.thbrown.softballsim.helpers.MonteCarloSimulationDataBuilder;
import com.github.thbrown.softballsim.helpers.ProcessHooks;
import com.github.thbrown.softballsim.helpers.TestServer;
import com.github.thbrown.softballsim.helpers.TimeEstimationConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This isn't a classical unit test.
 * 
 * It's used to generate and test the constants required by clients that wish to estimate
 * the expected time of their simulations based of changing simulation
 * parameters.
 */
public class TimeEstimateConstantsTest {

  /**
   * Run this test after the configs have been generated to evaluate how closely they reflect the actual runtime
   */
  @Test
  public void testTimeEstimationConfig() throws Exception {
    
    String json = new String(Files.readAllBytes(Paths.get("./estimatedTimeConfig.json")));
    
    GsonBuilder gsonBldr = new GsonBuilder();
    TimeEstimationConfig config = gsonBldr.create().fromJson(json, TimeEstimationConfig.class);
    
    final int INNINGS = 7;
    final int ITERATIONS = 100;
    final int LINEUP_TYPE = 1;
    final int THREAD_COUNT = 1;
    
    MonteCarloSimulationDataBuilder mcsdb = new MonteCarloSimulationDataBuilder()
        .withInnings(INNINGS)
        .withIterations(ITERATIONS)
        .withLineupType(LINEUP_TYPE)
        .withThreadCount(THREAD_COUNT);

    final int playerCount = 6;
    Player[] players = new Player[playerCount];

    /*
     * for (int k = 0; k < playerCount; k++) { players[k] = new
     * PlayerBuilder().withId("player" + k).outs(10).homeruns(17); } //
     */

    /// *
    players[0] = new Player.Builder("player" + 0).outs(11).singles(13).doubles(5).triples(3).homeruns(1).build();
    players[1] = new Player.Builder("player" + 1).outs(11).singles(2).doubles(8).triples(1).homeruns(4).build();
    players[2] = new Player.Builder("player" + 2).outs(8).singles(12).doubles(2).triples(2).homeruns(0).build();
    players[3] = new Player.Builder("player" + 3).outs(13).singles(8).doubles(0).triples(0).homeruns(0).build();
    players[4] = new Player.Builder("player" + 4).outs(13).singles(11).doubles(7).triples(3).homeruns(0).build();
    players[5] = new Player.Builder("player" + 5).outs(8).singles(20).doubles(6).triples(1).homeruns(1).build();
    // */
    
    
    // Determine team avg
    double teamHits = 0;
    double teamOuts = 0;
    for(int i = 0; i < players.length; i++) {
      teamHits = players[i].getSingles() + players[i].getDoubles() + players[i].getTriples() + players[i].getHomeruns();
      teamOuts = players[i].getOuts();
    }
    double teamAverage = teamHits / (teamHits + teamOuts);
    
    // Determine lineupCount
    mcsdb.withPlayers(players);
    

    final Map<String, Long> wrapper = new HashMap<>();
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
      public boolean onComplete(CompleteOptimizationCommand data, PrintWriter out) throws Exception {
        wrapper.put("elapsedTime", data.getElapsedTimeMs());
        wrapper.put("lineupCount", data.getComplete());
        return true;
      }
    });
    

    long elapsedTime = wrapper.get("elapsedTime");
    long lineupCount = wrapper.get("lineupCount");
    long estimatedTime = config.estimateSimulationTimeInMillis(THREAD_COUNT, teamAverage, INNINGS, ITERATIONS, LINEUP_TYPE, lineupCount);

   System.out.println("Estimated" + estimatedTime + " Actual " + elapsedTime + " Error: " + percentChange(estimatedTime,elapsedTime) );

  }

  /**
   * This test generates the time estimation constants and saves them, serialized as JSON to a 
   * file named estimatedTimeConfig.json
   */
  @Test
  public void generateTimeEstimationConfig() throws IOException {

    final String configFileName = "estimatedTimeConfig.json";
    Files.deleteIfExists(Paths.get(configFileName));

    final int INNINGS = 7;
    final int ITERATIONS = 100;
    final int LINEUP_TYPE = 1;
    final int THREAD_COUNT = 1;
    Long lineupCount = null;

    /**
     * Calculate the coefficents for the polynomial regression line
     */
    double[] coeff = null;
    {
      final String logFileName = "stdDevAndAvg.log";
      Files.deleteIfExists(Paths.get(logFileName));
      
      MonteCarloSimulationDataBuilder mcsdb = new MonteCarloSimulationDataBuilder().withInnings(INNINGS)
          .withIterations(ITERATIONS).withLineupType(LINEUP_TYPE).withThreadCount(THREAD_COUNT);

      final int PLAYER_COUNT = 8;
      final int PA_COUNT = 4;
      final int RUNS_MAX = PLAYER_COUNT * PA_COUNT;

      final int NUM_AVERAGES = 50;
      final int NUM_STD_DEV = 50;

      final int POLYNOMIAL_REGRESSION_DEGREE = 8;

      List<List<List<Integer>>> allDistributions = CombinatoricsUtil.getPartitions(RUNS_MAX, PA_COUNT, PLAYER_COUNT);
      List<List<Integer>> emptyList = new ArrayList<>();
      emptyList.add(Collections.emptyList());
      allDistributions.add(emptyList);

      // Collect data.
      final WeightedObservedPoints obs = new WeightedObservedPoints();

      // Iterate over different averages and standard deviations to get a feel
      // for
      // how they affect simulation time
      int AVG_ITERATIONS = Math.min(NUM_AVERAGES, RUNS_MAX);
      for (int i = 0; i < AVG_ITERATIONS; i++) {
        int numRuns = transform(i, 0, AVG_ITERATIONS, 0, RUNS_MAX);
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
          int stdDevIndex = transform(j, 0, STD_DEV_ITERATIONS, 0, targetDistribution.size());

          final double avg = (double) numRuns / (double) RUNS_MAX;
          System.out.println("Hit count: " + numRuns + " " + avg);
          final double stedev = StatsUtil.stdev(targetDistribution.get(stdDevIndex));
          System.out.println("Distribution index: " + targetDistribution.get(stdDevIndex) + " " + stedev);

          Player[] players = new Player[PLAYER_COUNT];
          for (int k = 0; k < PLAYER_COUNT; k++) {
            players[k] = new Player.Builder("player" + k).outs(PA_COUNT - targetDistribution.get(stdDevIndex).get(k))
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
              appendToFile(logFileName, "DATA: " + data.getElapsedTimeMs() + " " + avg + " " + stedev);

              System.out.println("DATA: " + String.valueOf(data.getElapsedTimeMs()) + " " + avg + " " + stedev);
              System.out.println(data);
              return true;
            }
          });
        }
      }

      // Retrieve fitted parameters (coefficients of the polynomial function).
      final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(POLYNOMIAL_REGRESSION_DEGREE);
      coeff = fitter.fit(obs.toList());
      System.out.println("Coefficients (" + POLYNOMIAL_REGRESSION_DEGREE + " degree polynomial)");
      System.out.println(Arrays.toString(coeff));

      appendToFile(logFileName, "Coefficients (" + POLYNOMIAL_REGRESSION_DEGREE + " degree polynomial)");
      appendToFile(logFileName, Arrays.toString(coeff));
    }

    /**
     * Calculate error for the number of threads used
     */
    double[] errorsConstants;
    {
      final String logFileName = "threadCount.log";
      Files.deleteIfExists(Paths.get(logFileName));
      
      final int coresToTest = Runtime.getRuntime().availableProcessors() + 5;

      MonteCarloSimulationDataBuilder mcsdb = new MonteCarloSimulationDataBuilder().withInnings(7).withIterations(1000)
          .withLineupType(1);

      final int playerCount = 8;
      Player[] players = new Player[playerCount];
      for (int k = 0; k < playerCount; k++) {
        players[k] = new Player.Builder("player" + k).outs(5).singles(5).build();
      }
      mcsdb.withPlayers(players);

      errorsConstants = new double[coresToTest];
      Map<String, Long> wrapper = new HashMap<>();

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
            wrapper.put("elapsedTime", data.getElapsedTimeMs());
            wrapper.put("lineupCount", data.getTotal());

            long linearImprovementTime = wrapper.get(0) / threadCountFinal;
            errorsConstants[threadCountFinal
                - 1] = (((double) (data.getElapsedTimeMs()) / (double) linearImprovementTime));

            appendToFile(logFileName, "DATA: " + String.valueOf(data.getElapsedTimeMs()) + " " + threadCountFinal);

            System.out.println("DATA: " + String.valueOf(data.getElapsedTimeMs()) + " " + threadCountFinal + " "
                + (errorsConstants[threadCountFinal - 1]) * linearImprovementTime);
            System.out.println(data);
            return true;
          }
        });
        
        lineupCount = wrapper.get("lineupCount");
      }


      appendToFile(logFileName, "Error Coefficients");
      appendToFile(logFileName, Arrays.toString(errorsConstants));

      appendToFile(logFileName, Arrays.toString(errorsConstants));

      System.out.println("Error Coefficients");
      System.out.println(Arrays.toString(errorsConstants));
    }

    // Save the config information to a file
    TimeEstimationConfig config = new TimeEstimationConfig();
    config.setInnings(INNINGS);
    config.setIterations(ITERATIONS);
    config.setThreads(THREAD_COUNT);
    config.setLineupType(LINEUP_TYPE);
    config.setCoefficients(coeff);
    config.setLineupCount(lineupCount);
    config.setErrorAdjustments(errorsConstants);
    Gson g = new Gson();
    appendToFile(configFileName, g.toJson(config));
  }

  /**
   * Takes a number (value) between oldMin and oldMax and maps linearly to a new
   * value between newMin and newMax
   */
  private int transform(int value, int oldMin, int oldMax, int newMin, int newMax) {
    double x = (double) value;
    double a = (double) oldMin;
    double b = (double) oldMax;
    double c = (double) newMin;
    double d = (double) newMax;
    return (int) ((x - a) * (d - c) / (b - a) + c);
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
  
  private String percentChange(double A, double B) throws IOException {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(3);
    return nf.format(Math.abs(A - B)/((A+B)/2) * 100);
  }
  
  // TODO: move this to lineup gen classes
  private long getLineupCount(int lineupType, int maleCount, int femaleCount) {

      long numberOfLineups = 0;
      if (lineupType == 1) {
        numberOfLineups = CombinatoricsUtils.factorial(maleCount + femaleCount);
      } else if (lineupType == 2) {
        numberOfLineups =
            CombinatoricsUtil.factorial(maleCount) * CombinatoricsUtil.factorial(femaleCount);
      } else if (lineupType == 3) {
        // All three of these cases are invalid and won't be processed on the server anyways,
        if (femaleCount < 0) {
          // No females? Then it's just a normal lineup for the men
          return CombinatoricsUtils.factorial(maleCount);
        }
        if (maleCount < 0) {
          // No males? If there is one female than there is one possible lineup. If there are any other number of females there are 0 possible lineups.
          // That difference doesn't matter much for time estimation purposes.
          return 1;
        }
        if (maleCount <= femaleCount) {
          // There are no lineups where females don't bat back-to-back
          return 0;
        }

        numberOfLineups =
            CombinatoricsUtil.factorial(maleCount) *
          CombinatoricsUtil.factorial(femaleCount) *
          (CombinatoricsUtil.binomial(maleCount, femaleCount) +
              CombinatoricsUtil.binomial(maleCount - 1, femaleCount - 1));
      } else {
        throw new Error("Unrecognized lineup type " + lineupType);
      }
      return numberOfLineups;
    };

}
