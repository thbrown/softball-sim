package com.github.thbrown.softballsim.lineup;

import java.util.List;

import com.github.thbrown.softballsim.Player;

public class OrdinaryBattingLineup implements BattingLineup {

	private List<Player> players;

	private int hitterIndex = 0;

	public OrdinaryBattingLineup(List<Player> players) {
		this.players = players;
		if(players.size() <= 0) {
			String message = "You must include at least one player in the lineup.";
			throw new IllegalArgumentException(message);
		}
	}

	public Player getNextBatter() {
		Player selection = players.get(hitterIndex);
		hitterIndex = (hitterIndex+1)%players.size();
		return selection;
	}

	public void reset() {
		hitterIndex = 0;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Players").append("\n");;
		for(Player p : players) {
			result.append("\t").append(p).append("\n");
		}
		return result.toString();
	}
}
