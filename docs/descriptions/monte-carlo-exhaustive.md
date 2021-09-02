Runs Monte Carlo simulations of games for all possible lineups. The optimizer then averages the runs scored acrosss all possible lineups and returns the lineup with the highest average runs scored.

### Overview

Simulation flow. This flowchart refers to the parameter *g* which is a configurable value indicating the number of games to simulate. The stages with thick black outlines have sections w/ additional details below.
```mermaid
flowchart TD;  
  A[Select a lineup from<br> the lineup pool.]:::addtDoc-->B;
  B[Simulate a game,<br>remember the number<br> of runs scored.]:::addtDoc-->D
  D{Have we simulated<br>more than *g* games?}--yes-->E
  D--no-->B
  E[Compute the mean <br>score of all simulated<br> games using this lineup.]-->F
  F{Is this lineup's <br>mean game score better <br>than all the others we<br> have simulated?}--yes-->G
  F--no-->I
  G[Remember this lineup<br> as bestLineup and its<br> mean score as bestScore.]-->I
  I{Is the lineup pool<br> depleted?}--yes-->J
  I--no-->A
  J[Done! Print the bestLineup <br>and bestScore that we<br> remembered earlier.]
  classDef addtDoc stroke:#333,stroke-width:4px;
```

### The Lineup Pool

This optimizer simulates games for all possible lineups. The number of possible lineups for each lineup type is given by these equations where 'm' is the number of male batters and 'f' is the number of female batters:

#### Standard
```math
numberOfLineups = (m+f)!
```

#### Alternating Gender
```math
numberOfLineups = (m! + f!) * 2
```

#### No Consecutive Females
```math
numberOfLineups = m! * f! * (\\binom{m}{f} + \\binom{m-1}{f-1})
```

#### Performance

Because of the factorial nature of these equations, adding even one more player to the lineup can make the optimizer take significantly longer to run.

Example simulation (7 innings, 10000 games)

| # players in lineup  | possible lineups     | runtime (ms) | ~runtime (human)  |
| -------------------- | -------------------- | ------------ | ----------------- |
| 6                    | 720                  | 1958         | 2 seconds         |
| 7                    | 5,040                | 15982        | 16 seconds        |
| 8                    | 40,320               | 135255       | 2 minutes         |
| 9                    | 362,880              | 832902       | 14 minutes        |
| 10                   | 3,628,800            | 6613484      | 2 hours           |

### Simulating a game
This flowchart refers to the parameter *i* which is a configurable value indicating the number of innings to simulate (typically 9 for baseball, 7 for softball, but can be anything). Same as before, the stage with the thick black outline has a section w/ additional details below.
Simulate a game
```mermaid
flowchart TD;
  B[Set active batter to<br>the first player <br>in the lineup.]-->C
  C{Simulate a <br> plate appearance <br> for the active batter.}:::addtDoc--Result: 0-->D
  C--Result: 1,2,3,4-->E
  D[Increment the<br> number of outs <br>by one]-->F
  E["Advance the runners. <br> Increase score if the<br>plate appearance <br>drove home (a) run(s)."]-->G
  F[>= 3 outs?]--no--> G
  F--yes-->I
  G[Set active batter to<br>the next batter <br>in the lineup.]
  G-->C
  I{Have we <br>simulated more <br>than *i* innings?}--yes-->J
  I--no--> K
  J[Done! Return the score<br> for this simulated game.]
  K[Increment the <br>number of innings<br> simulated by one. <br> Clear the bases. <br> Clear the outs.]-->G
  classDef addtDoc stroke:#333,stroke-width:4px;
```

### Simulate a Plate Appearance

Each plate appearance result (Out, SAC, E, BB, 1B, 2B, 3B, HRi, HRo) is mapped to a number indicating the number of bases awarded for that plate appearance. The mapping is illustrated in this table:

| Result          | Bases |
| --------------- | ----- |
| Out, SAC*, E, K | 0     |
| 1B, BB*         | 1     |
| 2B              | 2     |
| 3B              | 3     |
| HRi, HRo        | 4     |

We can then use the frequency of each type of hit to build a distribution that reflects the way any given player is likely perform when they get a plate appearance. Whenever we need to simulate a hit for that player, we draw a random sample from that player's distribution.

#### An Example

Tim's historical at bats are as follows:
Out,1B,2B,SAC,E,HRo,3B,1B,1B,Out,Out,2B,1B,Out,Out

First we translate those hits to number of bases using our mapping from the table above:
0,1,2,0,0,4,3,1,1,0,0,2,1,0,0

Then we determine the histogram and chance of each hit:

| # of bases | # of times | % of plate appearances |
| ---------- | ---------- | --------  |
| 0          | 7          | 47        |
| 1          | 4          | 27        |
| 2          | 2          | 13        |
| 3          | 1          | 7         |
| 4          | 1          | 7         |

And every time we simulate a plate appearance for Tim, we'll draw a random hit with that distribution. That is to say, for every simulated plate appearance, Tim has a 47% of getting out, 27% chance of getting a single, a 13% chance of getting a double, a 7% chance of getting a triple, and a 7% chance of getting a home run. Of course, other players will have their own distribution of hits to draw from based of their historical performance. 

### Other Notes

Things that are not accounted for in the simulation:

- Double/triple plays
- Stolen bases
- Players who were on base advancing more bases than the hitter
- Any pitching data

_*We can debate about how walks or sacrifices should be counted. It probably depends on what flavor of the sport you are playing. IMHO sacrifices should be counted as outs in slowpitch softball and kickball, but not baseball or fastpitch. In any event, these mapping are configurable (or will be configurable soon). So you are welcome to impose your own philosophy._