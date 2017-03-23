package com.github.thbrown.softballsim;

import com.github.thbrown.softballsim.lineupgen.LineupGenerator;

/**
 * Functional interface used for registering lineup generators with the CLI. Impls should be defined
 * Exclusively using lambdas in the main SoftballSim class.
 * @author thbrown
 */
public interface LineupGeneratorFactory {

	public LineupGenerator getLineupGenerator();
	
}
