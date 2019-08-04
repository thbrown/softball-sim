package com.github.thbrown.softballsim.commands;

import java.io.PrintWriter;
import com.github.thbrown.softballsim.helpers.ProcessHooks;

public abstract class BaseOptimizationCommand {
	
	private String command;
		
	public String getCommand() {
		return command;
	}

	/**
	 * All commands should should have a place where we can put code that will be run when that command is received.
	 * @param ps contains the code that should be invoked when the command is received
	 * @param out provides that code the ability to send a reply over the network
	 * @return 
	 */
	public abstract boolean process(ProcessHooks ps, PrintWriter out) throws Exception;
}
