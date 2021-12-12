package com.github.thbrown.softballsim.data.gson.helpers;

import com.github.thbrown.softballsim.data.gson.DataPlayer;

/**
 * Serialized lineups don't save stats info. When a lineup is deserialized it's often desirable to
 * re-populate those stats with data from existing classes. Implementing classes can be passed to
 * the GSON deserializer used to deserialized the batting lineup to replace the statless DataPlyer
 * from the batting lineup wiht the statful DataPlayer from the class.
 */
public interface DataPlayerLookup {
  /**
   * This should return null if there is no stats available for a given playerId \
   */
  public DataPlayer getDataPlayer(String playerId);

  // In case gson deserializer objects are expensive, we can cache them and retrieve them when we need
  // them
  // public String getLookupKey();
}
