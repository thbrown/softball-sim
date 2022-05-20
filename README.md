# SoftballSim

Lineup optimization tool for making sure you choose the best batting order for your softball, baseball, or kickball team. Used by softball.app for lineup optimizations but can also be run from the command line on file system data.

### Build (requires Java 11\*, in the instructions below replace `./gradlew` with `./gradlew.bat` for windows machines):

```
# Production Build (Note: this runs tests, formats code, optimizes jar)
./gradlew build

# Developement Build without running tests (skips test and jar optimizations)
./gradlew build -x test -x proguard

```
This will generate a runnable JAR named softball-sim.jar in ./build/libs

### Run

By default the stats used for the optimization can be found in `./stats/exampleData.json`. You can replace or edit this file to use different stats.

```
# Example
java -jar ./build/libs/softball-sim.jar -o MONTE_CARLO_EXHAUSTIVE --Games 10000 --Innings 7 -l "Devon,Aaron,Fred,Arnold,Simon,Artimis,Grant,Julia"

# See help for available options
java -jar ./build/libs/softball-sim.jar --help

# Or use gradle, be sure to provide arguments to the run task in the build.gradle file
./gradlew run
```

### Test/Debug

```
# Run all tests, ignore cache, show output (this will remove an exiting built jar, run `./gradlew build -x test` to regenerate)
./gradlew clean test --info

# Run a particular test/tests based on a filter, show output, ignore cached results, and wait for remote debugger to be attached
./gradlew test -info --rerun-tasks --tests Aggregate* --debug-jvm
```

### Flags

Flags (more command line flags will be available based on which optimizer and data source are supplied):

```
 -d,--data-source <arg>         Where to read the source data from. Options are [FILE_SYSTEM,
                                GCP_BUCKETS]. Default: FILE_SYSTEM
 -f,--force                     If this flag is provided, application will not attempt to use any
                                previously calculated results from the /cache directory to resume
                                the optimization from its state when it was interrupted or last run.
 -h,--help                      Prints the available flags. Help output will change depending on the
                                optimizer and dataSource specified.
 -l,--players-in-lineup <arg>   Comma separated list of player ids that should be included in the
                                optimized lineup. Defaults to all players.
 -o,--optimizer <arg>           Required. The optimizer to be used to optimize the lineup. You may
                                specify the name or the id. Options are [MONTE_CARLO_EXHAUSTIVE - 0,
                                MONTE_CARLO_ADAPTIVE - 1, MONTE_CARLO_ANNEALING - 2, EXPECTED_VALUE
                                - 3].
 -p,--path <arg>                FILE_SYSTEM: Path to the stats files. This can be a directory or
                                file. Default: ./stats
 -t,--lineup-type <arg>         Type of lineup to be simulated. You may specify the name or the id.
                                Options are [STANDARD - 0, ALTERNATING_GENDER - 1,
                                NO_CONSECUTIVE_FEMALES - 2, NO_CONSECUTIVE_FEMALES_AND_NO_THREE_CONSECUTIVE_MALES - 3]. Default: STANDARD
 -v,--verbose                   In development. If present, print debuging details on error.
```

#### Available optimizer Options

- 0 - MONTE_CARLO_EXHAUSTIVE
- 1 - MONTE_CARLO_ADAPTIVE
- 2 - MONTE_CARLO_ANNEALING
- 3 - EXPECTED_VALUE

#### Available dataSource Options

- FILE_SYSTEM - Gets data from files in the `./stats` directory. See the `stats` directory in this repository for example files. Application will attempt to read data of all files in the `./stats` directory. You can also specify a specific file using the `-f` flag.

- GCP_BUCKETS - Gets data from GCP cloud storage buckets. This option is intended to be used only by the softball-scorer app (https://github.com/thbrown/softball-scorer).

#### Available lineupType Options

Available lineup types:

- 0 - STANDARD
- 1 - ALTERNATING_GENDER
- 2 - NO_CONSECUTIVE_FEMALES

## Contributing

See CONTRIBUTING.md

## Other Notes

- \*All code checked into this repository must build against Java 11 for cloud computing compatibility reasons, but if you have a newer version of Java and just want to get this tool working, you can bypass the Java 11 requirement by commenting out the `sourceCompatibility` and `targetCompatibility` lines in the `java` section of the `build.gradle` file before running the build command.

- This CLI uses ANSI escape sequences to color error messages for readability. These are not enabled by default in common Windows shells. So, on Windows you may see some text artifacts before and after error messages.
