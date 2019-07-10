package com.github.thbrown.softballsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CombinatoricsUtil {

  /**
   * Given a lineup of players, returns a list of all possible lineups.
   * @Deprecated avoid keeping all possible lineups in memory
   */
  public static List<List<Player>> permute(List<Player> original) {
    if (original.size() == 0) {
      List<List<Player>> result = new ArrayList<List<Player>>();
      result.add(new ArrayList<Player>());
      return result;
    }
    Player firstElement = original.remove(0);
    List<List<Player>> returnValue = new ArrayList<List<Player>>();
    List<List<Player>> permutations = permute(original);
    for (List<Player> smallerPermutated : permutations) {
      for (int index = 0; index <= smallerPermutated.size(); index++) {
        List<Player> temp = new ArrayList<Player>(smallerPermutated);
        temp.add(index, firstElement);
        returnValue.add(temp);
      }
    }
    return returnValue;
  }
  
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
    if(number < 0 || number > factorials.length) {
      throw new RuntimeException("Bad factorial" + number);
    }
    return factorials[number];
  }
  
  public static long binomial(int n, int k) {
    if(n == 0 || k > n) {
      return 0;
    }
    return (factorial(n)/(factorial(k)*factorial(n-k)));
  }
  
  public static <T> List<T> mapListToArray(List<T> list, int[] order) {
	List<T> toReturn = new ArrayList<>();
	for(int i = 0; i < order.length; i++) {
		toReturn.add(list.get(order[i]));
	}
	return toReturn;
  }
  
  // http://webhome.cs.uvic.ca/~ruskey/Publications/RankPerm/MyrvoldRuskey.pdf
  public static int[] getIthPermutation(int size, long i) {
	int[] initialOrder = new int[size];  
	for(int j = 0; j < size; j++) {
		initialOrder[j] = j;
	}
	return getIthPermutation(size, i, initialOrder);
  }
  
  private static int[] getIthPermutation(int size, long i, int[] initialOrder) {
	if(size > 0) { // 1 > 0
      swap(size-1, (int) (i % size), initialOrder); //io[1], io[0]  | io[0], io[0]
      return getIthPermutation(size-1, (int) Math.floor(i/size), initialOrder); //1, 0, [1,0,2] |
  	}
	return initialOrder;
  }
  
  // https://computationalcombinatorics.wordpress.com/2012/09/10/ranking-and-unranking-of-combinations-and-permutations/
  public static int[] getIthCombination(int k, long m) {
    int[] S = new int[k];
    int i = k - 1;
    while(i >= 0) {
      int l = i;
      while(binomial(l,i+1) <= m) {
        l = l + 1;
      }
      S[i] = l - 1;
      m = m - binomial(l-1, i+1);
      i = i - 1;
    }
    return S;
  }
  
  static private void swap(int indexOne, int indexTwo, int[] arrayToSwap) {
	int temp = arrayToSwap[indexOne];
	arrayToSwap[indexOne] = arrayToSwap[indexTwo];
	arrayToSwap[indexTwo] = temp;
  }
  
}
