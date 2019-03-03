package com.github.thbrown.softballsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CombinatoricsUtil {

  /**
   * Given a lineup of players, returns a list of all possible lineups.
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
  
  public static long factorial(int number) {
      long result = 1;
      for (int factor = 2; factor <= number; factor++) {
          result *= factor;
      }
      return result;
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
  
  static private void swap(int indexOne, int indexTwo, int[] arrayToSwap) {
	int temp = arrayToSwap[indexOne];
	arrayToSwap[indexOne] = arrayToSwap[indexTwo];
	arrayToSwap[indexTwo] = temp;
  }
}
