package com.github.thbrown.softballsim;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.util.Logger;

public class SoftballSim {

  public static void main(String[] args) throws ParseException {
    try {
      mainInternal(args);
    } catch (Exception e) {
      Logger.error(e.getMessage());
    }
  }

  public static void mainInternal(String[] args) throws ParseException {
    // The valid command line flags change based on which optimizer and data source are supplied.
    CommandLineOptions commandLineOptions = CommandLineOptions.getInstance();
    CommandLineParser parser = new DefaultParser();

    // If there were no arguments supplied, show help with only the common options (i.e. no additional
    // flags for optimizer, and the default flags for data source)
    Options availableOptions =
        commandLineOptions.getOptionsForFlags(DataSourceEnum.getEnumFromName(CommandLineOptions.SOURCE_DEFAULT), null);
    if (args.length == 0) {
      HelpFormatter formatter = CommandLineOptions.getInstance().getHelpFormatter();
      formatter.printHelp(CommandLineOptions.APPLICATION_NAME, CommandLineOptions.HELP_HEADER_1, availableOptions,
          CommandLineOptions.HELP_FOOTER);
      return;
    }

    // Some arguments have been supplied, parse only the common arguments for now
    Options commonOptions = commandLineOptions.getOptionsForFlags(null, null);
    String[] filterdArgs = commandLineOptions.filterArgsArray(args, commonOptions);
    CommandLine commonCmd = parser.parse(commonOptions, filterdArgs, true);

    String dataSourceString =
        commonCmd.getOptionValue(CommandLineOptions.DATA_SOURCE, CommandLineOptions.SOURCE_DEFAULT);
    DataSourceEnum dataSource = DataSourceEnum.getEnumFromName(dataSourceString);

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
      return;
    }

    // Manually enforce optimizer as a required flag. Other required flags should be specified in their
    // options definition so their presence is enforced during parse. Enforcing the optimizer flag here
    // manually here lets us work with a null optimizer above.
    if (optimizer == null) {
      throw new MissingArgumentException(
          Msg.MISSING_OPTIMIZER.args(OptimizerEnum.getValuesAsString()));
    }

    // Parse command line arguments - this time include the optimizer and dataSource specific flags
    CommandLine allCmd = parser.parse(availableOptions, args, false);

    dataSource.execute(allCmd);
  }

}
