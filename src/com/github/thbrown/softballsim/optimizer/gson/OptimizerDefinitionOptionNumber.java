package com.github.thbrown.softballsim.optimizer.gson;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

public class OptimizerDefinitionOptionNumber extends OptimizerDefinitionOption {

  private String defaultValue;
  private String max;
  private String min;
  private String step;

  public Option getCommandLineOption() {
    return Option.builder(super.getShortLabel())
        .longOpt(super.getLongLabel())
        .desc(super.getDescription())
        .hasArg(true)
        .required(false)
        .build();
  }

  @Override
  public String getKey() {
    return super.getLongLabel();
  }

  @Override
  public String getValue(CommandLine cmd) {
    String value = cmd.getOptionValue(super.getShortLabel(), defaultValue);

    if (value == null) {
      return value;
    }

    BigDecimal bigValue = new BigDecimal(value);

    if (step != null) {
      BigDecimal bigStep = new BigDecimal(step);
      String roundedValue = round(bigValue, bigStep, RoundingMode.HALF_UP).toPlainString();
      if (!value.equals(roundedValue)) {
        System.out.println("WARNING: The value provided for argurment " + super.getLongLabel() + " is " + value
            + " which does not match the required step. It will be rounded to " + roundedValue);
        value = roundedValue;
      }
    }

    if (max != null) {
      BigDecimal bigMax = new BigDecimal(max);
      if (bigValue.compareTo(bigMax) > 0) {
        throw new RuntimeException("The value provided for argurment " + super.getLongLabel() + " is " + value
            + " which is higher than the maximum value " + max);
      }
    }

    if (min != null) {
      BigDecimal bigMin = new BigDecimal(min);
      if (bigValue.compareTo(bigMin) < 0) {
        throw new RuntimeException("The value provided for argurment " + super.getLongLabel() + " is " + value
            + " which is lower than the minimum value " + min);
      }
    }

    return value;
  }

  public static BigDecimal round(BigDecimal value, BigDecimal increment, RoundingMode roundingMode) {
    if (increment.signum() == 0) {
      return value;
    } else {
      BigDecimal divided = value.divide(increment, 0, roundingMode);
      BigDecimal result = divided.multiply(increment);
      return result;
    }
  }

  public NumberType getType(String toTest) {
    try {
      Integer.parseInt(toTest);
      return NumberType.INTEGER;
    } catch (NumberFormatException e) {
      // not int
    }

    try {
      Float.parseFloat(toTest);
      return NumberType.FLOAT;
    } catch (NumberFormatException e) {
      // not float
    }
    return NumberType.INVALID;
  }

  public static double round(double input, double step) {
    return ((Math.round(input / step)) * step);
  }

  private enum NumberType {
    FLOAT,
    INTEGER,
    INVALID
  }

}
