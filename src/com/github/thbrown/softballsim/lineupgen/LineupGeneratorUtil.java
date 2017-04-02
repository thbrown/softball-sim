package com.github.thbrown.softballsim.lineupgen;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import com.github.thbrown.softballsim.AtBatOutcome;
import com.github.thbrown.softballsim.Player;

public class LineupGeneratorUtil {

  static Player createPlayer(String name, String[] hitList) {
    return new Player.Builder(name)
        .outs((int) Arrays.stream(hitList).filter(e -> e.equals(AtBatOutcome.OUT.getStringValue())).count())
        .singles((int) Arrays.stream(hitList).filter(e -> e.equals(AtBatOutcome.SINGLE.getStringValue())).count())
        .doubles((int) Arrays.stream(hitList).filter(e -> e.equals(AtBatOutcome.DOUBLE.getStringValue())).count())
        .triples((int) Arrays.stream(hitList).filter(e -> e.equals(AtBatOutcome.TRIPLE.getStringValue())).count())
        .homeRuns((int) Arrays.stream(hitList).filter(e -> e.equals(AtBatOutcome.HOME_RUN.getStringValue())).count())
        .walks((int) Arrays.stream(hitList).filter(e -> e.equals(AtBatOutcome.WALK.getStringValue())).count()).build();
  }

  static void createPlayersFromMap(Map<String, String> nameAndGroupToHitData, List<Player> players) {
    for (Entry<String, String> entry : nameAndGroupToHitData.entrySet()) {
      String name = entry.getKey();
      String[] hitLine = entry.getValue().split(",");
      players.add(LineupGeneratorUtil.createPlayer(name, hitLine));
    }
  }

  static void validateHitValues(String[] splitHitLine) {
    for (int i = 0; i < splitHitLine.length; i++) {
      if (!AtBatOutcome.isValidAtBatOutcome(splitHitLine[i])) {
        throw new IllegalArgumentException(String.format("Invalid hit value: (%s)", splitHitLine[i]));
      }
    }
  }

  static List<Map<String, String>> readFilesFromPath(String statsPath, int numGroups,
      BiFunction<String, List<Map<String, String>>, Void> readFunction) {
    List<Map<String, String>> groups = new ArrayList<>();
    for (int i=0; i < numGroups; i++) {
      groups.add(new HashMap<>());
    }
    
    File[] listOfFiles = LineupGeneratorUtil.findFilesInPath(statsPath);
    for (int i = 0; i < listOfFiles.length; i++) {
      String filename = LineupGeneratorUtil.createAbsoluteFilename(statsPath, listOfFiles[i].getName());
      System.out.println("Processing file " + filename);
      readFunction.apply(filename, groups);
    }
    return groups;
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
