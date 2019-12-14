package com.github.thbrown.softballsim.data.gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.github.thbrown.softballsim.util.LevenshteinDistance;
import com.github.thbrown.softballsim.util.Logger;

public class DataStats {
  private List<DataPlayer> players;
  private List<DataTeam> teams;

  public List<DataPlayer> getPlayers() {
    return players;
  }

  public List<DataTeam> getTeams() {
    return teams;
  }

  public String getPlayersAsCommaSeparatedString() {
    return String.join(",", players.stream().map(v -> v.getId()).toArray(String[]::new));
  }

  transient Map<String, DataPlayer> playerMap;

  public void setPlayerMap(Map<String, DataPlayer> playerMap) {
    this.playerMap = playerMap;
  }

  public DataPlayer getPlayerById(String id) {
    return this.playerMap.get(id);
  }

  /**
   * Accepts a heterogeneous list of player ids or player names and converts it to a list of only
   * player ids. If a player name included in the list is not an exact match but the correct player
   * can be inferred by LevenshteinDistance, this method will print a warning but use the inferred
   * playerId.
   * 
   * @throws a runtime exception if names can not be resolved to an id because two or more
   *         possibilities are equally likely per their LevenshteinDistance (e.g. if the same name is
   *         used for a multiple players)
   */
  public List<String> convertPlayersListToIds(List<String> input) {
    List<String> result = new ArrayList<>();
    for (String playerString : input) {
      // First, if the value is a valid player id, just add it to result
      DataPlayer dataPlayer = this.getPlayerById(playerString);
      if (dataPlayer != null) {
        result.add(dataPlayer.getId());
      } else {
        // Second, iterate over the players to see if we can find a name that matches, if not remember the
        // closest name
        Integer lowestDistance = null;
        List<DataPlayer> lowestDistancePlayers = new ArrayList<>();
        for (DataPlayer iteratingDataPlayer : this.players) {
          int distance = LevenshteinDistance.getLevenshteinDistance(playerString, iteratingDataPlayer.getName());
          if (lowestDistance == null || distance < lowestDistance) {
            lowestDistancePlayers.clear();
            lowestDistancePlayers.add(iteratingDataPlayer);
            lowestDistance = distance;
          } else if (lowestDistance == distance) {
            lowestDistancePlayers.add(iteratingDataPlayer);
          }
        }
        if (lowestDistancePlayers.size() == 1) {
          // TODO: some cap for distance?
          // TODO: should we check ids for distance as well?
          if (lowestDistance != 0) {
            Logger.log("WARNING - assuming lineup player input '" + playerString + "' referes to player "
                + lowestDistancePlayers.get(0).getName() + " (" + lowestDistancePlayers.get(0).getId() + ") ");
          }
          result.add(lowestDistancePlayers.get(0).getId());
        } else if (lowestDistancePlayers.size() == 0) {
          throw new RuntimeException("No matches were found for player '" + playerString + "'");
        } else {
          String possiblePlayerNames =
              lowestDistancePlayers.stream().map(v -> v.getName()).collect(Collectors.joining(", "));
          throw new RuntimeException(
              "Player input is ambiguous '" + playerString + "' may be one of " + possiblePlayerNames);
        }
      }
    }
    return result;
  }


}
