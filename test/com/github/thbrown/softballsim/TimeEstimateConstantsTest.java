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
  
  public final String CONFIG_FILE = "estimated-time-config.js";

  /**
   * This test generates the time estimation constants and saves them, serialized as JSON to a 
   * file named estimatedTimeConfig.json
   */
  @Test
  public void generateTimeEstimationConfig() throws IOException {

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

      final int NUM_AVERAGES = 100;
      final int NUM_STD_DEV = 100;

      final int POLYNOMIAL_REGRESSION_DEGREE = 10;

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
          }, false);
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
      errorsConstants[0] = 1.0; // Value doesn't matter, this is just to make the threadCount = index in the array
      Map<String, Long> wrapper = new HashMap<>();

      for (int threadCount = 1; threadCount < coresToTest; threadCount++) {
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
            
            if(threadCountFinal == 1) {
              wrapper.put("singleThreadTime", data.getElapsedTimeMs());
            }

            long linearImprovementTime = wrapper.get("singleThreadTime") / threadCountFinal;
            errorsConstants[threadCountFinal] = (((double) (data.getElapsedTimeMs()) / (double) linearImprovementTime));

            appendToFile(logFileName, "DATA: " + String.valueOf(data.getElapsedTimeMs()) + " " + threadCountFinal);

            System.out.println("DATA: " + String.valueOf(data.getElapsedTimeMs()) + " " + threadCountFinal + " "
                + (errorsConstants[threadCountFinal]) * linearImprovementTime);
            System.out.println(data);
            return true;
          }
        }, false);
        
        lineupCount = wrapper.get("lineupCount");
      }

      appendToFile(logFileName, "Error Coefficients");
      appendToFile(logFileName, Arrays.toString(errorsConstants));

      System.out.println("Error Coefficients");
      System.out.println(Arrays.toString(errorsConstants));
    }

    // Save the config information to a file
    final String configFileName = CONFIG_FILE;
    Files.deleteIfExists(Paths.get(configFileName));
    TimeEstimationConfig config = new TimeEstimationConfig();
    config.setInnings(INNINGS);
    config.setIterations(ITERATIONS);
    config.setThreads(THREAD_COUNT);
    config.setLineupType(LINEUP_TYPE);
    config.setCoefficients(coeff);
    config.setLineupCount(lineupCount);
    config.setErrorAdjustments(errorsConstants);
    Gson g = new GsonBuilder().setPrettyPrinting().create();
    appendToFile(configFileName, "exports.config = JSON.parse(`" + g.toJson(config) + "`)");
  }
  

  @Test
  public void printPolynomialEquation() throws Exception {
    String json = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
    GsonBuilder gsonBldr = new GsonBuilder();
    TimeEstimationConfig config = gsonBldr.create().fromJson(json, TimeEstimationConfig.class);
    System.out.println(config.getPolynomailEquation());
  }
  
  /**
   * Run this test after the configs have been generated to evaluate how closely they reflect the actual runtime
   */
  @Test
  public void testTimeEstimationConfig() throws Exception {
    
    String json = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
    
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

    final int PLAYER_COUNT = 8;
    Player[] players = new Player[PLAYER_COUNT];

    /*
    for (int k = 0; k < PLAYER_COUNT; k++) { 
      players[k] = new Player.Builder("player" + k).outs(k).singles(PLAYER_COUNT-k).build(); 
    } 
    */
    
    ///*
    for (int k = 0; k < PLAYER_COUNT; k++) { 
      players[k] = new Player.Builder("player" + k).outs(5).singles(5).build(); 
    } 
    //*/

    /*
    players[0] = new Player.Builder("player" + 0).gender("F").outs(34).singles(26).doubles(0).triples(0).homeruns(0).build();
    players[1] = new Player.Builder("player" + 1).gender("F").outs(40).singles(13).doubles(3).triples(0).homeruns(0).build();
    players[2] = new Player.Builder("player" + 2).gender("F").outs(22).singles(22).doubles(0).triples(1).homeruns(0).build();
    players[3] = new Player.Builder("player" + 3).gender("F").outs(16).singles(4).doubles(1).triples(0).homeruns(0).build();
    players[4] = new Player.Builder("player" + 4).gender("M").outs(23).singles(14).doubles(6).triples(3).homeruns(6).build();
    players[5] = new Player.Builder("player" + 5).gender("M").outs(20).singles(22).doubles(9).triples(4).homeruns(6).build();
    players[6] = new Player.Builder("player" + 6).gender("M").outs(23).singles(22).doubles(8).triples(4).homeruns(2).build();
    players[7] = new Player.Builder("player" + 7).gender("M").outs(11).singles(9).doubles(1).triples(1).homeruns(1).build();
    players[7] = new Player.Builder("player" + 8).gender("M").outs(10).singles(7).doubles(5).triples(1).homeruns(1).build();
    players[7] = new Player.Builder("player" + 9).gender("F").outs(9).singles(1).doubles(0).triples(0).homeruns(0).build();
    */
    
    mcsdb.withPlayers(players);
    
    // Determine team avg
    double teamHits = 0;
    double teamOuts = 0;
    for(int i = 0; i < players.length; i++) {
      teamHits = teamHits + players[i].getSingles() + players[i].getDoubles() + players[i].getTriples() + players[i].getHomeruns();
      teamOuts = teamOuts + players[i].getOuts();
    }
    double teamAverage = teamHits / (teamHits + teamOuts);
    
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
    }, false);
    

    long elapsedTime = wrapper.get("elapsedTime");
    long lineupCount = wrapper.get("lineupCount");
    long estimatedTime = config.estimateSimulationTimeInMillis(THREAD_COUNT, teamAverage, INNINGS, ITERATIONS, LINEUP_TYPE, lineupCount);

    System.out.println("Estimated " + estimatedTime + " Actual " + elapsedTime + " Error: " + percentChange(estimatedTime,elapsedTime) + "%");

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

}
