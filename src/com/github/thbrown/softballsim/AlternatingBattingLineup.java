package com.github.thbrown.softballsim;

import java.util.List;

/**
 * Batting order that strictly alternates between two groups of players. (i.e. women and men or 
 * older and younger). Batters in a group with less players will bat more often.
 *  
 * @author thbrown
 */
public class AlternatingBattingLineup implements BattingLineup {
	
	private List<Player> groupA;
	private List<Player> groupB;
	
	private int groupAHitterIndex = 0;
	private int groupBHitterIndex = 0;
	
	private boolean selectFromGroupA = true;
	
	AlternatingBattingLineup(List<Player> groupA, List<Player> groupB) {
		this.groupA = groupA;
		this.groupB = groupB;
		if(groupA.size() <= 0 || groupB.size() <= 0) {
			String message = "You must include at least one player in each group " + 
					"groupA has " + groupA.size() + " players " + 
					"groupB has " + groupB.size() + " players.";
			throw new IllegalArgumentException(message);
		}
	}

	public Player getNextBatter() {
		Player selection;
		if(selectFromGroupA) {
			selection = groupA.get(groupAHitterIndex);
			groupAHitterIndex = (groupAHitterIndex+1)%groupA.size();
		} else {
			selection = groupB.get(groupBHitterIndex);
			groupBHitterIndex = (groupBHitterIndex+1)%groupB.size();
		}
		selectFromGroupA = !selectFromGroupA;
		return selection;
	}
	
	public void reset() {
		groupAHitterIndex = 0;
		groupBHitterIndex = 0;
	}
}
