package com.github.thbrown.softballsim.lineupgen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

import com.github.thbrown.softballsim.PermutationGeneratorUtil;
import com.github.thbrown.softballsim.Player;
import com.github.thbrown.softballsim.lineup.AlternatingBattingLineup;
import com.github.thbrown.softballsim.lineup.BattingLineup;

public class AlternatingBattingLineupGenerator implements LineupGenerator {

  private Queue<BattingLineup> allPossibleLineups = new LinkedList<>();
  Map<String, String> data = new HashMap<>();

  @Override
  public void readInDataFromFile(String statsPath) {
    List<Player> groupA = new ArrayList<>();
    List<Player> groupB = new ArrayList<>();

    // Read in batter data from the supplied directory
    File folder = new File(statsPath);
    File[] listOfFiles = folder.listFiles(m -> m.isFile());
    if (listOfFiles == null) {
      throw new IllegalArgumentException("No files were found in " + statsPath);
    }
    for (int i = 0; i < listOfFiles.length; i++) {
      System.out.println("Processing file " + listOfFiles[i].getName());
      read(statsPath + File.separator + listOfFiles[i].getName());
    }
    collect(groupA, groupB);

    // Find all batting lineup permutations
    List<List<Player>> groupALineups = PermutationGeneratorUtil.permute(groupA);
    List<List<Player>> groupBLineups = PermutationGeneratorUtil.permute(groupB);
    System.out.println("Possible lineups: " + groupALineups.size() * groupBLineups.size());

    for (List<Player> groupAPermutation : groupALineups) {
      for (List<Player> groupBPermutation : groupBLineups) {
        // TODO: need to account for both groupA bat first and groupB bat first
        allPossibleLineups.add(new AlternatingBattingLineup(groupAPermutation, groupBPermutation));
      }
    }
  }

  @Override
  public BattingLineup getNextLienup() {
    return allPossibleLineups.poll();
  }

  // FIXME: This is brittle and has a bad format
  private void read(String string) {
    try {
      Scanner in = null;
      try {
        in = new Scanner(new FileReader(string));
        in.useDelimiter("\n"); // System.lineSeparator());
        while (in.hasNext()) {
          String line = in.next();
          String[] s = line.split(",");
          String key = s[0] + "," + s[1];

          // Validate data
          if (!s[1].equals("A") && !s[1].equals("B")) {
            throw new RuntimeException("Expected each player to be in either group A or B");
          }
          for (int i = 2; i < s.length; i++) {
            String hit = s[i].trim();
            if (!(hit.equals("0") || hit.equals("1") || hit.equals("2") || hit.equals("3") || hit
                .equals("4"))) {
              throw new IllegalArgumentException("Invalid data value: " + s[i]);
            }
          }

          if (data.containsKey(key)) {
            data.put(key, data.get(key) + line.replace(key + ",", "") + ",");
          } else {
            data.put(key, line.replace(key + ",", "") + ",");
          }
        }
      } catch (FileNotFoundException e) {
        throw e;
      } finally {
        in.close();
      }
    } catch (Exception e) {
      System.out.println("WARNING: There was a problem while processing " + string
          + ". This file will be skipped. Problem: " + e.getMessage());
    }
  }

  // FIXME: This is brittle and has a bad format
  private void collect(List<Player> groupA, List<Player> groupB) {
    for (String key : data.keySet()) {
      String name = key.split(",")[0];
      String gender = key.split(",")[1];
      String line = data.get(key);
      String[] s = line.split(",");
      if (gender.equals("A")) {
        groupA.add(
            new Player(
                name,
                s.length,
                (int) Arrays.stream(s).filter(e -> e.equals("1")).count(),
                (int) Arrays.stream(s).filter(e -> e.equals("2")).count(),
                (int) Arrays.stream(s).filter(e -> e.equals("3")).count(),
                (int) Arrays.stream(s).filter(e -> e.equals("4")).count(),
                (int) Arrays.stream(s).filter(e -> e.equals("BB")).count()));
      } else {
        groupB.add(
            new Player(
                name,
                s.length,
                (int) Arrays.stream(s).filter(e -> e.equals("1")).count(),
                (int) Arrays.stream(s).filter(e -> e.equals("2")).count(),
                (int) Arrays.stream(s).filter(e -> e.equals("3")).count(),
                (int) Arrays.stream(s).filter(e -> e.equals("4")).count(),
                (int) Arrays.stream(s).filter(e -> e.equals("BB")).count()));
      }
    }
  }
}
