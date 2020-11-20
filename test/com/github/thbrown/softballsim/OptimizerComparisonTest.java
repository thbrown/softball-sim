package com.github.thbrown.softballsim;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloExhaustiveResult;
import com.github.thbrown.softballsim.util.Logger;

public class OptimizerComparisonTest {

  final String OUTPUT_FILE_NAME = "opt-test.log";

  final int INNINGS = 7;
  final double ALPHA = .0001;
  final int LINEUP_TYPE = 1;
  final int THREAD_COUNT = 8;
  final String LINEUP =
      "1OiRCCmrn16iyK,Nelly,1CV6WRyspDjA7Z,1MPJ24EEyS0g6p,Devon,Jordyn,Brianna,Paul,Jessica,Leroy,Roy";

  final int TIMES_TO_RUN = 10;

  @Test
  public void generatePerformanceMatrixFile() throws Exception {
    /*
     * for(int lineupSize = 6; lineupSize<11; lineupSize++) { String lineup =
     * getLineupWithNBatters(lineupSize); Logger.logToFile("Lineup Size: " + lineupSize,
     * OUTPUT_FILE_NAME);
     * 
     * // Run the simulation with 1 million exhaustive iterations, so we have a benchmark to compare to
     * String[] argsBenchmark = {"-O", "MONTE_CARLO_EXHAUSTIVE", "-L", lineup, "-g",
     * String.valueOf(1000000), "-i", String.valueOf(INNINGS), "-T", String.valueOf(LINEUP_TYPE), "-t",
     * String.valueOf(THREAD_COUNT)}; MonteCarloExhaustiveResult benchmark =
     * (MonteCarloExhaustiveResult)SoftballSim.mainInternal(argsBenchmark);
     * 
     * for(int iterations = 10; iterations <= 100000; iterations*=10) { double totalRunsScored = 0;
     * List<Double> runsScored = new ArrayList<>(10); for(int i = 0; i < TIMES_TO_RUN; i++) { String[]
     * args = {"-O", "MONTE_CARLO_ANNEALING", "-L", lineup, "-a", String.valueOf(ALPHA), "-i",
     * String.valueOf(INNINGS), "-T", String.valueOf(LINEUP_TYPE), "-d", String.valueOf(iterations),
     * "-F"}; Result result = SoftballSim.mainInternal(args);
     * 
     * totalRunsScored += result.getLineupScore(); runsScored.add(result.getLineupScore());
     * System.out.println("RUNS SCORED " + result.getLineupScore()); } System.out.println("AVG SCORED "
     * + (totalRunsScored/(TIMES_TO_RUN)) + " VS " + benchmark.getLineupScore());
     * 
     * double demoninator = benchmark.getLineupScore() - benchmark.getWorstScore(); double
     * bestAvgRunsScored = totalRunsScored/TIMES_TO_RUN; String prefPercentage =
     * String.valueOf((bestAvgRunsScored - benchmark.getWorstScore())/demoninator); //
     * benchmark.getWorstScore() + " - " + benchmark.getLineupScore() + " " + runsScored + " " +
     * bestAvgRunsScored + " " + Logger.logToFile(prefPercentage, OUTPUT_FILE_NAME); } }
     */
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
