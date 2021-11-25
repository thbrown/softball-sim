An optimization engine that uses statistical techniques to reduce the number of game simulations required to determine optimal batting lineups. Intended to be a faster version of the Monte Carlo Exhaustive optimizer.

## Overview

The Monte Carlo Exhaustive optimizer

The exact number of games simulated for each lineup is determined by continuing to do simulations on a lineup until a statistical t-test determines that the expected run totals for two lineups are significantly different (by some configurable alpha value). The lineup with the lower mean is then rejected and the larger one remembered as the best so far.
