package com.github.thbrown.softballsim.lineup;

import java.util.List;
import java.util.stream.Collectors;
import com.github.thbrown.softballsim.data.gson.DataPlayer;

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
    return AlternatingBattingLineup.class.getSimpleName();
  }

}
