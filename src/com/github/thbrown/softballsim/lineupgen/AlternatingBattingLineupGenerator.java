package com.github.thbrown.softballsim.lineupgen;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
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
import com.github.thbrown.softballsim.lineup.AlternatingBattingLineup;
import com.github.thbrown.softballsim.lineup.BattingLineup;

public class AlternatingBattingLineupGenerator implements LineupGenerator {

  private Queue<BattingLineup> allPossibleLineups = new LinkedList<>();

  @Override
  public void readDataFromFile(String statsPath) {
    List<Player> groupA = new ArrayList<>();
    List<Player> groupB = new ArrayList<>();

    Map<String, String> nameAndGroupToHitData =
        LineupGeneratorUtil.readFilesFromPath(statsPath, read);
    collect(nameAndGroupToHitData, groupA, groupB);

    // Find all batting lineup permutations
    List<List<Player>> groupALineups = PermutationGeneratorUtil.permute(groupA);
    List<List<Player>> groupBLineups = PermutationGeneratorUtil.permute(groupB);

    for (List<Player> groupAPermutation : groupALineups) {
      for (List<Player> groupBPermutation : groupBLineups) {
        // TODO: Need to account for both groupA bat first and groupB bat first.
        allPossibleLineups.add(new AlternatingBattingLineup(groupAPermutation, groupBPermutation));
        allPossibleLineups.add(new AlternatingBattingLineup(groupBPermutation, groupAPermutation));
      }
    }
  }

  @Override
  public BattingLineup getNextLineup() {
    return allPossibleLineups.poll();
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
              validate(splitLine);

              // Name,Group
              String key = splitLine[0] + "," + splitLine[1];
              // Hits
              String value = line.replace(key, "");

              if (nameAndGroupToHitData.containsKey(key)) {
                nameAndGroupToHitData.put(key, nameAndGroupToHitData.get(key) + value);
              } else {
                nameAndGroupToHitData.put(key, value);
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

  // FIXME: Format is brittle.
  private void collect(Map<String, String> nameAndGroupToHitData, List<Player> groupA,
      List<Player> groupB) {
    for (Entry<String, String> entry : nameAndGroupToHitData.entrySet()) {
      String[] keySplit = entry.getKey().split(",");
      String name = keySplit[0];
      String group = keySplit[1];
      String hitLine = entry.getValue();
      String[] hitSplit = hitLine.split(",");
      if (group.equals("A")) {
        groupA.add(
            LineupGeneratorUtil.createPlayer(name, hitSplit));
      } else {
        groupB.add(
            LineupGeneratorUtil.createPlayer(name, hitSplit));
      }
    }
  }
}
