A faster but less accurate optimizer that doesn't test the entire search space of possible lineups. 

Instead, it employs [simulated annealing](https://en.wikipedia.org/wiki/Simulated_annealing), a global optimization technique inspired by heating and controlled cooling of a material to alter its physical properties, to search only a subset of possible lineups. 

If you have 11 or more batters in your lineup, this is most likely the optimizer you'll want to use.

## Overview 

Dispite this optimizer's much shorter runtime, it's results are quite good.

In tests on "STANDARD", "ALTERNATING_GENDER", "NO_CONSECUTIVE_FEMALES", "NO_CONSECUTIVE_FEMALES_AND_NO_THREE_CONSECUTIVE_MALES" lineup types, the optimizer generally obtains the theoretically optimal lineup when run for at least 30 seconds.

That's a high quality answer in a fraction of the time. It's also currently the only viable optimizer for larger lineups (11+ batters).

Because simulated annealing is a stochastic process (relies on random numbers) you may get different results with each run, particularly if your selected time durations are low.

This optimizer is also multi-threaded. It will run several instances of itself simultaneously and, after each instance has completed, it will select the highest scoring lineup.

## Related Optimizers

This optimizer uses multiple game simulations (see Monte Carlo Exhaustive) and statistical t-tests (see Monte Carlo Adaptive) to determine whether a particular lineup is better or worse than another.
