package com.github.thbrown.softballsim.testing;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.github.thbrown.softballsim.CombinatoricsUtil;

public class PermutationUtilTest {

  @Test
  public void factorial() {
    assertEquals(CombinatoricsUtil.factorial(4), 24);
    assertEquals(CombinatoricsUtil.factorial(5), 120);
  }
  
  @Test
  public void generateNthPermutation() {
	  Set<String> dupDetector = new HashSet<>();
	  int length = 5;
	  for(int i = 0 ; i < CombinatoricsUtil.factorial(length); i++) {
		  String result = Arrays.toString(CombinatoricsUtil.getIthPermutation(length, i));
		  boolean isSuccess = dupDetector.add(result);
		  if(!isSuccess) {
			  throw new RuntimeException("Duplicate value detected");
		  }
	  }
  }

}
