package com.github.thbrown.softballsim.lineupgen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.function.BiFunction;

import com.github.thbrown.softballsim.AtBatOutcome;
import com.github.thbrown.softballsim.Player;

public class LineupGeneratorUtil {

  static Player createPlayer(String name, String commaSeparatedHitString) {
    String[] hitList = commaSeparatedHitString.split(",");
    return new Player.Builder(name)
        .outs(
            (int) Arrays.stream(hitList).filter(e -> e.equals(AtBatOutcome.OUT.getStringValue()))
                .count())
        .singles(
            (int) Arrays.stream(hitList)
                .filter(e -> e.equals(AtBatOutcome.SINGLE.getStringValue())).count())
        .doubles(
            (int) Arrays.stream(hitList)
                .filter(e -> e.equals(AtBatOutcome.DOUBLE.getStringValue())).count())
        .triples(
            (int) Arrays.stream(hitList)
                .filter(e -> e.equals(AtBatOutcome.TRIPLE.getStringValue())).count())
        .homeRuns(
            (int) Arrays.stream(hitList)
                .filter(e -> e.equals(AtBatOutcome.HOME_RUN.getStringValue())).count())
        .walks(
            (int) Arrays.stream(hitList).filter(e -> e.equals(AtBatOutcome.WALK.getStringValue()))
                .count()).build();
  }

  static void createPlayersFromMap(Map<String, String> nameAndGroupToHitData, List<Player> players) {
    for (Entry<String, String> entry : nameAndGroupToHitData.entrySet()) {
      String name = entry.getKey();
      String hitLine = entry.getValue();
      players.add(LineupGeneratorUtil.createPlayer(name, hitLine));
    }
  }

  static void validateHitValues(String[] splitHitLine) {
    for (int i = 0; i < splitHitLine.length; i++) {
      if (!AtBatOutcome.isValidAtBatOutcome(splitHitLine[i])) {
        throw new IllegalArgumentException(
            String.format("Invalid hit value: (%s)", splitHitLine[i]));
      }
    }
  }

  static void addEntryToGroup(Map<String, String> group, String key, String value) {
    if (group.containsKey(key)) {
      group.put(key, group.get(key) + value);
    } else {
      group.put(key, value);
    }
  }

  static List<Map<String, String>> readFilesFromPath(String statsPath, int numGroups,
      BiFunction<List<Map<String, String>>, String, Void> addLineToGroupsFunction) {
    List<Map<String, String>> groups = new ArrayList<>();
    for (int i = 0; i < numGroups; i++) {
      groups.add(new HashMap<>());
    }

    File[] listOfFiles = LineupGeneratorUtil.findFilesInPath(statsPath);
    for (int i = 0; i < listOfFiles.length; i++) {
      String filename = LineupGeneratorUtil.createAbsoluteFilename(statsPath,
          listOfFiles[i].getName());
      System.out.println("Processing file " + filename);
      readFileIntoGroups(filename, groups, addLineToGroupsFunction);
    }
    return groups;
  }

  private static void readFileIntoGroups(String filename, List<Map<String, String>> groups,
      BiFunction<List<Map<String, String>>, String, Void> addLineToGroupsFunction) {
    try
    {
      Scanner in = null;
      try {
        in = new Scanner(new FileReader(filename));
        in.useDelimiter(System.lineSeparator());

        while (in.hasNext()) {
          String line = in.next().trim();
          if (line.isEmpty()) {
            continue;
          }
          addLineToGroupsFunction.apply(groups, line);
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
  }

  static File[] findFilesInPath(String statsPath) {
    File folder = new File(statsPath);
    File[] listOfFiles = folder.listFiles(m -> m.isFile());
    if (listOfFiles == null) {
      throw new IllegalArgumentException("No files were found in " + statsPath);
    }
    return listOfFiles;
  }

  static String createAbsoluteFilename(String statsPath, String relativeName) {
    return statsPath + File.separator + relativeName;
  }
}
