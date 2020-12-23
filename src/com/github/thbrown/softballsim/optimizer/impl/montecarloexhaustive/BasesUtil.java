package com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive;

public class BasesUtil {

  private static HitResult[] hitResults = new HitResult[32];

  static {
    for (int i = 0; i < 32; i++) {
      // First three bits represent if a runner is present (1) on each base or not (0)
      boolean first = (i & 1) == 1;
      boolean second = (i >> 1 & 1) == 1;
      boolean third = (i >> 2 & 1) == 1;

      // Next two bits are the type of hit (00 - single, 01 - double, 10 - triple, 11 - homerun)
      // Add one so the value is the number of bases that a hit earns (e.g. double = 2 bases)
      int bases = (i >> 3) + 1;

      // Calculate the ending configuration
      int runsResultingFromHit = 0;
      for (int j = 0; j < bases; j++) {
        runsResultingFromHit += third ? 1 : 0;
        third = second;
        second = first;
        first = (j == 0) ? true : false;
      }

      // Save the results in a map for quick later lookup
      hitResults[i] = new HitResult(first, second, third, runsResultingFromHit);
    }
  }

  public static int updateRunsAndBasesAfterHit(int numBases, BasesState bases) {
    if (numBases == 0) {
      return 0;
    }
    // There 3 bases (2^3) that may or may not be holding a player
    // There are 4 non-out hit types (single, double, triple, hr) for 32 possibilities
    // We can enumerate all possibilities here for quick computation.
    int index = 0;
    // First three bits represent the presence (1) or absence (0) of runners on each base
    index |= bases.first ? 1 : 0;
    index |= bases.second ? 2 : 0;
    index |= bases.third ? 4 : 0;
    // Second two bits are the hit type
    index |= ((numBases - 1) << 3);

    // Lookup the solution in our map
    HitResult result = hitResults[index];
    bases.first = result.first;
    bases.second = result.second;
    bases.third = result.third;
    return result.runsScored;
  }

  // Class that holds info about what the field and score look like.
  public static class HitResult {
    HitResult(boolean first, boolean second, boolean third, int runsScored) {
      this.first = first;
      this.second = second;
      this.third = third;
      this.runsScored = runsScored;
    }

    public boolean first;
    public boolean second;
    public boolean third;
    public int runsScored;
  }

  // Class that holds info about whether or not a player is on each base
  public static class BasesState {
    public boolean first = false;
    public boolean second = false;
    public boolean third = false;

    public void clear() {
      first = false;
      second = false;
      third = false;
    }
  }

}
