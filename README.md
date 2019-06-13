# SoftballSim
Monte Carlo simulation tool for making sure you choose the best lineup for your softball, baseball, or kickball team. Used by softball.app for lineup optimizations but can also be run from the command line on file system data.

Build and Run (requires java and gradle):
```
gradle build
gradle jar
java -jar ./build/libs/softball-sim.jar FILE_SYSTEM 0
```

### Arguments

Arguments to be supplied to the jar take the form:
```
java -jar ~/build/lib softball-sim.jar <dataSource(string)> <lineupType(string) OR lineupType ordinal(int)>
```

#### dataSource options

* FILE_SYSTEM - Gets data from files in the `./stats` directory. See the `stats` directory in this repository for example files. Application will atteampt to read data of all files in the `./stats` directory.

* NETWORK - Get's data from a network connection. This option is intended to be used with by mostly by softball-scorer app (https://github.com/thbrown/softball-scorer).

#### lineupType options

Available lineup generators:
*  1 - OrdinaryBatteryLineupGenerator
   *  Expects data formated like: `Bashful,4,0,0,0`
   *  Which is interpreted as *(name, hits... (homerun, out, out, out)*
*  2 - AlternatingBattingLineupGenerator
   *  Expects data formated like: `Hermione Granger,B,1,0,2,1` 
   *  Which is interpreted as *(name, group \[e.g. A=male, B=female\], hits... (single, out, double, single)*
*  3 - NoConsecutiveFemalesLineupGenerator
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
