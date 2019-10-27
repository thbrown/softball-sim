package com.github.thbrown.softballsim.gson;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.OptimizationResult;
import com.github.thbrown.softballsim.Player;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.SoftballSim;
import com.github.thbrown.softballsim.datasource.NetworkProgressTracker;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.lineup.DummyAlternatingBattingLineup;
import com.github.thbrown.softballsim.lineup.DummyOrdinaryBattingLineup;
import com.github.thbrown.softballsim.lineupgen.LineupGenerator;
import com.github.thbrown.softballsim.lineupgen.LineupTypeEnum;
import com.github.thbrown.softballsim.util.Logger;
import com.google.gson.Gson;

public class MonteCarloExhaustiveOptimizatonDefinition extends BaseOptimizationDefinition {
  private int innings;
  private int iterations;
  private int lineupType;

  private Map<String, List<String>> initialLineup;
  private int startIndex;
  private Double initialScore; // Allow for nulls
  private long initialElapsedTimeMs;
  private Integer threadCount;
  
  private Map<Long, Long> initialHistogram;
  
  public int getInnings() {
    return innings;
  }

  public int getIterations() {
    return iterations;
  }

  public int getLineupType() {
    return lineupType;
  }

  public int getStartIndex() {
    return startIndex;
  }
  
  public long getInitialElapsedTimeMs() {
    return initialElapsedTimeMs;
  }

  public Map<String, List<String>> getInitialLineup() {
    return initialLineup;
  }
  
  public Double getInitialScore() {
    return initialScore;
  }

  public Map<Long, Long> getInitialHistogram() {
    return initialHistogram;
  }
  
  public Map<Long, Long> getThreadCount() {
    return initialHistogram;
  }


  @Override
  public void runSimulation(Gson gson, PrintWriter network) {
    int gamesToSimulate = this.getIterations() == 0 ? Integer.parseInt(CommandLineOptions.GAMES_DEFAULT) : this.getIterations();
    int inningsToSimulate = this.getInnings() == 0 ? Integer.parseInt(CommandLineOptions.INNINGS_DEFAULT) : this.getInnings();
    int lineupType = this.getLineupType();
    int threadCount = this.threadCount == null ? Integer.parseInt(CommandLineOptions.THREADS_DEFAULT) : this.threadCount;
    
    // Transform the json lineup given by the network to the comma separated form this program expects
    StringBuilder transformedData = new StringBuilder();
    if(this.getLineupType() == 1) {
      for(Player entry : this.getPlayers()) {
        String outs = buildCommaSeparatedList("0", entry.getOuts());
        String singles = buildCommaSeparatedList("1", entry.getSingles());
        String doubles = buildCommaSeparatedList("2", entry.getDoubles());
        String triples = buildCommaSeparatedList("3", entry.getTriples());
        String homeruns = buildCommaSeparatedList("4", entry.getHomeruns());
        String line = joinIgnoreEmpty(",", entry.getName(), outs, singles, doubles, triples, homeruns, "\n");
        transformedData.append(line);
      }
    } else if(this.getLineupType() == 2 || this.getLineupType() == 3) {
      // TODO: We should probably have both lineup types take the same stats data and the non-gendered types can just ignore gender
      for(Player entry : this.getPlayers()) {
        String outs = buildCommaSeparatedList("0", entry.getOuts());
        String singles = buildCommaSeparatedList("1", entry.getSingles());
        String doubles = buildCommaSeparatedList("2", entry.getDoubles());
        String triples = buildCommaSeparatedList("3", entry.getTriples());
        String homeruns = buildCommaSeparatedList("4", entry.getHomeruns());
        
        // Using A/B instead of M/F so we can reuse this optimizer for other use case (like maybe old/young). <- Still trying to decide if this was a good idea
        String line = joinIgnoreEmpty(",", entry.getName(), entry.getGender().equals("F") ? "B" : "A", outs, singles, doubles, triples, homeruns, "\n");
        transformedData.append(line);
      }
    } else {
      throw new UnsupportedOperationException("Unrecognized lineup type " + this.getLineupType());
    }
    
    LineupGenerator generator = LineupTypeEnum.getEnumFromIdOrName(String.valueOf(this.getLineupType())).getLineupGenerator();
    generator.readDataFromString(transformedData.toString());
    
    if(generator.size() <= 0) {
      throw new IllegalArgumentException("There are no possible lineups for this lineup type and player combination");
    }

    // Account for initial conditions if specified
    long startIndex = this.getStartIndex();
    long initialElapsedTimeMs = this.getInitialElapsedTimeMs();
    Map<Long, Long> initialHisto = this.getInitialHistogram();
    Double initialScore = this.getInitialScore();
    Result initialResult = null;
    Map<String, List<String>> initialLineup = this.getInitialLineup();
    
    if(initialLineup != null && initialScore != null && initialHisto != null) {
      // Build a list of players
      if(lineupType == 1 || lineupType == 3) {
        initialResult = new Result(initialScore, new DummyOrdinaryBattingLineup(initialLineup.get("GroupA")));
      } else if(lineupType == 2) {
        initialResult = new Result(initialScore, new DummyAlternatingBattingLineup(initialLineup.get("GroupA"), initialLineup.get("GroupB")));
      } else {
        throw new RuntimeException("Unrecognized lineup type: " + lineupType);
      }

      Logger.log("Initial conditions were specified");
      Logger.log(initialResult);
      Logger.log(initialHisto);
    } else {
      initialHisto = null;
      initialResult = null;
    }
    
    
    ProgressTracker tracker = new NetworkProgressTracker(generator.size(), SoftballSim.DEFAULT_UPDATE_FREQUENCY_MS, startIndex, gson, network, initialElapsedTimeMs);
    
    OptimizationResult result = SoftballSim.simulateLineups(generator, gamesToSimulate, inningsToSimulate, startIndex, tracker, initialResult, initialHisto, threadCount);
    
    Logger.log(result.toString());
    Logger.log("Local simulation time: " + tracker.getLocalElapsedTimeMs() + " milliseconds --");
    
    // Send the results back over the network
    Map<String,Object> completeCommand = new HashMap<>();
    completeCommand.put("command", "COMPLETE");
    completeCommand.put("lineup", result.getLineup());
    completeCommand.put("score", result.getScore());
    completeCommand.put("histogram", result.getHistogram());
    completeCommand.put("elapsedTimeMs", result.getElapsedTimeMs());
    completeCommand.put("total", generator.size());
    completeCommand.put("complete", generator.size());
    String jsonCompleteCommand = gson.toJson(completeCommand);
    network.println(jsonCompleteCommand);
    Logger.log("SENT: \t\t" + jsonCompleteCommand);
  }

  private String joinIgnoreEmpty(String delimiter, String...strings) {
    StringJoiner joiner = new StringJoiner(delimiter);
    for(String s : strings) {
      if(s == null || s.equals("")) {
        continue;
      }
      joiner.add(s);
    }
    return joiner.toString();
  }

  private String buildCommaSeparatedList(String value, int count) {
    if(count <= 0) {
      return "";
    }
    
    StringBuilder result = new StringBuilder();
    for(int i = 0; i < count; i ++) {
        result.append(value);
        result.append(",");
    }
    return result.length() > 0 ? result.substring(0, result.length() - 1): "";
  }

}
