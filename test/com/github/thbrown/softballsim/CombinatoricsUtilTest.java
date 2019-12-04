package com.github.thbrown.softballsim;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import com.github.thbrown.softballsim.util.CombinatoricsUtil;

public class CombinatoricsUtilTest {

  @Test
  public void factorial() {
    assertEquals(CombinatoricsUtil.factorial(4), 24);
    assertEquals(CombinatoricsUtil.factorial(5), 120);
  }

  @Test
  public void generateNthPermutation() {
    Set<String> dupDetector = new HashSet<>();
    int length = 5;
    for (int i = 0; i < CombinatoricsUtil.factorial(length); i++) {
      String result = Arrays.toString(CombinatoricsUtil.getIthPermutation(length, i));
      boolean isSuccess = dupDetector.add(result);
      if (!isSuccess) {
        throw new RuntimeException("Duplicate value detected" + result);
      }
    }
  }

  @Test
  public void generateNthCombination() {
    Set<String> dupDetector = new HashSet<>();
    int length = 8;
    int numberToChoose = 4;
    for (int i = 0; i < CombinatoricsUtil.binomial(length, numberToChoose); i++) {
      String result = Arrays.toString(CombinatoricsUtil.getIthCombination(numberToChoose, i));
      boolean isSuccess = dupDetector.add(result);
      if (!isSuccess) {
        throw new RuntimeException("Duplicate value detected" + result);
      }
    }
  }

  @Test
  public void generatePartitions() {
    // https://oeis.org/A000041
    int[] correctSizes = {1, 1, 2, 3, 5, 7, 11, 15, 22, 30, 42, 56, 77, 101, 135, 176, 231, 297, 385, 490, 627, 792,
        1002, 1255, 1575, 1958, 2436, 3010, 3718, 4565, 5604};

    List<List<List<Integer>>> results =
        CombinatoricsUtil.getPartitions(correctSizes.length, Integer.MAX_VALUE, Integer.MAX_VALUE);
    for (int i = 0; i < correctSizes.length; i++) {
      assertEquals(results.get(i).size(), correctSizes[i]);
    }
  }

  @Test
  public void generatePartitionsWithBucketSizeLimit() {
    final int MAX_BUCKET_SIZE = 5;
    List<List<List<Integer>>> results = CombinatoricsUtil.getPartitions(30, MAX_BUCKET_SIZE, Integer.MAX_VALUE);
    for (int i = 1; i < results.size(); i++) {
      int maxPartitionSize = results
          .get(i)
          .stream()
          .map(s -> Collections.max(s))
          .reduce((a, b) -> Math.max(a, b))
          .orElse(Integer.MAX_VALUE);
      assertEquals(true, maxPartitionSize <= MAX_BUCKET_SIZE);
    }
  }

  @Test
  public void generatePartitionsWithBucketCpuntLimit() {
    final int MAX_BUCKET_COUNT = 5;
    List<List<List<Integer>>> results = CombinatoricsUtil.getPartitions(30, Integer.MAX_VALUE, MAX_BUCKET_COUNT);
    for (int i = 1; i < results.size(); i++) {
      int maxPartitionSize = results
          .get(i)
          .stream()
          .map(s -> s.size())
          .reduce((a, b) -> Math.max(a, b))
          .orElse(Integer.MAX_VALUE);
      assertEquals(true, maxPartitionSize <= MAX_BUCKET_COUNT);
    }
  }

}
