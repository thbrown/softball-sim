package com.github.thbrown.softballsim;

import org.junit.Test;

/**
 * Tests for the various dataSource values
 */
public class DataSourceTest {

  @Test
  public void testDataSourceFileSystem() throws Exception {
    String[] args = {"-O", "MONTE_CARLO_EXHAUSTIVE", "-P",
        "1OiRCCmrn16iyK,00000000000001,0000000000000f,0000000000000k,1CV6WRyspDjA7Z", "1MPJ24EEyS0g6p", "-g", "1000",
        "-i", "7", "-T", "1"};
    SoftballSim.main(args);
  }
  /*
   * TODO: Network
   * 
   * @Test public void testDataSourceNetwork() throws Exception {
   * 
   * final int INNINGS = 7; final int ITERATIONS = 100; final int LINEUP_TYPE = 1; final int
   * THREAD_COUNT = 1;
   * 
   * final int PLAYER_COUNT = 8; Player[] players = new Player[PLAYER_COUNT];
   * 
   * for (int k = 0; k < PLAYER_COUNT; k++) { players[k] = new Player.Builder("player" +
   * k).outs(5).singles(5).build(); }
   * 
   * MonteCarloSimulationDataBuilder mcsdb = new MonteCarloSimulationDataBuilder()
   * .withInnings(INNINGS) .withIterations(ITERATIONS) .withLineupType(LINEUP_TYPE)
   * .withThreadCount(THREAD_COUNT) .withPlayers(players);
   * 
   * final Map<String, Long> wrapper = new HashMap<>(); TestServer.runSimulationOverNetwork(new
   * ProcessHooks() {
   * 
   * @Override public boolean onReady(ReadyOptimizationCommand data, PrintWriter out) { String json;
   * json = mcsdb.toString(); json = json.replace("\n", "").replace("\r", ""); out.println(json);
   * return false; }
   * 
   * @Override public boolean onComplete(CompleteOptimizationCommand data, PrintWriter out) throws
   * Exception { wrapper.put("elapsedTime", data.getElapsedTimeMs()); wrapper.put("lineupCount",
   * data.getComplete()); return true; } }, false); }
   */

}
