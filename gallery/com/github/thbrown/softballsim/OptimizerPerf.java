package com.github.thbrown.softballsim;

import com.github.thbrown.softballsim.optimizer.OptimizerEnum;

public class OptimizerPerf {

  private int optimizerId;
  private String optimizer;
  private String lineupType;
  private int lineupLength;
  private long executionTime;
  private double runsScored;

  private Scorer scorer;

  public OptimizerPerf(String tsvData, Scorer scorer) {
    // MONTE_CARLO_EXHAUSTIVE STANDARD 6 1968 5.5373
    String[] data = tsvData.split("\t");
    this.optimizer = data[0];
    this.lineupType = data[1];
    this.lineupLength = Integer.parseInt(data[2]);
    this.executionTime = Long.parseLong(data[3]);
    this.runsScored = Double.parseDouble(data[4]);
    this.scorer = scorer;
    this.optimizerId = OptimizerEnum.getEnumFromIdOrName(optimizer).getId();
    scorer.add(this.optimizer, this.lineupType, this.lineupLength, this.executionTime, this.runsScored);
  }

  public double getSpeedScore() {
    return this.scorer.getSpeedScore(this.optimizer, this.lineupType, this.lineupLength);
  }

  public double getQualityScore() {
    return this.scorer.getQualityScore(this.optimizer, this.lineupType, this.lineupLength);
  }

  public String getOptimizer() {
    return this.optimizer;
  }

  public String getLineupType() {
    return this.lineupType;
  }

  public int getLineupLength() {
    return lineupLength;
  }

  public int getOptimizerId() {
    return this.optimizerId;
  }

  @Override
  public String toString() {
    return optimizer.substring(optimizer.length() - 8, optimizer.length()) + "\t" + lineupType.substring(0, 5) + "\t"
        + lineupLength + "\t"
        + String.valueOf(getSpeedScore()).substring(0, 3)
        + "\t" + String.valueOf(getQualityScore()).substring(0, 3)
        + "\t" + String.valueOf(this.runsScored).substring(0, 3);
  }

}
