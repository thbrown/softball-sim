package com.github.thbrown.softballsim;

import java.util.concurrent.*;
import java.util.TreeMap;

public class Player {

  String name;

  private final int singles;
  private final int doubles;
  private final int triples;
  private final int homeRuns;
  private final int walks;
  private final int plateAppearances;

  private TreeMap<Integer, Integer> plateApperanceDistribution = new TreeMap<>();

  private Player(Player.Builder builder) {
    this.name = builder.name;
    this.singles = builder.singles;
    this.doubles = builder.doubles;
    this.triples = builder.triples;
    this.homeRuns = builder.homeRuns;
    this.walks = builder.walks;
    this.plateAppearances = singles + doubles + triples + homeRuns + walks + builder.outs;

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
    int randomValue = ThreadLocalRandom.current().nextInt(plateAppearances) + 1;
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

  public static class Builder {
    private String name;

    private int singles;
    private int doubles;
    private int triples;
    private int homeRuns;
    private int walks;
    private int outs;

    public Builder(String name) {
      this.name = name;
    }

    public Player build() {
      return new Player(this);
    }

    public Builder player(Player player) {
      this.name = player.name;
      this.singles = player.singles;
      this.doubles = player.doubles;
      this.triples = player.triples;
      this.homeRuns = player.homeRuns;
      this.walks = player.walks;
      this.outs = player.plateAppearances - singles - doubles - triples - homeRuns - walks;
      return this;
    }

    public Builder singles(int singles) {
      this.singles = singles;
      return this;
    }

    public Builder doubles(int doubles) {
      this.doubles = doubles;
      return this;
    }

    public Builder triples(int triples) {
      this.triples = triples;
      return this;
    }

    public Builder homeRuns(int homeRuns) {
      this.homeRuns = homeRuns;
      return this;
    }

    public Builder walks(int walks) {
      this.walks = walks;
      return this;
    }

    public Builder outs(int outs) {
      this.outs = outs;
      return this;
    }
  }
}
