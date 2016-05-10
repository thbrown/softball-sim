package com.github.thbrown.softballsim;
import java.util.Random;

public class Player {
	
	private static final long SEED = System.currentTimeMillis();
	private static Random rand = new Random(SEED); 

	String name;
	double average;
	
	// Err... TODO, currently unused
	int atBats;
	int hits;
	int doubles;
	int triples;
	int homeRuns;
	
	public Player(String name, double average) {
		this.name = name;
		this.average = average;
	}
	
	public int hit() {
		double randomValue = rand.nextInt(1000);
		if (randomValue/1000.0 < average) {
			// Players have an equal change of getting a single, double, triple, or homerun
			return rand.nextInt(4) + 1;
		} else {
			return 0;
		}
	}
	
}
