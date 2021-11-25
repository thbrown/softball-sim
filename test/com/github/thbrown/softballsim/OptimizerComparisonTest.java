package com.github.thbrown.softballsim;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloExhaustiveResult;
import com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive.MonteCarloAdaptiveResult;
import com.github.thbrown.softballsim.util.Logger;

public class OptimizerComparisonTest {

  final String OUTPUT_FILE_NAME = "opt-test.log";

  final int INNINGS = 7;
  final double ALPHA = .01;
  final int LINEUP_TYPE = 2;
  final int ANNEALING_THREAD_COUNT = 1;
  final String LINEUP =
      "Keenan,Roy,Brianna,Tina,Paul,Leroy,1OiRCCmrn16iyK,Nelly,1CV6WRyspDjA7Z,1MPJ24EEyS0g6p,Devon,Jordyn,Jessica";
  // final String LINEUP =
  // "Keenan,Tina,1OiRCCmrn16iyK,Nelly,1CV6WRyspDjA7Z,1MPJ24EEyS0g6p,Devon,Jordyn,Brianna,Paul,Jessica,Leroy,Roy";
  // final String LINEUP = "Katelyn A,Thomas B,Jesse,Benjamin B,Shane W,Geoff,Lauren A,Renee R,Morgan
  // S, Jill M,Brian Z";

  /*
   * Alexa 1CV6WRyspDjA7Z 0.333 0.333 Keenan 0000000000000P 0.636 0.973 Dora 1MPJ24EEyS0g6p 0.667
   * 0.667 Nelly 1KDGsd6ikXY6iH 0.710 1.387 Tina 0000000000000n 0.080 0.080 Ivan 1OiRCCmrn16iyK 0.400
   * 0.400
   */

  final int TIMES_TO_RUN = 10;

  // @Test
  public void generatePerformanceMatrixFile() throws Exception {

    for (int lineupSize = 6; lineupSize < 13; lineupSize++) {
      String lineup = getLineupWithNBatters(lineupSize);

      // Run the simulation with 1 million exhaustive iterations, so we have a benchmark to compare to
      // String[] argsBenchmark = {"-o", "MONTE_CARLO_EXHAUSTIVE", "-l", lineup, "-G",
      // String.valueOf(1000000), "-I",
      // String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE)};
      // MonteCarloExhaustiveResult benchmark = (MonteCarloExhaustiveResult)
      // SoftballSim.mainInternal(argsBenchmark);

      // Run the simulation for max an min, so we have a benchmark to compare to
      String[] argsBenchmarkHigh = {"-o", "MONTE_CARLO_ADAPTIVE", "-l", lineup, "-I",
          String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE)};
      MonteCarloAdaptiveResult benchmarkHigh = (MonteCarloAdaptiveResult) SoftballSim.mainInternal(argsBenchmarkHigh);

      String[] argsBenchmarkLow = {"-o", "MONTE_CARLO_ADAPTIVE", "-l", lineup, "-L", "-I",
          String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE)};
      MonteCarloAdaptiveResult benchmarkLow = (MonteCarloAdaptiveResult) SoftballSim.mainInternal(argsBenchmarkLow);


      Logger.logToFile("Lineup Size: " + lineupSize + " (min: " + benchmarkLow.getLineupScore() + " max: "
          + benchmarkHigh.getLineupScore() + ")", OUTPUT_FILE_NAME);

      for (int duration = 1; duration <= 64; duration *= 2) {
        double totalRunsScored = 0;
        long totalRuntime = 0;
        List<Double> runsScored = new ArrayList<>(10);
        for (int i = 0; i < TIMES_TO_RUN; i++) {
          String[] args =
              {"-o", "MONTE_CARLO_ANNEALING", "-l", lineup, "-A", String.valueOf(ALPHA), "-I", String.valueOf(INNINGS),
                  "-t", String.valueOf(LINEUP_TYPE), "-D", String.valueOf(duration), "-f", "-v", "-T",
                  String.valueOf(ANNEALING_THREAD_COUNT)};
          Result result = SoftballSim.mainInternal(args);

          totalRuntime += result.getElapsedTimeMs();
          totalRunsScored += result.getLineupScore();
          runsScored.add(result.getLineupScore());
          System.out.println("RUNS SCORED " + result.getLineupScore());
        }
        System.out
            .println("AVG SCORED " + (totalRunsScored / (TIMES_TO_RUN)) + " VS " + benchmarkHigh.getLineupScore());

        double denominator = benchmarkHigh.getLineupScore() - benchmarkLow.getLineupScore();
        double bestAvgRunsScored = totalRunsScored / TIMES_TO_RUN;
        String prefPercentage = String.valueOf((bestAvgRunsScored - benchmarkLow.getLineupScore()) / denominator);
        // benchmark.getWorstScore() + " - " + benchmark.getLineupScore() + " " + runsScored + " " +
        // bestAvgRunsScored + " " +
        double avgRuntime = totalRuntime / TIMES_TO_RUN;
        Logger.logToFile(prefPercentage + "\t" + avgRuntime, OUTPUT_FILE_NAME);
      }
    }
  }

  private String getLineupWithNBatters(int n) {
    String[] batters = LINEUP.split(",");
    if (n > batters.length) {
      throw new RuntimeException("Not enough batters provided");
    }
    String[] result = new String[n];
    int counter = 0;
    for (int i = 0; i < n; i++) {
      result[counter] = batters[i];
      counter++;
    }
    return String.join(",", result);
  }

}
