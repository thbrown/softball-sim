package com.github.thbrown.softballsim.lineup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.github.thbrown.softballsim.Player;

public class OrdinaryBattingLineup implements BattingLineup {

  private List<Player> players;
  private int hitterIndex = 0;

  public OrdinaryBattingLineup(List<Player> players) {
    this.players = players;
    if (players.size() <= 0) {
      throw new IllegalArgumentException("You must include at least one player in the lineup.");
    }
  }

  @Override
  public Player getNextBatter() {
    Player selection = players.get(hitterIndex);
    hitterIndex = (hitterIndex + 1) % players.size();
    return selection;
  }

  @Override
  public void reset() {
    hitterIndex = 0;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("Players").append("\n");
    for (Player p : players) {
      result.append("\t").append(p).append("\n");
    }
    return result.toString();
  }

  @Override
  public BattingLineup getRandomSwap() {
    List<Player> players = new ArrayList<>();
    players.addAll(this.players);

    int randomValueA = ThreadLocalRandom.current().nextInt(players.size());
    int randomValueB = ThreadLocalRandom.current().nextInt(players.size()-1);

    if(randomValueB >= randomValueA) {
      randomValueB = randomValueB + 1;
    }
    
    Player temp = players.get(randomValueA);
    players.set(randomValueA, players.get(randomValueB));
    players.set(randomValueB, temp);
    
    return new OrdinaryBattingLineup(players);
  }
}
