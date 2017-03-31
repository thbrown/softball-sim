package com.github.thbrown.softballsim.lineupgen;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Scanner;
import java.util.function.BiFunction;

import com.github.thbrown.softballsim.PermutationGeneratorUtil;
import com.github.thbrown.softballsim.Player;
import com.github.thbrown.softballsim.lineup.BattingLineup;
import com.github.thbrown.softballsim.lineup.OrdinaryBattingLineup;

public class OrdinaryBattingLineupGenerator implements LineupGenerator {

  private Queue<BattingLineup> allPossibleLineups = new LinkedList<>();

  @Override
  public BattingLineup getNextLineup() {
    return allPossibleLineups.poll();
  }

  @Override
  public void readDataFromFile(String statsPath) {
    List<Player> players = new LinkedList<>();

    Map<String, String> nameAndGroupToHitData =
        LineupGeneratorUtil.readFilesFromPath(statsPath, read);
    collect(nameAndGroupToHitData, players);

    // Find all batting lineup permutations
    List<List<Player>> lineups = PermutationGeneratorUtil.permute(players);
    for (List<Player> lineup : lineups) {
      allPossibleLineups.add(new OrdinaryBattingLineup(lineup));
    }
  }

  // FIXME: Format is brittle.
  private static BiFunction<String, Map<String, String>, Void> read =
      (filename, nameAndGroupToHitData) -> {
        try {
          Scanner in = null;
          try {
            in = new Scanner(new FileReader(filename));
            in.useDelimiter(System.lineSeparator());

            while (in.hasNext()) {
              String line = in.next().trim();
              if (line.isEmpty()) {
                continue;
              }
              String[] splitLine = line.split(",");
              LineupGeneratorUtil.validateHitValues(Arrays.copyOfRange(splitLine, 1,
                  splitLine.length));

              // Name
              String key = splitLine[0];
              // Hits
              String value = line.replace(key, "");

              if (nameAndGroupToHitData.containsKey(key)) {
                nameAndGroupToHitData.put(key,
                    nameAndGroupToHitData.get(key) + value);
              } else {
                nameAndGroupToHitData.put(key, value);
              }
            }
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          } finally {
            in.close();
          }
        } catch (Exception e) {
          System.out.println("WARNING: There was a problem while processing " + filename
              + ". This file will be skipped. Problem: " + e.getMessage());
        }
        return null;
      };

  // FIXME: Format is brittle.
  private void collect(Map<String, String> nameAndGroupToHitData, List<Player> players) {
    for (Entry<String, String> entry : nameAndGroupToHitData.entrySet()) {
      String name = entry.getKey();
      String[] hitLine = entry.getValue().split(",");
      players.add(LineupGeneratorUtil.createPlayer(name, hitLine));
    }
  }
}
