package com.github.thbrown.softballsim.helpers;

import java.io.PrintWriter;

import com.github.thbrown.softballsim.commands.CompleteOptimizationCommand;
import com.github.thbrown.softballsim.commands.ErrorOptimizationCommand;
import com.github.thbrown.softballsim.commands.InProgressOptimizationCommand;
import com.github.thbrown.softballsim.commands.ReadyOptimizationCommand;

public interface ProcessHooks {

	public default boolean onReady(ReadyOptimizationCommand data, PrintWriter out) throws Exception {
		return false;
	}
	
	public default boolean onInProgress(InProgressOptimizationCommand inProgressOptimizationCommand, PrintWriter out) {
		return false;
	}

	public default boolean onComplete(CompleteOptimizationCommand data, PrintWriter out) throws Exception {
		return true;
	}

	public default boolean onError(ErrorOptimizationCommand errorOptimizationCommand, PrintWriter out) {
		return true;
	}

}
