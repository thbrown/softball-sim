# Softball Lineup Simulator Contributing

Hello! Thanks for your interest in contributing to the Softball Lineup Simulator.

I'm happy to approve pull requests that add new lineup optimizer algorithms or that add new lineup types. See the sections below for instructions on how do each of these. If you'd like to change something major, lets talk since this may affect the softball.app web application that uses this repo and I'll need to make sure other changes are compatible.

A note on code style. This repo generally follows the Google Style Guide (https://google.github.io/styleguide/javaguide.html) with some changes to line length and enum formatting. Once you've made your changes, you can import the code-format.xml into Eclipse and do your formatting there or just run `./gradlew spotlessApply` to auto format all Java code in the project.

## Add a Lineup Optimizer Algorithm

The default Monte Carlo optimizer is fine, but I have a better algorithm for lineup optimization that [uses cutting edge machine learning, is endorced by Tony La Russa, accounts for relativistic effects, etc...] Great! Add your own optimizer by following these steps:

1. Create a JSON definition file for your optimizer and put in the `<project_root>/json` directory. TODO: Doc the json schema
1. Create a new java package for your Optimizer named com.github.thbrown.softballsim.optimizer.impl.<your_optimizer_name>. All the Java for your implementation should go in this package or in packages below this one (i.e. ...imple.<your_optimizer_name>.subpackage is fine).
1. Create a class that implements the Optimizer interface. Optimizer is a generic type. If your Optimizer implementation produces data other than an optimized lineup and an estimated score, you'll need to subclass Result. If not you can just use the existing Result class. See the java doc on the relevant classes for additional help. 
1. Make an enum value for your optimizer in com.github.thbrown.softballsim.optimizer.OptimizerEnum. The enum constructor accepts an id (set this to the next available integer) and an instance of your Optimizer implementation.
1. Run all the unit tests `./gradlew clean test --info` and fix any failures.
1. Re-generate the index.html for the gallery `./gradlew generateGalleryHtml`. 
1. Make sure your entry in the gallery looks the way you like it. Open index.html in your browser. Alternatively, Run the built-in Gradle web-server `./gradlew serve` and checkout `http://localhost:8085/index.html` in your browser.
1. Format your code `./gradlew spotlessApply` (`./gradlew build` will also format code)
1. Submit the pull request!

## Add a LineupType

Different leagues and tournaments have different rules for how lineups can be constructed. If the simulator doesn't support the format your team uses, consider adding a new lineupType by following these steps:

To add a new lineup type:
1. Create a class that implements LineupIndexer in the package `com.github.thbrown.softballsim.lineupindexer`.
	- LineupIndexer retrieves the set of all valid BattingLineups by index. Implement your new lineup indexer such that it only returns BattingLineups that are valid according to your lineup rules.
	- In the strage case that your lineup can't be represented by a list of players for functional purposes, you'll need to add a new BattingLineup implementation as well. If you do so, you'll still need to provide a way to represent your lineup as a list for display purposes. The BattingLineup interface enforces this.
1. If you added a new LineupIndexer, register your new indexer in the static map in the LineupType class.
1. Run all the unit tests `./gradlew clean test --info` and fix any failures.
1. Format your code `./gradlew spotlessApply` (`./gradlew build` will also format code)
1. Submit the pull request!