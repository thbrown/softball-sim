package com.github.thbrown.softballsim.data.gson;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;

public class DataPlayer {
  private String id;
  private String name;
  private String gender;

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getGender() {
    return gender;
  }

  private transient Set<DataTeam> teams = new HashSet<>();
  private transient Set<DataGame> games = new HashSet<>();
  private transient Set<DataPlateAppearance> plateAppearances = new HashSet<>();

  public void addTeam(DataTeam team) {
    teams.add(team);
  }

  public void addGame(DataGame game) {
    games.add(game);
  }

  public void addPlateAppearences(DataPlateAppearance plateAppearance) {
    plateAppearances.add(plateAppearance);
  }

  public Set<DataTeam> getTeams() {
    return teams;
  }

  public Set<DataGame> getGames() {
    return games;
  }

  public Set<DataPlateAppearance> getPlateAppearances() {
    return plateAppearances;
  }

  // Caching variables for calculated fields - using composite data types here to allow null to
  // represent an un-initialized cache
  private transient Integer directOutCount;
  private transient Integer singleCount;
  private transient Integer doubleCount;
  private transient Integer tripleCount;
  private transient Integer homerunCount;
  private transient Integer walkCount;
  private transient Integer sacCount;
  private transient Integer atBatCount;
  private transient Integer errorCount;
  private transient Integer strikeoutCount;
  private transient Integer fcCount;
  private transient Integer outCount;
  private transient Integer hitCount;
  private transient Integer totalBases;
  private transient Double battingAverage;
  private transient Double sluggingPercentage;

  public int getOutCount() {
    calculateIfNull(outCount);
    return outCount;
  }

  public int getSingleCount() {
    calculateIfNull(singleCount);
    return singleCount;
  }

  public int getDoubleCount() {
    calculateIfNull(doubleCount);
    return doubleCount;
  }

  public int getTripleCount() {
    calculateIfNull(tripleCount);
    return tripleCount;
  }

  public int getHomerunCount() {
    calculateIfNull(homerunCount);
    return homerunCount;
  }

  public int getWalkCount() {
    calculateIfNull(walkCount);
    return walkCount;
  }

  public int getPlateAppearanceCount() {
    return plateAppearances.size();
  }

  public double getBattingAverage() {
    calculateIfNull(battingAverage);
    return battingAverage;
  }

  public double getSluggingPercentage() {
    calculateIfNull(sluggingPercentage);
    return sluggingPercentage;
  }

  public int getSacCount() {
    calculateIfNull(sacCount);
    return sacCount;
  }

  public int getStrikeoutCount() {
    calculateIfNull(strikeoutCount);
    return strikeoutCount;
  }

  public int getReachedOnErrorCount() {
    calculateIfNull(errorCount);
    return this.errorCount;
  }

  private void calculateIfNull(Object input) {
    if (input == null) {
      calculatePlayerStats();
    }
  }

  private int zeroIfNull(Integer object) {
    return Optional.ofNullable(object).orElse(0);
  }

  private void calculatePlayerStats() {
    // Build a map with the counts for each
    Map<String, Integer> results = new HashMap<>();
    for (DataPlateAppearance pa : this.plateAppearances) {
      Integer result = results.get(pa.getResult());
      if (result == null) {
        results.put(pa.getResult(), 1);
      } else {
        results.put(pa.getResult(), result + 1);
      }
    }

    Logger.log(results);

    // Remember these results so we don't have to calculate them again
    this.directOutCount =
        zeroIfNull(results.get("Out")) + zeroIfNull(results.get("DP")) + zeroIfNull(results.get("TP"));
    this.errorCount = zeroIfNull(results.get("E"));
    this.fcCount = zeroIfNull(results.get("FC"));

    // "êž°" and "\u2588" are both different encodings for the backward K unicode character
    this.strikeoutCount =
        zeroIfNull(results.get("K")) + zeroIfNull(results.get("Ʞ")) + zeroIfNull(results.get("\uA7B0"));
    this.sacCount = zeroIfNull(results.get("SAC"));
    this.walkCount = zeroIfNull(results.get("BB"));
    this.singleCount = zeroIfNull(results.get("1B"));
    this.doubleCount = zeroIfNull(results.get("2B"));
    this.tripleCount = zeroIfNull(results.get("3B"));
    this.homerunCount = zeroIfNull(results.get("HRi")) + zeroIfNull(results.get("HRo"));
    this.outCount = this.directOutCount + this.fcCount + this.strikeoutCount + this.errorCount; // Omits SAC/BB
    this.hitCount = this.singleCount + this.doubleCount + this.tripleCount + this.homerunCount;
    this.atBatCount = this.hitCount + this.outCount;
    this.totalBases = this.singleCount + this.doubleCount * 2 + this.tripleCount * 3 + this.homerunCount * 4;

    if (this.atBatCount == 0) {
      this.battingAverage = 0d;
      this.sluggingPercentage = 0d;
    } else {
      this.battingAverage = ((double) this.hitCount / (double) this.atBatCount);
      this.sluggingPercentage = ((double) (this.totalBases) / (double) this.atBatCount);
    }
  }

  private static final int NAME_PADDING = 24;

  public static String getListHeader() {
    return StringUtils.padRight("Name", NAME_PADDING) + " " + StringUtils.padRight("Id", 14) + " "
        + StringUtils.padRight("avg", 5) + " " + StringUtils.padRight("slg", 5);
  }

  @Override
  public String toString() {
    calculatePlayerStats();
    // Test
    return StringUtils.padRight(this.name, NAME_PADDING) + " " + this.id + " "
        + StringUtils.formatDecimal(this.getBattingAverage(), 3) + " "
        + StringUtils.formatDecimal(this.getSluggingPercentage(), 3);
    // + this.hitCount + " "
    // + this.atBatCount + " "
    // + this.getOutCount() + " "
    // + this.directOutCount + " "
    // + this.fcCount + " "
    // + this.strikeoutCount + " "
    // + this.errorCount + " "
    // + this.hitCount + " "
    // + this.singleCount + " "
    // + this.doubleCount + " "
    // + this.tripleCount + " "
    // + this.homerunCount + " ";

  }

}
