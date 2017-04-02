package com.github.thbrown.softballsim.testing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.thbrown.softballsim.AtBatOutcome;

public class AtBatOutcomeTest {

  @Test
  public void isValidAtBatOutcome_true() {
    assertTrue(AtBatOutcome.isValidAtBatOutcome("0"));
    assertTrue(AtBatOutcome.isValidAtBatOutcome("1  "));
    assertTrue(AtBatOutcome.isValidAtBatOutcome("  2"));
    assertTrue(AtBatOutcome.isValidAtBatOutcome("3"));
    assertTrue(AtBatOutcome.isValidAtBatOutcome("4\n"));
    assertTrue(AtBatOutcome.isValidAtBatOutcome("BB"));
  }

  @Test
  public void isValidAtBatOutcome_false() {
    assertFalse(AtBatOutcome.isValidAtBatOutcome("-1"));
    assertFalse(AtBatOutcome.isValidAtBatOutcome("BBB"));
    assertFalse(AtBatOutcome.isValidAtBatOutcome("5"));
  }
}
