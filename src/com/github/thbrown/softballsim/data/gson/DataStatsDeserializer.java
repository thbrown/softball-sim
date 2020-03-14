package com.github.thbrown.softballsim.data.gson;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * A custom deserializer for stats data that populates fields from json and creates references
 * between classes w/ relationships.
 * 
 * @author thomasbrown
 */
public class DataStatsDeserializer implements JsonDeserializer<DataStats> {

  @Override
  public DataStats deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {

    // First, deserialise StatsData using the gson's default deserialization
    DataStats stats = GsonAccessor.getInstance().getDefault().fromJson(json, DataStats.class);

    // Then, create references for the relationships between objects

    // To make these references, we'll need to build a lookup table for players by id
    Map<String, DataPlayer> playerLookup = new HashMap<>();
    for (DataPlayer player : stats.getPlayers()) {
      playerLookup.put(player.getId(), player);
    }
    stats.setPlayerMap(playerLookup);

    List<DataTeam> teams = stats.getTeams();
    for (DataTeam team : teams) {
      List<DataGame> games = team.getGames();
      Set<DataPlateAppearance> plateAppearancesForTeam = new HashSet<>();
      Set<String> playerIdsForTeam = new HashSet<>();
      for (DataGame game : games) {
        for (DataPlateAppearance plateAppearance : game.getPlateAppearances()) {
          // Set up player's relationships
          DataPlayer player = playerLookup.get(plateAppearance.getPlayer_id());
          player.addGame(game);
          player.addTeam(team);
          player.addPlateAppearences(plateAppearance);

          // Setup plateApperance's relationships
          plateAppearance.setGame(game);
          plateAppearance.setTeam(team);
          plateAppearance.setPlayer(player);

          // Populate lists we need for later to setup team relationships
          plateAppearancesForTeam.add(plateAppearance);
          playerIdsForTeam.add(plateAppearance.getPlayer_id());
        }

        // Setup game relationships
        game.setTeam(team);
        Set<DataPlayer> playersForGame = getPlayerDataFromIds(game.getLineup(), playerLookup);
        game.setPlayers(playersForGame);
      }

      // Setup team relationships
      team.setPlateApperances(plateAppearancesForTeam);
      Set<DataPlayer> playersForTeam = getPlayerDataFromIds(playerIdsForTeam, playerLookup);
      team.setPlayers(playersForTeam);
    }
    return stats;
  }

  private Set<DataPlayer> getPlayerDataFromIds(Collection<String> playerIds, Map<String, DataPlayer> lookup) {
    Set<DataPlayer> players = new HashSet<>();
    for (String playerId : playerIds) {
      players.add(lookup.get(playerId));
    }
    return players;
  }

}


