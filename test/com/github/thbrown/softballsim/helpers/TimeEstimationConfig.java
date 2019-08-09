package com.github.thbrown.softballsim.helpers;

/**
 * Class for serializing/deserializing time estimation constants
 */
public class TimeEstimationConfig {
  private int iterations;
  private int innings;
  private int threads;
  private int lineupType;
  private long lineupCount;
  private double[] coefficients;
  private double[] errorAdjustments;

  public int getIterations() {
    return iterations;
  }

  public void setIterations(int iterations) {
    this.iterations = iterations;
  }

  public int getInnings() {
    return innings;
  }

  public void setInnings(int innings) {
    this.innings = innings;
  }

  public int getThreads() {
    return threads;
  }

  public void setThreads(int threads) {
    this.threads = threads;
  }

  public int getLineupType() {
    return lineupType;
  }

  public void setLineupType(int lineupType) {
    this.lineupType = lineupType;
  }

  public double[] getCoefficients() {
    return coefficients;
  }

  public void setCoefficients(double[] coefficients) {
    this.coefficients = coefficients;
  }

  public double[] getErrorAdjustments() {
    return errorAdjustments;
  }

  public void setErrorAdjustments(double[] errorAdjustments) {
    this.errorAdjustments = errorAdjustments;
  }
  
  public long getLineupCount() {
    return lineupCount;
  }

  public void setLineupCount(long lineupCount) {
    this.lineupCount = lineupCount;
  }
  
  public long estimateSimulationTimeInMillis(int threads, double teamAverage, int innings, int iterations, int lineupType, long numLineups) {
    double x = teamAverage;
    double y = 0;
    for (int i = 0; i < coefficients.length; i++) {
      double coeff = coefficients[i];
      y = y + coeff * Math.pow(x, i);
    }
    // Linear adjustments

    // Adjust for CPU core count (inverse relationship)
    y *= this.threads / threads;
    y *= this.errorAdjustments[threads] + 1;

    // Adjust for number of iterations
    y *= iterations / this.iterations;

    // Adjust for number of innings simulated
    y *= innings / this.innings;

    // Adjust for number for lineups
    // TODO: add this to lineup gen classes
    y *= numLineups / this.lineupCount;

    //Return the time in milliseconds
    return (long)y;
  }
}
