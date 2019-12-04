package com.github.thbrown.softballsim.commands;

import java.io.PrintWriter;
import com.github.thbrown.softballsim.helpers.ProcessHooks;

public class ReadyOptimizationCommand extends BaseOptimizationCommand {

  @Override
  public boolean process(ProcessHooks ps, PrintWriter out) throws Exception {
    return ps.onReady(this, out);
  }

}
