package com.github.thbrown.softballsim;

import java.util.Optional;

import com.github.thbrown.softballsim.lineup.BattingLineup;

public class Result {
	private double avgScore;
	private BattingLineup lineup;
	
	Result(double score, BattingLineup lineup) {
		this.avgScore = score;
		this.lineup = lineup;
	}
	
	public double getScore() {
		return this.avgScore;
	}
	
	public BattingLineup getLineup() {
		return this.lineup;
	}
	
	@Override
	public String toString() {
	  return avgScore + " " + Optional.ofNullable(lineup).map(v -> v.toString()).orElse("null");
	}
}
