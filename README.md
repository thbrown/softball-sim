# SoftballSim
Lineup optimization tool for making sure you choose the best batting order for your softball, baseball, or kickball team. Used by softball.app for lineup optimizations but can also be run from the command line on file system data.

Build (requires Java 11*, in the instructions below replace `./gradlew` with `./gradlew.bat` for windows machines):

```
# Build (Note: this runs tests and formats code)
./gradlew build

# Or build without running tests
./gradlew build -x test
```

Run 

```
# Example
java -jar ./build/libs/softball-sim.jar -o MONTE_CARLO_EXHAUSTIVE --games 10000 --innings 7

# See help for available options
java -jar ./build/libs/softball-sim.jar --help

# Or use gradle, be sure to provide arguments to the run task in the build.gradle file
./gradlew run
```

Test

```
./gradlew clean test --info
```

### Flags

Flags (more command line flags will be available based on which optimizer and data source are supplied):

```
 -D,--Data-source <arg>         Where to read the source data from. Options are [FILE_SYSTEM,
                                NETWORK]. Default: FILE_SYSTEM
 -E,--Estimate-only             In Development. If this flag is provided, application will return an
                                estimated completion time only, not the result.
 -F,--File-path <arg>           FILE_SYSTEM: Path to the stats files. This can be a directory or
                                file. Default: ./stats/exampleData.json
 -H,--Help                      Prints the available flags. Help output will change depending on the
                                optimizer and dataSource specified.
 -O,--Optimizer <arg>           Required. The optimizer to be used to optimize the lineup. You may
                                specify the name or the id. Options are [MONTE_CARLO_EXHAUSTIVE -
                                0].
 -P,--Players-in-lineup <arg>   Comma separated list of player ids that should be included in the
                                optimized lineup. Defaults to all players.
 -T,--Lineup-type <arg>         Type of lineup to be simulated. You may specify the name or the id.
                                Options are [ORDINARY - 1, ALTERNATING_GENDER - 2,
                                NO_CONSECUTIVE_FEMALES - 3]. Default: ORDINARY
 -V,--Verbose                   In development. If present, print debugging details on error.
```

#### Available optimizer Options

* 0 - MONTE\_CARLO\_EXHAUSTIVE
* 1 - MONTE\_CARLO\_ADAPTIVE
* 2 - MONTE\_CARLO\_ANNEALING
* 3 - EXPECTED\_VALUE

#### Available dataSource Options

* FILE_SYSTEM - Gets data from files in the `./stats` directory. See the `stats` directory in this repository for example files. Application will attempt to read data of all files in the `./stats` directory. You can also specify a spicific file using the `-F` flag.

* NETWORK - Gets data from a local network connection. This option is intended to be used mostly by softball-scorer app (https://github.com/thbrown/softball-scorer).

#### Available lineupType Options

Available lineup types:
*  1 - ORDINARY
*  2 - ALTERNATING_GENDER
*  3 - NO\_CONSECUTIVE\_FEMALES

### GCP Functions Deployment

Shorter optimizations (< 5 minutes) can be executed on GCP cloud functions. There are two endpoints for this:

1. https://us-central1-optimum-library-250223.cloudfunctions.net/softball-sim-start
2. https://us-central1-optimum-library-250223.cloudfunctions.net/softball-sim-query

The first is a synchronous query that will start an optimization, the second is also a synchronous query that can be run in parallel to retrieve the 1st query's progress.

These calls are associated by an id parameter that is passed to both. 'zsjdklasaskfjaskfdjs' is id used in the example. If you are calling these endpoints, you should change this id to something random, long, and unique because if you don't, the runs might conflict. Furthermore, you might see other peoples data and other people might see your data.

#### To Deploy

Make sure 'WRITE_LOG_TO_FILE = false' in com.github.thbrown.softballsim.util.Logger before building.

Then, from the project root directory, run:

`gcloud functions deploy softball-sim-start --entry-point=com.github.thbrown.softballsim.cloud.GcpFunctionsEntryPointStart --timeout=540 --memory=256 --runtime=java11 --trigger-http --source=build/libs --allow-unauthenticated`

`gcloud functions deploy softball-sim-query --entry-point=com.github.thbrown.softballsim.cloud.GcpFunctionsEntryPointQuery --timeout=20 --memory=256 --runtime=java11 --trigger-http --source=build/libs --allow-unauthenticated`

#### To Test

Make sure you have params you like in ./stats/exampleGcpFunctionsParams.json. Then:

`curl -X POST "https://us-central1-optimum-library-250223.cloudfunctions.net/softball-sim-start" -H "Content-Type:application/json" --data @./stats/exampleGcpFunctionsParams.json`

To see incremental progress, use:

`curl -X POST "https://us-central1-optimum-library-250223.cloudfunctions.net/softball-sim-query" -N -H "Content-Type:application/json" --data {"I":zsjdklasaskfjaskfdjs}`

## Contributing

See CONTRIBUTING.md

## Other Notes

* *All code checked into this repository must build against Java 11 for cloud computing compatibility reasons, but if you have a newer version of Java and just want to get this tool working, you can bypass the Java 11 requirement by commenting out the `sourceCompatibility` and `targetCompatibility` lines in the `java` section of the `build.gradle` file before running the build command.

* This CLI uses ANSI escape sequences to color error messages for readability. These are not enabled by default in common Windows shells. So, on Windows you may see some text artifacts before and after error messages.