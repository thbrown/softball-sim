Sorts lineup in descending order by batting average.

If that's not possible (based on the lineupType), this optimizer selects the lineup with the minimum sum of the squares of each set of two consecutive batters with decreasing batting average.

i.e.
1) For every lineup...
2) For every two consecutive batters in the lineup for which the second batter has a lower batting average than the first, square then sum the difference in batting average, call this value the lineup's "score"
3) Choose the lineup with the lowest "score".