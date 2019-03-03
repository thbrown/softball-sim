package com.github.thbrown.softballsim;

import com.github.thbrown.softballsim.lineup.BattingLineup;

public class Result {
	private double avgScore;
	private BattingLineup lineup;
	
	Result(double score, BattingLineup lineup) {
		this.avgScore = score;
		this.lineup = lineup;
	}
	
	double getScore() {
		return this.avgScore;
	}
	
	BattingLineup getLineup() {
		return this.lineup;
	}
}
