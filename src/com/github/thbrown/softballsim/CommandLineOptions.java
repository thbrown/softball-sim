package com.github.thbrown.softballsim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;
import com.github.thbrown.softballsim.optimizer.impl.montecarloexhaustive.MonteCarloExhaustiveArgumentParser;

/**
 * Central point that controls what command line flags are valid.
 * 
 * Common arguments are defined here. Arguments that are only applicable to a specific optimizer or
 * data source are defined in the respective data source or optimizer class files.
 * 
 * Project convention is that common flags and flags applicable only for a particular data source
 * use short names made up of a single lower case character. Flags that are applicable only to a
 * particular optimizer use short names made up of a single upper case letter.
 * 
 * The terms 'flag' and 'argument' are used interchangeably.
 */
public class CommandLineOptions {

  public final static String APPLICATION_NAME = "java -jar softball-sim.jar";

  // Common Flags
  public final static String DATA_SOURCE = "d";
  public final static String LINEUP_TYPE = "t";
  public final static String OPTIMIZER = "o";
  public final static String HELP = "h";
  public final static String VERBOSE = "v"; // Should this be 'B' and use 'V' for version?
  public final static String LINEUP = "l";
  public final static String FORCE = "f";
  public final static String UPDATE_INTERVAL = "u";
  public final static String ESTIMATE_ONLY = "e";

  public final static String DATA_SOURCE_DEFAULT = "FILE_SYSTEM";
  public final static String TYPE_LINEUP_DEFAULT = "STANDARD";
  public final static String UPDATE_INTERVAL_DEFAULT = "5000";

  public final static int WIDTH = 110;

  // Help
  public final static String HELP_HEADER_1 =
      "An application for optimizing batting lineups using historical hitting data. Powered by by open source lineup optimization engines at https://github.com/thbrown/softball-sim. For more options, specify an optimizer. Run the application with the --help flag or see https://optimizers.softball.app to see a list of available optimizers.\t"
          + System.lineSeparator() + System.lineSeparator() + "options:";
  public final static String HELP_HEADER_2 = "Showing additional flags for ";
  public final static String HELP_FOOTER = System.lineSeparator() + System.lineSeparator()
      + String.join(" ", "example:", APPLICATION_NAME, "-" + DATA_SOURCE, "FILE_SYSTEM", "-" + OPTIMIZER,
          "MONTE_CARLO_EXHAUSTIVE", "-" + LINEUP, "Maya,PJ,Rex,Bodie,Lizzy,Julia",
          "-" + MonteCarloExhaustiveArgumentParser.GAMES, String.valueOf(10000),
          "-" + MonteCarloExhaustiveArgumentParser.INNINGS, String.valueOf(7));

  private final static CommandLineOptions INSTANCE = new CommandLineOptions();
  private final static CommandLineParser parser = new DefaultParser();

  public static CommandLineOptions getInstance() {
    return INSTANCE;
  }

  private HelpFormatter helpFormatter;

  private CommandLineOptions() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.setOptionComparator(getComparatorHelp());
    formatter.setWidth(WIDTH);
    this.helpFormatter = formatter;
  }

  public List<Option> getCommonOptions() {
    List<Option> commonOptions = new ArrayList<>();
    // Common options
    commonOptions.add(Option.builder(DATA_SOURCE).longOpt("data-source")
        .desc("Where to read the source data from (e.g. the stats file). Options are "
            + DataSourceEnum.getValuesAsString() + ". Default: " + DATA_SOURCE_DEFAULT)
        .hasArg(true).required(false).build());
    commonOptions.add(Option.builder(FORCE).longOpt("force").desc(
        "If this flag is provided, application will not attempt to use any previously calculated results from cache to resume the optimization from its state when it was interrupted or last run.")
        .hasArg(false).required(false).build());
    commonOptions.add(Option.builder(LINEUP_TYPE).longOpt("lineup-type")
        .desc("Type of lineup to be simulated. You may specify the name or the id. Options are "
            + LineupTypeEnum.getValuesAsString() + ". Default: " + TYPE_LINEUP_DEFAULT)
        .hasArg(true).required(false).build());
    commonOptions.add(Option.builder(OPTIMIZER).longOpt("optimizer").desc(
        "Required. The optimizer to be used to optimize the lineup. You may specify the name or the id. Options are "
            + OptimizerEnum.getValuesAsString() + ".")
        .hasArg(true).required(false) // This is a required field, but we'll enforce it manually (i.e. no using Apache
                                      // cli)
        .build());
    commonOptions.add(Option.builder(LINEUP).longOpt("players-in-lineup").desc(
        "Comma separated list of player ids that should be included in the optimized lineup. Defaults to all players.")
        .hasArg(true).required(false).build());
    commonOptions.add(Option.builder(HELP).longOpt("help")
        .desc(
            "Prints the available flags. Help output will change depending on the optimizer and dataSource specified.")
        .hasArg(false).required(false).build());
    commonOptions.add(Option.builder(VERBOSE).longOpt("verbose")
        .desc("In development. If present, print debuging details on error.").hasArg(false).required(false).build());
    commonOptions.add(Option.builder(UPDATE_INTERVAL).longOpt("update-interval").desc(
        "Time period, in milliseconds, that the application should report results. Default: " + UPDATE_INTERVAL_DEFAULT)
        .hasArg(true).required(false).build());
    commonOptions.add(Option.builder(ESTIMATE_ONLY).longOpt("estimate-only").desc(
        "If this flag is enabled, the application will only run for UPDATE_INTERVAL milliseconds. The produced result is useful for estimating optimization completion time.")
        .hasArg(false).required(false).build());
    return commonOptions;
  }

  /**
   * Gets the flags that are applicable for the command that has the specified dataSource and
   * optimizer. This method tolerates null dataSource and optimizer params.
   */
  public Options getOptionsForFlags(DataSourceEnum dataSource, OptimizerEnum optimizer) {
    Options options = new Options();

    // Options that are always available
    List<Option> optionsList = this.getCommonOptions();
    for (Option o : optionsList) {
      options.addOption(o);
    }

    // Options available for the selected data source
    if (dataSource != null) {
      optionsList = dataSource.getCommandLineOptions();
      for (Option o : optionsList) {
        options.addOption(o);
      }
    }

    // Options available for the selected optimizer
    if (optimizer != null) {
      optionsList = optimizer.getCommandLineOptions();
      for (Option o : optionsList) {
        options.addOption(o);
      }
    }
    return options;
  }

  /**
   * Takes an array of Strings and filters it such that the only elements remaining in the array are
   * valid command line options (as specified by the options param). This is used for filter to out
   * command line arguments that may be valid for a particular optimizer or data source but are not
   * applicable for all optimizers/data sources.
   */
  private String[] filterArgsArray(String[] args, Options options) {
    List<String> validEntries = new ArrayList<>();
    int flagArgs = 0;
    for (String arg : args) {
      // We previously encountered a valid flag, this is that flag's arguments
      if (flagArgs > 0) {
        validEntries.add(arg);
        flagArgs--;
      }

      // Remove leading dashes
      String nakedArg = arg.replaceAll("^-+", "");

      // Check for any flags we want to keep
      for (Option o : options.getOptions()) {
        if (nakedArg.equals(o.getOpt()) || nakedArg.equals(o.getLongOpt())) {
          validEntries.add(arg);
          flagArgs = o.getArgs();
          if (flagArgs == Option.UNLIMITED_VALUES) {
            // TODO: handle UNINITIALIZED and UNLIMITED_VALUES better?
            // UNLIMITED_VALUES is currently treated as no-arg
            throw new UnsupportedOperationException(
                "Unlimited args are not currently supported, consider accepting a delimited string and splitting it manually");
          }
          break;
        }
      }
    }
    return validEntries.stream().toArray(String[]::new);
  }

  private static Comparator<Option> getComparatorHelp() {
    // Case-sensitive, capitals first
    return new Comparator<Option>() {
      @Override
      public int compare(Option o1, Option o2) {
        return o1.toString().compareTo(o2.toString());
      }
    };
  }

  public HelpFormatter getHelpFormatter() {
    return this.helpFormatter;
  }

  /**
   * Wraps the Apache CLI parse method so we don't have to worry about the checked ParseException or
   * instantiating the default parser every time. This also provides alternate behavior for ignoring
   * spurious commands.
   * 
   * @param ignoreNonOptions when true commands in the arguments array that are not specified in
   *        options are ignored. When false these spurious commands throw an exception.
   */
  public CommandLine parse(Options options, String[] arguments, boolean ignoreNonOptions) {
    try {
      if (ignoreNonOptions) {
        arguments = this.filterArgsArray(arguments, options);
      }
      return parser.parse(options, arguments, false);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

}
