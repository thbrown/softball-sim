package com.github.thbrown.softballsim.lineup;

import java.util.List;
import java.util.stream.Collectors;
import com.github.thbrown.softballsim.data.gson.DataPlayer;
import com.github.thbrown.softballsim.data.gson.DataStats;

public interface BattingLineup {

  /**
   * TODO
   */
  public List<DataPlayer> asList();

  /**
   * TODO
   */
  public default List<String> asListOfIds() {
    return asList().stream().map(p -> p.getId()).collect(Collectors.toList());
  };

  /**
   * Gets the batter at given index in the lineup
   */
  public DataPlayer getBatter(int index);

  /**
   * String used to identify the type of lineup during serialization/deserialization
   * 
   * @return
   */
  public default String getLineupType() {
    return this.getClass().getSimpleName();
  }

  /**
   * Player statistics aren't stored in serialised result data. So players will appear to have no
   * stats after deserialization. This method replaces the players w/ empty stats objects with their
   * counterparts from DataStats that do have stats info.
   */
  public void populateStats(DataStats battingData);

}
