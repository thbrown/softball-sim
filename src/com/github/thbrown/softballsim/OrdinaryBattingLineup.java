package com.github.thbrown.softballsim;

import java.util.List;

public class OrdinaryBattingLineup {

	private List<Player> players;

	private int hitterIndex = 0;

	OrdinaryBattingLineup(List<Player> players) {
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
}
