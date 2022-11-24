# SoftballSim

Lineup optimization tool for making sure you choose the best batting order for your softball, baseball, or kickball team. Used by softball.app for lineup optimizations but can also be run from the command line on file system data.

### Example Output

```
$ java -jar ./build/libs/softball-sim.jar -o MONTE_CARLO_EXHAUSTIVE --Games 100000 --Innings 7 -l "Devon,Aaron,Fred,Arnold,Simon,Artimis,Grant,Julia"
*********************************************************************
Optimizer: Monte Carlo Exhaustive
*********************************************************************
T: default
G: 100000
I: 7
L: false
*********************************************************************
Possible lineups:               40,320
Games to simulate per lineup:   100000
Innings per game:               7
Threads used:                   8
Select lowest scoring lineup?:  false
*********************************************************************
{BB=1, 1B=1, E=1, Out=1}
{2B=2, 1B=4, E=4, FC=1, Out=3}
{2B=3, BB=1, 1B=63, SAC=2, E=14, K=1, FC=3, Out=36, 3B=1}
{2B=1, 1B=13, E=3, FC=4, 3B=1, Out=7}
{1B=6, FC=1, Out=5}
{2B=10, 1B=50, HRo=20, SAC=3, E=11, FC=1, HRi=2, Out=20, 3B=3}
{2B=15, BB=1, 1B=34, SAC=4, HRo=10, E=5, K=1, FC=1, HRi=1, Out=20, 3B=2}
{1B=1, K=1, Out=3}
0.55% complete -- Estimated Seconds Remaining: 904 (Estimated time total: 909)
1.16% complete -- Estimated Seconds Remaining: 866 (Estimated time total: 876)
1.78% complete -- Estimated Seconds Remaining: 815 (Estimated time total: 830)
2.39% complete -- Estimated Seconds Remaining: 798 (Estimated time total: 818)
...
97.63% complete -- Estimated Seconds Remaining: 19 (Estimated time total: 819)
98.26% complete -- Estimated Seconds Remaining: 13 (Estimated time total: 819)
98.89% complete -- Estimated Seconds Remaining: 8 (Estimated time total: 819)
99.52% complete -- Estimated Seconds Remaining: 3 (Estimated time total: 819)
Optimal lineup:
   Name           Avg     Slg
   Devon        0.333   0.333
   Simon        0.500   0.500
   Fred         0.554   0.595
   Arnold       0.517   0.621
   Grant        0.697   1.281
   Artimis      0.726   1.427
   Aaron        0.429   0.571
   Julia        0.200   0.200
Optimal lineup avg score:
   10.83 runs per game
Details:
   Histogram:
    Runs | # Lineups | Histogram
    9.4  | 46   |
    9.5  | 189  | █
    9.6  | 486  | █
    9.7  | 1137 | ███
    9.8  | 2789 | ████████
    9.9  | 5415 | ████████████████
    10.0 | 6001 | ██████████████████
    10.1 | 5153 | ███████████████
    10.2 | 5052 | ███████████████
    10.3 | 4397 | █████████████
    10.4 | 4478 | █████████████
    10.5 | 3527 | ███████████
    10.6 | 1278 | ████
    10.7 | 349  | █
    10.8 | 23   |

   Worst lineup:
    Name           Avg     Slg
    Arnold       0.517   0.621
    Devon        0.333   0.333
    Fred         0.554   0.595
    Simon        0.500   0.500
    Julia        0.200   0.200
    Aaron        0.429   0.571
    Artimis      0.726   1.427
    Grant        0.697   1.281

   Worst lineup avg score:
    9.45 runs per game

Status: COMPLETE
Progress: 40320/40320 (100.00%)
Elapsed time (ms): 819498
Estimated time remaining (ms): 0
Exiting, optimization ended
```

The default dataSource for this tool is file system data in the `./stats` directory.

This CLI tool expects data to be in softball.app JSON format. You can either edit the JSON in the stats directory directly (yuck) or input the data on https://softball.app and use the "Export To File" tool on the main menu.

### Build (requires Java 11\*, for windows machines, in the instructions below replace `./gradlew` with `./gradlew.bat`):

```
# Production Build (Note: this runs tests, formats code, optimizes jar)
./gradlew build

# Development Build without running tests (skips test and jar optimizations)
./gradlew build -x test -x proguard

```

This will generate a runnable JAR named softball-sim.jar in ./build/libs

### Run

By default the stats used for the optimization can be found in `./stats/exampleData.json`. You can replace or edit this file to use different stats.

Whether you build from source or downloaded the jar, you can run it like this:

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
 -d,--data-source <arg>         Where to read the source data from (e.g. the stats file). Options are
                                [FILE_SYSTEM, GCP_BUCKETS]. Default: FILE_SYSTEM
 -e,--estimate-only             If this flag is enabled, the application will only run for UPDATE_INTERVAL
                                milliseconds. The produced result is useful for estimating optimization
                                completion time.
 -f,--force                     If this flag is provided, application will not attempt to use any previously
                                calculated results from cache to resume the optimization from its state when
                                it was interrupted or last run.
 -h,--help                      Prints the available flags. Help output will change depending on the optimizer
                                and dataSource specified.
 -l,--players-in-lineup <arg>   Comma separated list of player ids that should be included in the optimized
                                lineup. Defaults to all players.
 -o,--optimizer <arg>           Required. The optimizer to be used to optimize the lineup. You may specify the
                                name or the id. Options are [MONTE_CARLO_EXHAUSTIVE - 0, MONTE_CARLO_ADAPTIVE
                                - 1, MONTE_CARLO_ANNEALING - 2, EXPECTED_VALUE - 3, SORT_BY_AVERAGE - 4].
 -p,--stats-path <arg>          FILE_SYSTEM: Read. File (or directory with a single file). Path to the stats
                                file. Default: ./stats/exampleData.json
 -t,--lineup-type <arg>         Type of lineup to be simulated. You may specify the name or the id. Options
                                are [STANDARD - 0, ALTERNATING_GENDER - 1, NO_CONSECUTIVE_FEMALES - 2,
                                NO_CONSECUTIVE_FEMALES_AND_NO_THREE_CONSECUTIVE_MALES - 3]. Default: STANDARD
 -u,--update-interval <arg>     Time period, in milliseconds, that the application should report results.
                                Default: 5000
 -v,--verbose                   In development. If present, print debugging details on error.
 -x,--flags-path <arg>          FILE_SYSTEM: Read. Directory or File. Path that is periodically checked by the
                                application, if this file contains the text 'HALT' the application will
                                terminate. If the provided path is a directory the application will generate
                                the file name based on a hash of an optimization run's stats file and options.
 -z,--cache-path <arg>          FILE_SYSTEM: Read/Write. Directory or File. Path where results should be saved
                                as they are computed and where the application can look for cached results
                                before actually running any optimizer. This directory includes both final
                                results for completed optimizations as well as partial results for in-progress
                                or interrupted optimizations.  If the provided path is a directory the
                                application will generate the file name based on a hash of an optimization
                                run's stats file and options. Default: ./cache
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
- 3 - NO_CONSECUTIVE_FEMALES_AND_NO_THREE_CONSECUTIVE_MALES

## Send HTTP post on success

To facilitate integration of this command line utility into applications, the tool provides the options of specifying a url when running an optimization. The CLI will make an HTTP POST request to the specified url anytime the tool produces an optimization update (including the final completion update).

Additionally, "apiKey", "id", and a generic "stuff" flags exist for use in passing additional information in the body of this request.

This feature exists primarily to get this working with softball.app and may be removed from the repository in the future.

## Contributing

See CONTRIBUTING.md

## Other Notes

- \*All code checked into this repository must build against Java 11 for cloud computing compatibility reasons, but if you have a newer version of Java and just want to get this tool working, you can bypass the Java 11 requirement by commenting out the `sourceCompatibility` and `targetCompatibility` lines in the `java` section of the `build.gradle` file before running the build command.

- This CLI uses ANSI escape sequences to color error messages for readability. These are not enabled by default in common Windows shells. So, on Windows you may see some text artifacts before and after error messages.
