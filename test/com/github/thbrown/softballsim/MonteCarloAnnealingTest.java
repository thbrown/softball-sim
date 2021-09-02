package com.github.thbrown.softballsim;

import org.junit.Assert;
import org.junit.Test;

public class MonteCarloAnnealingTest {

  @Test
  public void testMonteCarloAnnealing() throws Exception {
    final int INNINGS = 7;
    final double ALPHA = .01;
    final int DURATION = 10;
    final int LINEUP_TYPE = 0;
    final String LINEUP = "1OiRCCmrn16iyK,Nelly,1CV6WRyspDjA7Z,1MPJ24EEyS0g6p,Devon,Jordyn";

    String[] args = {"-o", "MONTE_CARLO_ANNEALING", "-l", LINEUP, "-A", String.valueOf(ALPHA), "-I",
        String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE), "-D", String.valueOf(DURATION), "-f"};

    String[] args2 = {"-o", "MONTE_CARLO_EXHAUSTIVE", "-l", LINEUP, "-A", String.valueOf(ALPHA), "-I",
        String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE), "-D", String.valueOf(DURATION), "-f"};

    String[] args3 = {"-o", "MONTE_CARLO_ADAPTIVE", "-l", LINEUP, "-A", String.valueOf(ALPHA), "-I",
        String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE), "-D", String.valueOf(DURATION), "-f"};

    Result result = SoftballSim.mainInternal(args);
    Assert.assertNotNull("Expected the simulation to produce a result, but it produced null", result);
  }

}
