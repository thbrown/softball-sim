package com.github.thbrown.softballsim;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.lineupgen.LineupTypeEnum;

/**
 * Defines the args that can be passed to the command line and their properties.
 */
public class CommandLineOptions {
	
	public final static String APPLICATION_NAME = "java -jar softball-sim";
	
	// Common
	public final static String DATA_SOURCE = "d";
	public final static String ESTIMATE_ONLY = "e";
	public final static String LINEUP_TYPE = "t";
	public final static String OPTIMIZER = "o";
	public final static String HELP = "h";
	public final static String VERBOSE = "v";
	
	public final static String SOURCE_DEFAULT = "FILE_SYSTEM";
	public final static String TYPE_LINEUP_DEFAULT = "ORDINARY";
	
	// Optimizer - MONTE_CARLO_EXHAUSTIVE
	public final static String GAMES = "g";
	public final static String INNINGS = "i";
	public final static String THREADS = "t";
	
	public final static String GAMES_DEFAULT = "1000";
	public final static String INNINGS_DEFAULT = "7";
	public final static String THREADS_DEFAULT = String.valueOf(Runtime.getRuntime().availableProcessors());
	
	// DataSource - FILE_SYSTEM
	public final static String PATH = "p";
	
	public final static String PATH_DEFAULT = "./";
	
	// DataSource - NETWORK
	public final static String LOCAL_IP_ADDRESS = "l";
	public final static String OPTIMIZATION_ID = "k";
	public final static String CLEANUP_SCRIPT = "c";
	
	public final static String LOCAL_IP_ADDRESS_DEFAULT = "127.0.0.1";
	public final static String OPTIMIZATION_ID_DEFAULT  = "0000000000";
	
	// Help
	public final static String HELP_HEADER_1 = "An application for optimizing batting lineups using historical hitting data. Powered by by open source optimization engines at https://github.com/thbrown/softball-sim. For more options, specify an optimizer.";
	public final static String HELP_HEADER_2 = "Showing additional flags for ";
	public final static String HELP_FOOTER = String.join(" ", "Example:", APPLICATION_NAME, "-" + DATA_SOURCE, "FILE_SYSTEM", "-" + OPTIMIZER, "MONTE_CARLO_EXHAUSTIVE", "-" + GAMES, String.valueOf(10000), "-" + INNINGS, String.valueOf(9));
	
	private final static CommandLineOptions INSTANCE = new CommandLineOptions();

	public static CommandLineOptions getInstance() {
		return INSTANCE;
	}

	public List<Option> getCommonOptions() {
		List<Option> commonOptions = new ArrayList<>();
		// Common options
		commonOptions.add(Option.builder(DATA_SOURCE)
	      	.longOpt("data-source")
	      	.desc("Where to read the source data from. Options are " + DataSourceEnum.getValuesAsString() + ". Default: " + SOURCE_DEFAULT)
	      	.hasArg(true)
	      	.required(false)
	      	.build());   
		commonOptions.add(Option.builder(ESTIMATE_ONLY)
	      	.longOpt("estimate-only")
	      	.desc("In Development. " + "If this flag is provided, application will return an estimated completion time only, not the result.")
	      	.hasArg(false)
	      	.required(false)
	      	.build());
		commonOptions.add(Option.builder(LINEUP_TYPE)
	      	.longOpt("lineup-type")
	      	.desc("Type of lineup to be simulated. You may specify the name or the id. Options are " + LineupTypeEnum.getValuesAsString() + ". Default: " + TYPE_LINEUP_DEFAULT)
	      	.hasArg(true)
	      	.required(false)
	      	.build());    
		commonOptions.add(Option.builder(OPTIMIZER)
	      	.longOpt("optimizer")
	      	.desc("Required. The optimizer to be used to optimize the lineup. You may specify the name or the id. Options are " + OptimizerEnum.getValuesAsString() + ".")
	      	.hasArg(true)
	      	.required(false) // This is a required field, but we'll enforce it manually (i.e. no using Apache cli)
	      	.build());
		commonOptions.add(Option.builder(HELP)
	      	.longOpt("help")
	      	.desc("Prints the available flags. Help output will change depending on the optimizer and dataSource specified.")
	      	.hasArg(false)
	      	.required(false)
	      	.build());
		commonOptions.add(Option.builder(VERBOSE)
	      	.longOpt("verbose")
	      	.desc("In development. If present, print debuging details on error.")
	      	.hasArg(false)
	      	.required(false)
	      	.build());
		return commonOptions;
	}
	
	public List<Option> getOptimizerOptions(OptimizerEnum optimizer) {
		List<Option> options = new ArrayList<>();
		if(optimizer == OptimizerEnum.MONTE_CARLO_EXHAUSTIVE) {
			options.add(Option.builder(GAMES)
			      	.longOpt("games")
			      	.desc(OptimizerEnum.MONTE_CARLO_EXHAUSTIVE + ": Number of games to simulate for each lineup. Default: " + GAMES_DEFAULT)
			      	.hasArg(true)
			      	.required(false)
			      	.build()); 
			options.add(Option.builder(INNINGS)
			      	.longOpt("innings")
			      	.desc(OptimizerEnum.MONTE_CARLO_EXHAUSTIVE + ": Number of innings to simulate for each game. Default: " + INNINGS_DEFAULT)
			      	.hasArg(true)
			      	.required(false)
			      	.build());
			options.add(Option.builder(THREADS)
			      	.longOpt("threads")
			      	.desc(OptimizerEnum.MONTE_CARLO_EXHAUSTIVE + ": Number of threads to be used to run the optimization. Default: " + THREADS_DEFAULT)
			      	.hasArg(true)
			      	.required(false)
			      	.build()); 
		}
		return options;
	}
	
	public List<Option> getDataSourceOptions(DataSourceEnum dataSource) {
		List<Option> options = new ArrayList<>();
		if(dataSource == DataSourceEnum.FILE_SYSTEM) {
			options.add(Option.builder(PATH)
			      	.longOpt("path")
			      	.desc("In Development. " + DataSourceEnum.FILE_SYSTEM + ": Path to the stats files. This can be a directory or file. Default: " + PATH_DEFAULT)
			      	.hasArg(true)
			      	.required(false)
			      	.build()); 	
		} else if(dataSource == DataSourceEnum.NETWORK) {
			options.add(Option.builder(LOCAL_IP_ADDRESS)
			      	.longOpt("local-ip-address")
			      	.desc(DataSourceEnum.NETWORK + ": The ip address the application should attempt to connect to in order to get the information required to run the optimization. Default: " + LOCAL_IP_ADDRESS_DEFAULT)
			      	.hasArg(true)
			      	.required(false)
			      	.build());
			options.add(Option.builder(OPTIMIZATION_ID)
			      	.longOpt("optimization-id")
			      	.desc(DataSourceEnum.NETWORK + ": The id of the optimization information to be request from the server. Default: " + OPTIMIZATION_ID_DEFAULT)
			      	.hasArg(true)
			      	.required(false)
			      	.build());
			// Should this be common?
			options.add(Option.builder(CLEANUP_SCRIPT)
			      	.longOpt("cleanup-script")
			      	.desc(DataSourceEnum.NETWORK + ": If this flag is provided, the appliction will attempt to invoke ./cleanup.sh (linux, osx) or ./cleanup.bat (windows) after the optimization completes.")
			      	.hasArg(false)
			      	.required(false)
			      	.build()); 
		}
		return options;
	}
	
	public Options getOptionsForFlags(DataSourceEnum dataSource, OptimizerEnum optimizer) {
		Options options = new Options();
		List<Option> optionsList = this.getCommonOptions();
		for(Option o : optionsList) {
			options.addOption(o);
		}
		
		if(dataSource != null) {
			optionsList = this.getDataSourceOptions(dataSource);
			for(Option o : optionsList) {
				options.addOption(o);
			}
		}
		
		if(optimizer != null) {
			optionsList = this.getOptimizerOptions(optimizer);
			for(Option o : optionsList) {
				options.addOption(o);
			}
		}
		return options;
	}

	/**
	 * Takes an array of Strings and filters it such that the only elements 
	 * remaining in the array are valid command line options (as specified by the options param).
	 */
	public String[] filterArgsArray(String[] args, Options options) {
		List<String> validEntries = new ArrayList<>();
		int flagArgs = 0;
		for(String arg : args) {
			// We previously encountered a valid flag, this is that flag's arguments
			if(flagArgs > 0) {
				validEntries.add(arg);
				flagArgs--;
			}
			
			// Remove leading dashes
			String nakedArg = arg.replaceAll("^-+", "");
			
			// Check for any flags we want to keep
			for(Option o : options.getOptions()) {
				if(nakedArg.equals(o.getOpt()) || nakedArg.equals(o.getLongOpt()) ) {
					validEntries.add(arg);
					flagArgs = o.getArgs();
					break;
				}
			}
		}
		return validEntries.stream().toArray(String[]::new);
	}
	
}
