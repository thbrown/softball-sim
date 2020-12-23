package com.github.thbrown.softballsim.optimizer.impl.montecarloadaptive;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class ActiveLineupCompositeTracker {

  private Set<LineupComposite> inProgress;
  private Queue<LineupComposite> ready;

  public Set<LineupComposite> getAllActiveLineups() {
    Set<LineupComposite> all = new HashSet<>();
    all.addAll(ready);
    all.addAll(inProgress);
    return all;
  }

}
