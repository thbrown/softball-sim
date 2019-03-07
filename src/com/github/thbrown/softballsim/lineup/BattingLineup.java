package com.github.thbrown.softballsim.lineup;

import java.util.List;
import java.util.Map;

import com.github.thbrown.softballsim.Player;

public interface BattingLineup {

  /**
   * Returns the next player up to bat.
   */
  public Player getNextBatter();

  /**
   * This hook runs after a game has completed. Use this to ensure that the
   * first player is up to bat at the beginning of the next simulated game.
   */
  public void reset();
  
  /**
   * Gets a map representation of the result so it can be easily serialized into json
   * @return 
   */
  public Map<String, List<String>> toMap();
  
  public BattingLineup getRandomSwap();

}
