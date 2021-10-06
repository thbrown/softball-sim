package com.github.thbrown.softballsim;

import org.junit.Assert;
import org.junit.Test;

public class MonteCarloAdaptiveTest {

  @Test
  public void testMonteCarloAdaptive() throws Exception {
    final int INNINGS = 5;
    final double ALPHA = .01;
    final int LINEUP_TYPE = 1;
    final int THREAD_COUNT = 8;
    final String LINEUP = "1OiRCCmrn16iyK,Nelly,1CV6WRyspDjA7Z,1MPJ24EEyS0g6p,Devon,Jordyn";

    String[] args = {"-o", "MONTE_CARLO_ADAPTIVE", "-l", LINEUP, "-A", String.valueOf(ALPHA), "-I",
        String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE), "-T", String.valueOf(THREAD_COUNT), "-f"};

    // Run the same simulation with the exhaustive optimizer, useful for doing a
    // comparison
    String[] args2 = {"-o", "MONTE_CARLO_EXHAUSTIVE", "-l", LINEUP, "-G", String.valueOf(5000), "-I",
        String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE), "-T", String.valueOf(THREAD_COUNT)};

    Result result = SoftballSim.mainInternal(args);
    Assert.assertNotNull("Expected the simulation to produce a result, but it produced null", result);
  }

}
