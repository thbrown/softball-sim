package com.github.thbrown.softballsim;

import org.junit.Assert;
import org.junit.Test;

public class MonteCarloExhaustiveTest {

  @Test
  public void testDataSourceFileSystem() throws Exception {
    final int INNINGS = 7;
    final int GAMES = 1000;
    final int LINEUP_TYPE = 0;
    final int THREAD_COUNT = 1;

    String[] args = {"-o", "MONTE_CARLO_EXHAUSTIVE", "-f", "-l",
        "1OiRCCmrn16iyK,Oscar,Molly,Nelly,1CV6WRyspDjA7Z,1MPJ24EEyS0g6p -y ./stats", "-G", String.valueOf(GAMES), "-I",
        String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE), "-T", String.valueOf(THREAD_COUNT)};

    Result result = SoftballSim.mainInternal(args);
    Assert.assertNotNull("Expected the simulation to produce a result, but it produced null", result);
  }

}
