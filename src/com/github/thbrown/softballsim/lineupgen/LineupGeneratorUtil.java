package com.github.thbrown.softballsim.lineupgen;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import com.github.thbrown.softballsim.AtBatOutcome;
import com.github.thbrown.softballsim.Player;

public class LineupGeneratorUtil {

  static Player createPlayer(String name, String[] hitList) {
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
                .count())
        .build();
  }

  static void validateHitValues(String[] splitHitLine) {
    for (int i = 0; i < splitHitLine.length; i++) {
      if (!AtBatOutcome.isValidAtBatOutcome(splitHitLine[i])) {
        throw new IllegalArgumentException(String.format("Invalid hit value: (%s)",
            splitHitLine[i]));
      }
    }
  }

  static Map<String, String> readFilesFromPath(String statsPath,
      BiFunction<String, Map<String, String>, Void> readFunction) {
    Map<String, String> nameAndGroupToHitData = new HashMap<>();
    File[] listOfFiles = LineupGeneratorUtil.findFilesInPath(statsPath);
    for (int i = 0; i < listOfFiles.length; i++) {
      String filename = LineupGeneratorUtil.createAbsoluteFilename(statsPath,
          listOfFiles[i].getName());
      System.out.println("Processing file " + filename);
      readFunction.apply(filename, nameAndGroupToHitData);
    }
    return nameAndGroupToHitData;
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
