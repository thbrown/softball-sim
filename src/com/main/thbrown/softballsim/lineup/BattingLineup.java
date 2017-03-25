package com.main.thbrown.softballsim.lineup;

import com.github.thbrown.softballsim.Player;

public interface BattingLineup {
	
	/**
	 * Returns the next player up to bat.
	 */
	public Player getNextBatter();
	
	/**
	 * This hook runs after a game has completed. Use this to ensure that the first player is up to bat
	 * at the beginning of the next simulated game.
	 */
	public void reset();
	
}
