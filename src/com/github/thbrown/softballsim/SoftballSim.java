package com.github.thbrown.softballsim;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.github.thbrown.softballsim.lineupgen.AlternatingBattingLineupGenerator;
import com.github.thbrown.softballsim.lineupgen.LineupGenerator;
import com.github.thbrown.softballsim.lineupgen.OrdinaryBatteryLineupGenerator;
import com.main.thbrown.softballsim.lineup.BattingLineup;

public class SoftballSim {
	// Config -- TODO convert these to flags w/ defaults
	public static final int INNINGS_PER_GAME = 6;
	public static final int GAMES_TO_SIMULATE = 1000;
	public static final boolean VERBOSE = false;
	public static final int NAME_PADDING = 24; // Just for formatting verbose output
	public static final String STATS_FILE_PATH = System.getProperty("user.dir") + File.separator + "stats";
	
	// Register all new batting lineups here
	public static final Map<Integer, LineupGeneratorFactory> LINEUP_TYPES = new HashMap<>();
	static {
		LINEUP_TYPES.put(0, () -> new OrdinaryBatteryLineupGenerator());
		LINEUP_TYPES.put(1, () -> new AlternatingBattingLineupGenerator());
	}

	public static void main(String[] args) {
		
		// User input validation stuff
		if(args.length == 0) {
			System.out.println("usage: java " + getApplicationName() + " lineupGeneratorNumber");
			System.out.println("\tExpecting input files in " + STATS_FILE_PATH);
			printAvailableLineupTypes();
			System.exit(0);
		}
		
		Integer selection = null;
		try {
			selection = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			System.out.println("Selection must be numerical. Was " + args[0]);
			System.exit(1);
		}
		
		LineupGeneratorFactory generatorFactory = LINEUP_TYPES.get(selection);
		if(generatorFactory  == null) {
			System.out.println("Invalid lineup type selected. Was " + selection);
			printAvailableLineupTypes();
			System.exit(1);
		}
		LineupGenerator generator = generatorFactory.getLineupGenerator();
		generator.readInDataFromFile(STATS_FILE_PATH);

		// More fun simulation stuff
		double bestResult = 0;
		BattingLineup bestLineup = null;

		System.out.println("*********************************************************************");
		System.out.println("Games simulated per lineup: " + GAMES_TO_SIMULATE);
		System.out.println("Innings per game: " + INNINGS_PER_GAME);
		System.out.println("*********************************************************************");

		BattingLineup lineup;
		while ((lineup = generator.getNextLienup()) != null){
			System.out.print(".");

			Simulation s = new Simulation(lineup);
			double result = s.run(GAMES_TO_SIMULATE);

			if(result > bestResult) {
				bestResult = result;
				bestLineup = lineup;
			}
		}
		
		System.out.println();
		System.out.println("Best lineup");
		System.out.println(bestLineup);
		System.out.println("Best lineup mean runs scored: " + bestResult);
	} 
	
	public static void printAvailableLineupTypes() {
		System.out.println("\tAvailable lineup generators:");
		for(Integer i : LINEUP_TYPES.keySet()) { 
			System.out.println("\t\t" + i + ") " + LINEUP_TYPES.get(i).getLineupGenerator().getClass().getSimpleName());
		}
	}

	public static String getApplicationName() {
		// TODO: get simple class name from Thread.currentThread().getStackTrace()[2].getClassName());
		return "SoftballSim";
	}
	

}
