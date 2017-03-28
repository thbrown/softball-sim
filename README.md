# SoftballSim
Command line Monte Carlo simulation tool for making sure you choose the best lineup for your 
softball or baseball team.

Usage:
`ant`
`java -jar SoftballSim <lineupType (string) OR lineupType ordinal (int)>`
`java -jar SoftballSim ordinary`

Assumes input file(s) in `./stats`. See the `stats` directory in this repository for example files.

Available lineup generators:
*  0 - OrdinaryBatteryLineupGenerator
   *  Expects file format like: `Hermione Granger,A,3,4,2,4`
*  1 - AlternatingBattingLineupGenerator
   *  Expects file format like: `Bashful,4,0,0,0` 
		
## Notes

Input data is an integer 0-4 (inclusive) that represents the number of bases the player reached in each of their at bats:
*  0 => out/error/fielder's choice
*  1 => single/walk
*  2 => double
*  3 => triple
*  4 => homerun
		
## Extending the simulation
To add your own lineup generator:
1. Implement BattingLienup and LineupGenerator.
1. Register your new generator in the static map in the main SoftballSim class.
		
