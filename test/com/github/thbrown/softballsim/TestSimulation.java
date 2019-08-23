package com.github.thbrown.softballsim;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.github.thbrown.softballsim.commands.CompleteOptimizationCommand;
import com.github.thbrown.softballsim.commands.ReadyOptimizationCommand;
import com.github.thbrown.softballsim.helpers.MonteCarloSimulationDataBuilder;
import com.github.thbrown.softballsim.helpers.ProcessHooks;
import com.github.thbrown.softballsim.helpers.TestServer;

public class TestSimulation {
    
  @Test
  public void testSimulation() throws Exception {
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
    
    for (int k = 0; k < PLAYER_COUNT; k++) { 
      players[k] = new Player.Builder("player" + k).outs(5).singles(5).build(); 
    } 

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
    }, true);
    
    // Can't test this automatically because it requires shutting down the jvm
    /*
    // Wait for shutdown script to run
    Thread.sleep(1000);
    
    System.out.println("CHECKING FOR FILE");
   
    // We'll know if the shutdown script was executed based on whether or not it created the test file.
    File tmpDir = new File("testCleanupScriptFile");
    boolean exists = tmpDir.exists();
    if(!exists) {
      throw new RuntimeException("Cleanup script was not executed! Or, at least, it did not create the file we expected it to");
    }
    
    // Cleanup
    tmpDir.delete();
    */
  }

}
