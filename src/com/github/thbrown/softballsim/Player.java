package com.github.thbrown.softballsim;
import java.util.Random;
import java.util.TreeMap;

public class Player {
	
	private static final long SEED = System.currentTimeMillis();
	private static Random rand = new Random(SEED); 

	String name;
	
	private int atBats;
	private int hits;
	private int doubles;
	private int triples;
	private int homeRuns;
	private int walks;
	
	TreeMap<Integer, Integer> plateApperanceDistribution = new TreeMap<>();
	
	public Player(String name, int atBats, int hits, int doubles, int triples, int homeRuns, int walks) {
		this.name = name;
		this.atBats = atBats;
		this.hits = hits;
		this.doubles = doubles;
		this.triples = triples;
		this.homeRuns = homeRuns;
		this.walks = walks;
		
		plateApperanceDistribution.putIfAbsent(homeRuns, 4);
		plateApperanceDistribution.putIfAbsent(homeRuns + triples, 3);
		plateApperanceDistribution.putIfAbsent(homeRuns + triples + doubles, 2);
		plateApperanceDistribution.putIfAbsent(walks + hits, 1);
		plateApperanceDistribution.putIfAbsent(walks + atBats, 0);
	}
	
	public int hit() {
		int randomValue = rand.nextInt(walks + atBats) + 1;
		return plateApperanceDistribution.ceilingEntry(randomValue).getValue();
	}
	
	@Override
	public String toString() {
		return this.name + " " + getAvg() + " " + getSlgPer();	
	}
	
	private String getAvg() {
		Double result = (double) (hits+doubles+triples+homeRuns+walks)/atBats;
		return String.format(java.util.Locale.US,"%.3f", result);
	}
	
	private String getSlgPer() {
		Double result = (double) (hits*1+doubles*2+triples*3+homeRuns*4)/atBats;
		return String.format(java.util.Locale.US,"%.3f", result);
	}
	
}
