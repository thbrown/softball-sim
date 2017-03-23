# SoftballSim
Command line Monte Carlo simulation tool for making sure you choose the best lineup for your 
softball or baseball team.

usage: java SoftballSim lineupGeneratorNumber
	Expecting input files in D:\Eclipse_thbrown\Softball\stats
	Available lineup generators:
		0) OrdinaryBatteryLineupGenerator
		1) AlternatingBattingLineupGenerator
		
Notes:

Input data is (for all generators so far) a number 0-4 inclusive that represents the number of
bases the player reached in each of their at bats (e.g. 0=out/error/fc, 1=single/walk, 2=double...).
		
To add your own lineup generator, implement BattingLienup and LineupGenerator; register your new
generator in the static map in the main SoftballSim class.
		