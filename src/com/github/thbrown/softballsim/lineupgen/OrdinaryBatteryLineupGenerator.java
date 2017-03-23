package com.github.thbrown.softballsim.lineupgen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

import com.github.thbrown.softballsim.PermutationGeneratorUtil;
import com.github.thbrown.softballsim.Player;
import com.main.thbrown.softballsim.lineup.BattingLineup;
import com.main.thbrown.softballsim.lineup.OrdinaryBattingLineup;

public class OrdinaryBatteryLineupGenerator implements LineupGenerator {

	private Queue<BattingLineup> allPossibleLineups = new LinkedList<>();

	@Override
	public BattingLineup getNextLienup() {
		return allPossibleLineups.poll();
	}

	@Override
	public void readInDataFromFile(String statsPath) {
		List<Player> players = new LinkedList<>();

		// Read in batter data from the supplied directory
		File folder = new File(statsPath);
		File[] listOfFiles = folder.listFiles(m -> m.isFile());
		if(listOfFiles == null) {
			throw new IllegalArgumentException("No files were found in " + statsPath);
		}
		for (int i = 0; i < listOfFiles.length; i++) {
			System.out.println("Processing file " + listOfFiles[i].getName());
			read(statsPath + File.separator + listOfFiles[i].getName());
		}
		collect(players);

		// Find all batting lineup permutations
		List<List<Player>> lineups = PermutationGeneratorUtil.permute(players);

		for(List<Player> lineup : lineups) {
			allPossibleLineups.add(new OrdinaryBattingLineup(lineup));
		}
	}

	Map<String, String> data = new HashMap<>();

	// FIXME: This is brittle and has a bad format
	private void read(String filePath) {
		try {
			Scanner in = null;
			try {
				in = new Scanner(new FileReader(filePath));
				in.useDelimiter(System.lineSeparator());
				while (in.hasNext()) {
					String line = in.next();
					String[] s = line.split(",");
					String key = s[0];
					
					// Validata data
					for(int i = 1; i < s.length; i++) {
						if(!s[i].equals("0") && !s[i].equals("1") && !s[i].equals("2") && !s[i].equals("3") && !s[i].equals("4")) {
							throw new IllegalArgumentException("Invalid data value: " + s[i]);
						}
					}
					
					if(data.containsKey(key)) {
						data.put(key,data.get(key) + line.replace(key+",","") + ",");
					} else {
						data.put(key,line.replace(key+",","") + ",");
					}
				} 
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				in.close();
			}
		} catch (Exception e) {
			System.out.println("WARNING: There was a problem while processing " + filePath + ". This file will be skipped. Problem: " + e.getMessage());
		}
	}

	// FIXME: This is brittle and has a bad format
	private void collect(List<Player> players) {
		for(String key : data.keySet()) {
			String name = key.split(",")[0];
			String line = data.get(key);
			String[] s = line.split(",");
			players.add(
					new Player(
							name,
							s.length,
							(int)Arrays.stream(s).filter(e -> e.equals("1")).count(),
							(int)Arrays.stream(s).filter(e -> e.equals("2")).count(),
							(int)Arrays.stream(s).filter(e -> e.equals("3")).count(),
							(int)Arrays.stream(s).filter(e -> e.equals("4")).count(),
							(int)Arrays.stream(s).filter(e -> e.equals("BB")).count()));
		}
	}

}
