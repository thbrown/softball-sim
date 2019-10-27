package com.github.thbrown.softballsim;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.github.thbrown.softballsim.commands.CompleteOptimizationCommand;
import com.github.thbrown.softballsim.commands.ReadyOptimizationCommand;
import com.github.thbrown.softballsim.helpers.MonteCarloSimulationDataBuilder;
import com.github.thbrown.softballsim.helpers.ProcessHooks;
import com.github.thbrown.softballsim.helpers.TestServer;

/**
 * Tests for the various dataSource values
 */
public class DataSourceTest {
    
  @Test
  public void testNetworkDataSource() throws Exception {
            
    final int INNINGS = 7;
    final int ITERATIONS = 100;
    final int LINEUP_TYPE = 1;
    final int THREAD_COUNT = 1;
    
    final int PLAYER_COUNT = 8;
    Player[] players = new Player[PLAYER_COUNT];

    for (int k = 0; k < PLAYER_COUNT; k++) { 
      players[k] = new Player.Builder("player" + k).outs(5).singles(5).build(); 
    } 

    /*
    players[0] = new Player.Builder("player" + 0).gender("F").outs(34).singles(26).doubles(0).triples(0).homeruns(0).build();
    players[1] = new Player.Builder("player" + 1).gender("F").outs(40).singles(13).doubles(3).triples(0).homeruns(0).build();
    players[2] = new Player.Builder("player" + 2).gender("F").outs(22).singles(22).doubles(0).triples(1).homeruns(0).build();
    players[3] = new Player.Builder("player" + 3).gender("F").outs(16).singles(4).doubles(1).triples(0).homeruns(0).build();
    players[4] = new Player.Builder("player" + 4).gender("M").outs(23).singles(14).doubles(6).triples(3).homeruns(6).build();
    players[5] = new Player.Builder("player" + 5).gender("M").outs(20).singles(22).doubles(9).triples(4).homeruns(6).build();
    players[6] = new Player.Builder("player" + 6).gender("M").outs(23).singles(22).doubles(8).triples(4).homeruns(2).build();
    players[7] = new Player.Builder("player" + 7).gender("M").outs(11).singles(9).doubles(1).triples(1).homeruns(1).build();
    players[7] = new Player.Builder("player" + 8).gender("M").outs(10).singles(7).doubles(5).triples(1).homeruns(1).build();
    players[7] = new Player.Builder("player" + 9).gender("F").outs(9).singles(1).doubles(0).triples(0).homeruns(0).build();
    */
    
    MonteCarloSimulationDataBuilder mcsdb = new MonteCarloSimulationDataBuilder()
            .withInnings(INNINGS)
            .withIterations(ITERATIONS)
            .withLineupType(LINEUP_TYPE)
            .withThreadCount(THREAD_COUNT)
    			.withPlayers(players);
    
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
    }, false);
  }

  /**
   * Takes a number (value) between oldMin and oldMax and maps linearly to a new
   * value between newMin and newMax
   */
  private int transform(int value, int oldMin, int oldMax, int newMin, int newMax) {
    double x = (double) value;
    double a = (double) oldMin;
    double b = (double) oldMax;
    double c = (double) newMin;
    double d = (double) newMax;
    return (int) ((x - a) * (d - c) / (b - a) + c);
  }
  
  private String percentChange(double A, double B) throws IOException {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(3);
    return nf.format(Math.abs(A - B)/((A+B)/2) * 100);
  }

}
