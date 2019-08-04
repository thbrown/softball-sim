package com.github.thbrown.softballsim.commands;

import java.io.PrintWriter;

import com.github.thbrown.softballsim.helpers.ProcessHooks;

public class ErrorOptimizationCommand extends BaseOptimizationCommand {

	private String trace;
	private String message;
	
	@Override
	public boolean process(ProcessHooks ps, PrintWriter out) throws Exception {
		return ps.onError(this, out);
	}

	public String getTrace() {
		return trace;
	}

	public String getMessage() {
		return message;
	}
	
}
