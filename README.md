# SoftballSim
Monte Carlo simulation tool for making sure you choose the best lineup for your softball, baseball, or kickball team. Used by softball.app for lineup optimizations but can also be run from the command line on file system data.

Build and Run (requires java, replace `./gradlew` with `./gradlew.bat` for windows machines):
```
./gradlew build

# Example
java -jar ./build/libs/softball-sim.jar -o MONTE_CARLO_EXHAUSTIVE --games 10000 --innings 7

# See help for available options
java -jar ./build/libs/softball-sim.jar --help
```

To run tests:
```
./gradlew clean test --info
```

### Flags

Flags (more command line flags will be available based on which optimizer and data source are supplied):
```
 -d,--data-source <arg>   Where to read the source data from. Options are
                          [FILE_SYSTEM, NETWORK]. Default: FILE_SYSTEM
 -e,--estimate-only       In Development. If this flag is provided,
                          application will return an estimated completion
                          time only, not the result.
 -h,--help                Prints the available flags. Help output will
                          change depending on the optimizer and dataSource
                          specified.
 -o,--optimizer <arg>     Required. The optimizer to be used to optimize
                          the lineup. You may specify the name or the id.
                          Options are [MONTE_CARLO_EXHAUSTIVE - 0].
 -p,--path <arg>          In Development. FILE_SYSTEM: Path to the stats
                          files. This can be a directory or file. Default:
                          ./
 -t,--lineup-type <arg>   Type of lineup to be simulated. You may specify
                          the name or the id. Options are
                          [MONTE_CARLO_EXHAUSTIVE - 0]. Default: ORDINARY
 -v,--verbose             In development. If present, print debuging
                          details on error.
```

#### optimizer Options

* 0 - MONTE_CARLO_EXHAUSTIVE

#### dataSource Options

* FILE_SYSTEM - Gets data from files in the `./stats` directory. See the `stats` directory in this repository for example files. Application will attempt to read data of all files in the `./stats` directory.

* NETWORK - Gets data from a network connection. This option is intended to be used mostly by softball-scorer app (https://github.com/thbrown/softball-scorer).

#### lineupType Options

Available lineup generators:
*  1 - ORDINARY
   *  Expects data formated like: `Bashful,4,0,0,0`
   *  Which is interpreted as *(name, hits... (homerun, out, out, out)*
*  2 - ALTERNATING_GENDER
   *  Expects data formated like: `Hermione Granger,B,1,0,2,1` 
   *  Which is interpreted as *(name, group \[e.g. A=male, B=female\], hits... (single, out, double, single)*
*  3 - NO_CONSECUTIVE_FEMALES
   *  Expects data formatted the same way as AlternatingBattingLineupGenerator except expect second value must be gender (A=male, B=female)

## Notes

Hit data is an integer 0-4 (inclusive) that represents the number of bases the player reached in each of their at bats. Explicitly:
*  0 => out/error/fielder's choice
*  1 => single/walk
*  2 => double
*  3 => triple
*  4 => homerun
		
## Extending the simulation
To add your own lineup generator:
1. Implement BattingLienup and LineupGenerator.
1. Register your new generator in the static map in the LineupType class.
