package com.github.thbrown.softballsim.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.SoftballSim;

public class EstimationParameterSampler {

  @Test
  public void testMultiple() throws Exception {
    final int[] INNINGS_POOL = {4, 5, 6, 7, 8};
    final double[] ALPHA_POOL = {.1, .01, .001, .0001};
    final int[] LINEUP_TYPE_POOL = {1};
    final int THREAD_COUNT = 8;
    final String[] PLAYERS_POOL = {"00000000000001", "00000000000002", "00000000000003", "00000000000004",
        "00000000000005", "00000000000008", "0000000000000a", "0000000000000f", "0000000000000g", "0000000000000h",
        "0000000000000e", "0000000000000i", "0000000000000j", "0000000000000k", "0000000000000l", "0000000000000n"};
    final int[] lineupLength = {6, 7, 8, 9, 10};

    List<String> results = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
      try {
        int INNINGS = getRandom(INNINGS_POOL);
        double ALPHA = getRandom(ALPHA_POOL);
        int LINEUP_TYPE = getRandom(LINEUP_TYPE_POOL);
        String LINEUP = getRandomSet(PLAYERS_POOL, getRandom(lineupLength));

        System.out.println(i);

        System.out.println(LINEUP);

        String[] actualArgs = {"-o", "MONTE_CARLO_ADAPTIVE", "-l", LINEUP, "-A", String.valueOf(ALPHA), "-I",
            String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE), "-T", String.valueOf(THREAD_COUNT), "-f"};

        String[] estimateArgs = {"-o", "MONTE_CARLO_ADAPTIVE", "-l", LINEUP, "-A", String.valueOf(ALPHA), "-I",
            String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE), "-T", String.valueOf(THREAD_COUNT), "-f", "-e"};

        Result resultEst = SoftballSim.mainInternal(estimateArgs);
        Result resultAct = SoftballSim.mainInternal(actualArgs);

        results.add(resultEst.getEstimatedTimeRemainingMs() + "\t" + resultAct.getElapsedTimeMs());
      } catch (Exception e) {
      }
      // Print results
      for (String s : results) {
        System.out.println(s);
      }
    }

    // Print results
    for (String s : results) {
      System.out.println(s);
    }
  }


  /*
   * public static <T> T getRandom(T[] array) { int rnd = new Random().nextInt(array.length); return
   * array[rnd]; }
   */
  public static int getRandom(int[] array) {
    int rnd = new Random().nextInt(array.length);
    return array[rnd];
  }

  public static double getRandom(double[] array) {
    int rnd = new Random().nextInt(array.length);
    return array[rnd];
  }

  public static String getRandomSet(String[] array, int size) {
    Collections.shuffle(Arrays.asList(array));
    String[] subset = Arrays.copyOfRange(array, 0, size);
    return String.join(",", subset);
  }
}
