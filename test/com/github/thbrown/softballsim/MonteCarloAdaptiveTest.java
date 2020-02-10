package com.github.thbrown.softballsim;

import org.junit.Test;

public class MonteCarloAdaptiveTest {

  @Test
  public void testMonteCarloAdaptive() throws Exception {
    final int INNINGS = 7;
    final double ALPHA = .0001;
    final int LINEUP_TYPE = 1;
    final int THREAD_COUNT = 8;

    //        "1OiRCCmrn16iyK,Oscar,Molly,Nelly,1CV6WRyspDjA7Z,1MPJ24EEyS0g6p,Devon,Jordyn,Roger",

    
    String[] args = {"-O", "MONTE_CARLO_ADAPTIVE", "-P",
        "1OiRCCmrn16iyK,Oscar,Molly,Nelly,1CV6WRyspDjA7Z,1MPJ24EEyS0g6p,Devon,Jordyn",
        "-a", String.valueOf(ALPHA), "-i", String.valueOf(INNINGS), "-T", String.valueOf(LINEUP_TYPE), "-t",
        String.valueOf(THREAD_COUNT)};
    

    String[] args2 = {"-O", "MONTE_CARLO_EXHAUSTIVE", "-P",
        "1OiRCCmrn16iyK,Oscar,Molly,Nelly,1CV6WRyspDjA7Z,1MPJ24EEyS0g6p,Devon,Jordyn",
        "-g", String.valueOf(1066), "-i", String.valueOf(INNINGS), "-T", String.valueOf(LINEUP_TYPE), "-t",
        String.valueOf(THREAD_COUNT)};

    SoftballSim.main(args);
  }

  // TODO: network test

}
