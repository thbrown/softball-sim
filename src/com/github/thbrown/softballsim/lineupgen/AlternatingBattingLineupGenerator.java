package com.github.thbrown.softballsim.lineupgen;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.function.BiFunction;

import com.github.thbrown.softballsim.PermutationGeneratorUtil;
import com.github.thbrown.softballsim.Player;
import com.github.thbrown.softballsim.lineup.AlternatingBattingLineup;
import com.github.thbrown.softballsim.lineup.BattingLineup;

public class AlternatingBattingLineupGenerator implements LineupGenerator {

  private Queue<BattingLineup> allPossibleLineups = new LinkedList<>();

  @Override
  public void readDataFromFile(String statsPath) {
    List<Player> groupA = new ArrayList<>();
    List<Player> groupB = new ArrayList<>();

    List<Map<String, String>> groups = LineupGeneratorUtil.readFilesFromPath(statsPath, 2 /* numGroups */, read);
    LineupGeneratorUtil.createPlayersFromMap(groups.get(0), groupA);
    LineupGeneratorUtil.createPlayersFromMap(groups.get(1), groupB);

    // Find all batting lineup permutations
    List<List<Player>> groupALineups = PermutationGeneratorUtil.permute(groupA);
    List<List<Player>> groupBLineups = PermutationGeneratorUtil.permute(groupB);

    for (List<Player> groupAPermutation : groupALineups) {
      for (List<Player> groupBPermutation : groupBLineups) {
        // TODO: Need to account for both groupA bat first and groupB bat first.
        allPossibleLineups.add(new AlternatingBattingLineup(groupAPermutation, groupBPermutation));
        // allPossibleLineups.add(new AlternatingBattingLineup(groupBPermutation, groupAPermutation));
      }
    }
  }

  @Override
  public BattingLineup getNextLineup() {
    return allPossibleLineups.poll();
  }

  // FIXME: Format is brittle.
  private static BiFunction<String, List<Map<String, String>>, Void> read = (filename, groups) -> {
    if (groups.size() != 2) {
      throw new IllegalArgumentException(
          "AlternatingBattingLineupGenerator expects 2 groups, was " + groups.size());
    }
    Map<String, String> groupAMap = groups.get(0);
    Map<String, String> groupBMap = groups.get(1);
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
          validate(splitLine);

          // Name
          String key = splitLine[0];
          // Hits
          String value = line.replace(splitLine[0] + "," + splitLine[1], "");

          if (splitLine[1].equals("A")) {
            if (groupAMap.containsKey(key)) {
              groupAMap.put(key, groupAMap.get(key) + value);
            } else {
              groupAMap.put(key, value);
            }
          } else {
            if (groupBMap.containsKey(key)) {
              groupBMap.put(key, groupBMap.get(key) + value);
            } else {
              groupBMap.put(key, value);
            }
          }

        }
      } catch (FileNotFoundException e) {
        throw e;
      } finally {
        in.close();
      }
    } catch (Exception e) {
      System.out.println("WARNING: There was a problem while processing " + filename
          + ". This file will be skipped. Problem: " + e.getMessage());
    }
    return null;
  };

  private static void validate(String[] splitLine) {
    if (!splitLine[1].equals("A") && !splitLine[1].equals("B")) {
      throw new RuntimeException("Expected each player to be in either group A or B");
    }
    LineupGeneratorUtil.validateHitValues(Arrays.copyOfRange(splitLine, 2, splitLine.length));
  }
}
