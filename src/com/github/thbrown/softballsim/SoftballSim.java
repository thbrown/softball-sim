package com.github.thbrown.softballsim;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.Logger;

public class SoftballSim {

  public static void main(String[] args) throws ParseException {
    try {
      mainInternal(args);
    } catch (Exception e) {
      Logger.error(e.getMessage());
      e.printStackTrace();
    }
  }

  public static Result mainInternal(String[] args) throws MissingArgumentException {
    // The valid command line flags change based on which optimizer and data source are supplied.
    CommandLineOptions commandLineOptions = CommandLineOptions.getInstance();

    // If there were no arguments supplied, show help with only the common options (i.e. no additional
    // flags for optimizer, and the default flags for data source)
    Options availableOptions =
        commandLineOptions.getOptionsForFlags(DataSourceEnum.getEnumFromName(CommandLineOptions.SOURCE_DEFAULT), null);
    if (args.length == 0) {
      HelpFormatter formatter = CommandLineOptions.getInstance().getHelpFormatter();
      formatter.printHelp(CommandLineOptions.APPLICATION_NAME, CommandLineOptions.HELP_HEADER_1, availableOptions,
          CommandLineOptions.HELP_FOOTER);
      return null;
    }

    // Some arguments have been supplied, parse only the common arguments for now
    Options commonOptions = commandLineOptions.getOptionsForFlags(null, null);
    CommandLine commonCmd = commandLineOptions.parse(commonOptions, args, true);

    String dataSourceString =
        commonCmd.getOptionValue(CommandLineOptions.DATA_SOURCE, CommandLineOptions.SOURCE_DEFAULT);
    DataSourceEnum dataSource = DataSourceEnum.getEnumFromName(dataSourceString);

    // Get players, lineup type, and optimizer from the cmd line flags
    String lineupTypeString =
        commonCmd.getOptionValue(CommandLineOptions.LINEUP_TYPE, CommandLineOptions.TYPE_LINEUP_DEFAULT);
    LineupTypeEnum lineupType = LineupTypeEnum.getEnumFromIdOrName(lineupTypeString);

    String playerString =
        commonCmd.getOptionValue(CommandLineOptions.LINEUP, ""); // TODO: make this required?
    List<String> players = Arrays.asList(playerString.split(","));

    OptimizerEnum optimizer = null;
    if (commonCmd.hasOption(CommandLineOptions.OPTIMIZER)) {
      String optimizerString = commonCmd.getOptionValue(CommandLineOptions.OPTIMIZER);
      optimizer = OptimizerEnum.getEnumFromIdOrName(optimizerString);
    }

    // Help will show the valid flags based on what optimizer and dataSource are supplied.
    availableOptions = commandLineOptions.getOptionsForFlags(dataSource, optimizer);
    if (commonCmd.hasOption(CommandLineOptions.HELP)) {
      String helpHeader =
          (optimizer == null) ? CommandLineOptions.HELP_HEADER_1 : CommandLineOptions.HELP_HEADER_2 + optimizer;
      HelpFormatter formatter = CommandLineOptions.getInstance().getHelpFormatter();
      formatter.printHelp(CommandLineOptions.APPLICATION_NAME, helpHeader, availableOptions,
          CommandLineOptions.HELP_FOOTER);
      return null;
    }

    return dataSource.execute(args, lineupType, players, optimizer);
  }

}
