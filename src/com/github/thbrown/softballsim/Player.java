package com.github.thbrown.softballsim;

import java.util.Random;
import java.util.TreeMap;

public class Player {

  private static final long SEED = System.currentTimeMillis();
  private static Random rand = new Random(SEED);

  String name;

  private int plateAppearances;
  private int singles;
  private int doubles;
  private int triples;
  private int homeRuns;
  private int walks;

  TreeMap<Integer, Integer> plateApperanceDistribution = new TreeMap<>();

  public Player(String name, int outs, int singles, int doubles, int triples, int homeRuns,
      int walks) {
    this.name = name;
    this.singles = singles;
    this.doubles = doubles;
    this.triples = triples;
    this.homeRuns = homeRuns;
    this.walks = walks;
    this.plateAppearances = singles + doubles + triples + homeRuns + walks + outs;

    plateApperanceDistribution.putIfAbsent(homeRuns, 4);
    plateApperanceDistribution.putIfAbsent(homeRuns + triples, 3);
    plateApperanceDistribution.putIfAbsent(homeRuns + triples + doubles, 2);
    plateApperanceDistribution.putIfAbsent(homeRuns + triples + doubles + singles + walks, 1);
    plateApperanceDistribution.putIfAbsent(plateAppearances, 0);
    System.out.println(String.format(
        "%s\t 1B: %d\t 2B: %d\t 3B: %d\t HR: %d\t BB: %d\t PA: %d",
        this, singles, doubles, triples, homeRuns, walks, plateAppearances));
  }

  public int hit() {
    int randomValue = rand.nextInt(plateAppearances) + 1;
    return plateApperanceDistribution.ceilingEntry(randomValue).getValue();
  }

  @Override
  public String toString() {
    return Simulation.padRight(this.name, 12) + Simulation.padRight(getAverage(), 8)
        + getSluggingPercentage();
  }

  private String getAverage() {
    double result = (double) (singles + doubles + triples + homeRuns + walks) / plateAppearances;
    return String.format(java.util.Locale.US, "%.3f", result);
  }

  private String getSluggingPercentage() {
    double result = (double) (singles * 1 + doubles * 2 + triples * 3 + homeRuns * 4)
        / plateAppearances;
    return String.format(java.util.Locale.US, "%.3f", result);
  }
}
