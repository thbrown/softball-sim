package com.github.thbrown.softballsim;

import java.util.ArrayList;
import java.util.List;

public class PermutationGeneratorUtil {
	
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
			for (int index=0; index <= smallerPermutated.size(); index++) {
				List<Player> temp = new ArrayList<Player>(smallerPermutated);
				temp.add(index, firstElement);
				returnValue.add(temp);
			}
		}
		return returnValue;
	}
	
}
