package com.github.thbrown.softballsim;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.data.gson.DataStats;

public class ExpectedValueTest {

  @Test
  public void testDataSourceFileSystem() throws Exception {
    final int INNINGS = 7;
    final int MAX_BATTERS = 1;
    final int LINEUP_TYPE = 0;
    final int THREAD_COUNT = 4;

    String[] args = {"-o", "EXPECTED_VALUE", "-f", "-l",
        "1OiRCCmrn16iyK,Oscar,Molly,Nelly,1CV6WRyspDjA7Z,1MPJ24EEyS0g6p",
        "-B", String.valueOf(MAX_BATTERS), "-I", String.valueOf(INNINGS), "-t", String.valueOf(LINEUP_TYPE), "-T",
        String.valueOf(THREAD_COUNT)};

    Result result = SoftballSim.mainInternal(args);
    Assert.assertNotNull("Expected the simulation to produce a result, but it produced null", result);
  }


}
