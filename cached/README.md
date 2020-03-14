TODO: This feature hasn't been implemented yet

This directory contains partial results for optimizations that were interrupted.
 
Each file name is the md5 hash of the parameters that were passed to the 
optimization. If a subsequent optimization receives parameters that hash to the 
same md5 value as the saved file name, the contents of that file are used to 
seed the optimization with partial results data. This allows the optimization to
be resumed from the point which it was stopped.

Command line users can force an optimization to ignore any saved results by 
using the '-f' of '--force' flags.