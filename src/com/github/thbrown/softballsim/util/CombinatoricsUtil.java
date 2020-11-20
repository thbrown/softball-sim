package com.github.thbrown.softballsim.util;

import java.util.ArrayList;
import java.util.Arrays;
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
   * Calculate how many ways there are to choose k elements from a set of n elements (n choose k)
   */
  public static long binomial(int n, int k) {
    if (n == 0 || k > n || k < 0) {
      return 0;
    }
    return (factorial(n) / (factorial(k) * factorial(n - k)));
  }

  /**
   * Maps each element of a list to a new list based of the specification in 'order' e.g. if the 1st
   * element of 'order' is 8, the 8th element of the return list will be the first element of 'list'
   */
  public static <T> List<T> mapListToArray(List<T> list, int[] order) {
    List<T> toReturn = new ArrayList<>();
    for (int i = 0; i < order.length; i++) {
      toReturn.add(list.get(order[i]));
    }
    return toReturn;
  }

  public static <T> int[] getOrdering(List<T> shuffledList, List<T> originalList) {
    // TODO: can we do this better than n^2
    // TODO: check that lists are = length?
    int[] order = new int[originalList.size()];
    int counter = 0;
    for (int i = 0; i < originalList.size(); i++) {
      T originalElement = originalList.get(i);
      for (int j = 0; j < shuffledList.size(); j++) {
        T shuffledElement = shuffledList.get(j);
        if (originalElement.equals(shuffledElement)) {
          order[counter] = j;
          counter++;
          continue;
        }
      }
    }
    return order;
  }

  // TODO: make this return the 0,1,2,3,4 etc... as the first permutation
  // http://webhome.cs.uvic.ca/~ruskey/Publications/RankPerm/MyrvoldRuskey.pdf - unrank1
  public static int[] getIthPermutation(int size, long i) {
    // Index adjustment to make the 0th permutation match the initial order
    if (i == 0) {
      i = factorial(size) - 1;
    } else {
      i -= 1;
    }

    // MyrvoldRuskey
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

  // TODO: int isn't enough here (will fail on normal lineups of 13 players or more)
  // http://webhome.cs.uvic.ca/~ruskey/Publications/RankPerm/MyrvoldRuskey.pdf - rank1
  public static int getPermutationIndex(int[] permutation) {
    int[] permutaionCopy = permutation.clone();
    int[] inversePermutaion = new int[permutation.length];
    for (int i = 0; i < inversePermutaion.length; i++) {
      inversePermutaion[permutation[i]] = i;
    }
    int result = getPermutationIndex(permutation.length, permutaionCopy, inversePermutaion);

    // Index adjustment to make the 0th permutation match the initial order
    if (result == factorial(permutation.length) - 1) {
      return 0;
    } else {
      return result + 1;
    }
  }

  private static int getPermutationIndex(int size, int[] order, int[] inverseOrder) {
    if (size == 1) {
      return 0;
    }
    int something = order[size - 1];
    swap(size - 1, inverseOrder[size - 1], order);
    swap(something, size - 1, inverseOrder);
    return (something + size * getPermutationIndex(size - 1, order, inverseOrder));
  }

  // https://computationalcombinatorics.wordpress.com/2012/09/10/ranking-and-unranking-of-combinations-and-permutations/
  // unrank-co-lexographic
  public static int[] getIthCombination(int size, long index) {
    int k = size;
    long m = index;
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

  // https://computationalcombinatorics.wordpress.com/2012/09/10/ranking-and-unranking-of-combinations-and-permutations/
  // rank-co-lexographic
  public static long getCombinationIndex(int[] S) {
    int k = S.length;
    long sum = 0;
    for (int i = 0; i < k; i++) {
      sum += binomial(S[i], i + 1);
    }
    return sum;
  }

  static public void swap(int indexOne, int indexTwo, int[] arrayToSwap) {
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
