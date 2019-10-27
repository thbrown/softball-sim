package com.github.thbrown.softballsim;

import java.util.concurrent.*;

import com.github.thbrown.softballsim.util.Logger;

public class Player {

  private final String id; // Used to be name
  private final String gender;

  private final int outs;
  private final int singles;
  private final int doubles;
  private final int triples;
  private final int homeruns;
  private final int walks;
  private final int plateAppearances;
  
  // Primitive array is slightly faster than array list and significantly faster than tree map
  // This array is not bounded but I think that's okay. Worst case some malicious client will get an OOM error.
  private final int[] resultBucket;
  
  private Player(Player.Builder builder) {
    this.id = builder.id;
    this.gender = builder.gender;
    this.singles = builder.singles;
    this.doubles = builder.doubles;
    this.triples = builder.triples;
    this.homeruns = builder.homeruns;
    this.walks = builder.walks;
    this.outs = builder.outs;
    this.plateAppearances = singles + doubles + triples + homeruns + builder.outs;
    
    this.resultBucket = new int [this.plateAppearances];
    
    int index = 0;
    for(int i = 0; i < homeruns; i++) {
      resultBucket[index] = 4;
      index++;
    }
    for(int i = 0; i < triples; i++) {
      resultBucket[index] = 3;
      index++;
    }
    for(int i = 0; i < doubles; i++) {
      resultBucket[index] = 2;
      index++;
    }
    for(int i = 0; i < singles + walks; i++) {
      resultBucket[index] = 1;
      index++;
    }
    for(int i = 0; i < builder.outs; i++) {
      resultBucket[index] = 0;
      index++;
    }
    
    Logger.log(String.format(
        "%s\t 1B: %d\t 2B: %d\t 3B: %d\t HR: %d\t BB: %d\t PA: %d",
        this, singles, doubles, triples, homeruns, walks, plateAppearances));
    
    if(this.plateAppearances <= 0) {
      throw new RuntimeException("Each batter must have at least one plate apperaance");
    }
  }

  public int hit() {
    // TODO: Using a length that is a power of 2 is about 25% faster, there could be some optimization here
    int randomValue = ThreadLocalRandom.current().nextInt(resultBucket.length);
    return resultBucket[randomValue];
  }

  public String getName() {
    return id;
  }

  @Override
  public String toString() {
    return Simulation.padRight(this.id, 12) + Simulation.padRight(getAverage(), 8)
        + getSluggingPercentage();
  }

  public String getGender() {
    return gender;
  }

  public int getSingles() {
    return singles;
  }

  public int getDoubles() {
    return doubles;
  }

  public int getTriples() {
    return triples;
  }

  public int getHomeruns() {
    return homeruns;
  }

  public int getOuts() {
    return outs;
  }

  private String getAverage() {
    double result = (double) (singles + doubles + triples + homeruns + walks) / plateAppearances;
    return String.format(java.util.Locale.US, "%.3f", result);
  }

  public double getAverageNumeric() {
	    double result = (double) (singles + doubles + triples + homeruns + walks) / plateAppearances;
	    return result;
  }
  
  private String getSluggingPercentage() {
    double result = (double) (singles * 1 + doubles * 2 + triples * 3 + homeruns * 4)
        / plateAppearances;
    return String.format(java.util.Locale.US, "%.3f", result);
  }

  public static class Builder {
    private String id;
	private String gender;
    
    private int singles;
    private int doubles;
    private int triples;
    private int homeruns;
    private int walks;
    private int outs;

    public Builder(String id) {
      this.id = id;
    }

    public Player build() {
      return new Player(this);
    }

    public Builder player(Player player) {
      this.id = player.id;
      this.singles = player.singles;
      this.doubles = player.doubles;
      this.triples = player.triples;
      this.homeruns = player.homeruns;
      this.walks = player.walks;
      this.outs = player.plateAppearances - singles - doubles - triples - homeruns - walks;
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

    public Builder homeruns(int homeRuns) {
      this.homeruns = homeRuns;
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
    
    public Builder gender(String gender) {
    	this.gender = gender;
    	return this;
    }
  }
}
