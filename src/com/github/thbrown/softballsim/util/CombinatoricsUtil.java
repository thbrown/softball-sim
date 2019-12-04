package com.github.thbrown.softballsim.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CombinatoricsUtil {

  public static long[] factorials = {
      1,
      1,
      2,
      6,
      24,
      120,
      720,
      5040,
      40320,
      362880,
      3628800,
      39916800,
      479001600,
      6227020800L,
      87178291200L,
      1307674368000L,
      20922789888000L,
      355687428096000L,
      6402373705728000L,
      121645100408832000L,
      2432902008176640000L}; // Last factorial that fits in a long is 20!

  public static long factorial(int number) {
    if (number < 0 || number > factorials.length) {
      throw new RuntimeException("Bad factorial" + number);
    }
    return factorials[number];
  }

  /**
   * Calculate how many whys ther are to choose k elements from a set of n elements (n choose k)
   */
  public static long binomial(int n, int k) {
    if (n == 0 || k > n || k < 0) {
      return 0;
    }
    return (factorial(n) / (factorial(k) * factorial(n - k)));
  }

  public static <T> List<T> mapListToArray(List<T> list, int[] order) {
    List<T> toReturn = new ArrayList<>();
    for (int i = 0; i < order.length; i++) {
      toReturn.add(list.get(order[i]));
    }
    return toReturn;
  }

  // http://webhome.cs.uvic.ca/~ruskey/Publications/RankPerm/MyrvoldRuskey.pdf
  public static int[] getIthPermutation(int size, long i) {
    int[] initialOrder = new int[size];
    for (int j = 0; j < size; j++) {
      initialOrder[j] = j;
    }
    return getIthPermutation(size, i, initialOrder);
  }

  private static int[] getIthPermutation(int size, long i, int[] initialOrder) {
    if (size > 0) { // 1 > 0
      swap(size - 1, (int) (i % size), initialOrder); // io[1], io[0] | io[0], io[0]
      return getIthPermutation(size - 1, (int) Math.floor(i / size), initialOrder); // 1, 0, [1,0,2] |
    }
    return initialOrder;
  }

  // https://computationalcombinatorics.wordpress.com/2012/09/10/ranking-and-unranking-of-combinations-and-permutations/
  public static int[] getIthCombination(int k, long m) {
    int[] S = new int[k];
    int i = k - 1;
    while (i >= 0) {
      int l = i;
      while (binomial(l, i + 1) <= m) {
        l = l + 1;
      }
      S[i] = l - 1;
      m = m - binomial(l - 1, i + 1);
      i = i - 1;
    }
    return S;
  }

  static private void swap(int indexOne, int indexTwo, int[] arrayToSwap) {
    int temp = arrayToSwap[indexOne];
    arrayToSwap[indexOne] = arrayToSwap[indexTwo];
    arrayToSwap[indexTwo] = temp;
  }

  /**
   * Partitions count objects into buckets. https://en.wikipedia.org/wiki/Partition_(number_theory)
   */
  public static List<List<List<Integer>>> getPartitions(int count, int maxBucketSize, int maxBucketCount) {
    List<List<List<Integer>>> result = new ArrayList<>();

    List<List<Integer>> zeroResult = new ArrayList<>();
    zeroResult.add(new ArrayList<>());
    result.add(zeroResult);

    // We need to calculate results for all the partitions from 1 to count
    for (int counter = 1; counter <= count; counter++) {
      // System.out.println("Calculating " + counter + " of " + count);

      List<List<Integer>> resultForOneCount = new ArrayList<>();
      // We need to start with the highest value of each count and work our way to 0
      for (int progress = counter; progress > 0; progress--) {
        // System.out.println(progress + " of " + 0);
        List<Integer> partialResult = new ArrayList<>();
        partialResult.add(progress);

        int remaining = counter - progress;
        // System.out.println("remaining " + remaining);

        if (remaining == 0) {
          // This isn't a partial result, it's a full result
          // Only add the result if it fits into the max bucket size
          if (Collections.max(partialResult) <= maxBucketSize) {
            if (partialResult.size() <= maxBucketCount) { // Doesn't this always pass?
              resultForOneCount.add(partialResult);
            }
          }

          continue;
        } else {
          List<List<Integer>> resultsAtRemainingIndex = result.get(remaining);

          for (List<Integer> entry : resultsAtRemainingIndex) {
            // all values in entry are lower than progress
            if (Collections.max(entry) <= progress) {
              List<Integer> singleResult = new ArrayList<>();
              singleResult.addAll(partialResult);
              singleResult.addAll(entry);
              // Only add the result if it fits into the max bucket size
              if (Collections.max(singleResult) <= maxBucketSize) {
                if (singleResult.size() <= maxBucketCount) {
                  resultForOneCount.add(singleResult);
                }
              }
            }
          }
        }
      }
      // System.out.println(counter + ") " + resultForOneCount);

      result.add(resultForOneCount);
    }
    return result;
  }
}
