package com.github.thbrown.softballsim;

import java.util.ArrayList;
import java.util.List;

public class Main {
	// Config
	public static final boolean VERBOSE = true;
	public static final int INNINGS_PER_GAME = 5;
	public static final int NAME_PADDING = 24; // Just for formatting verbose output

	public static void main(String[] args) {
		List<Player> ladies = new ArrayList<>();
		List<Player> gentleman = new ArrayList<>();
		
		ladies.add(new Player("Hermione Granger", .945));
		ladies.add(new Player("Pam Beesly", .765));
		ladies.add(new Player("D.W. Reid", .543));
		ladies.add(new Player("Veronica Mars", .321));
		ladies.add(new Player("Fa Mulan", .123));

		gentleman.add(new Player("Gus T.T. Showbiz", .965));
		gentleman.add(new Player("MC Clap Yo Hanz", .767));
		gentleman.add(new Player("Ghee Buttersnaps", .543));
		gentleman.add(new Player("Control Alt Delete", .432));
		gentleman.add(new Player("Gus \"Sillypants\" Jackson", .134));
		gentleman.add(new Player("Ovaltine Jenkins", .004)); //Cm'on Ovaltine
		
		BattingLineup lineup = new AlternatingBattingLineup(ladies, gentleman);
		
		Simulation m = new Simulation(lineup);
		int gamesToSimulate = 10;
		double result = m.run(gamesToSimulate);
		
		System.out.println("*********************************************************************");
		System.out.println("Simulation completed (" + "n=" + gamesToSimulate + ")");
		System.out.println("Average runs scored with this lineup: " + result);
		System.out.println("*********************************************************************");
	}
}
