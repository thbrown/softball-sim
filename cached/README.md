This directory contains files with the results of previously run optimizations 
or partial results for optimizations that were interrupted. These allow us to
server up results that have already been computed quickly.

Command line users can force an optimization to ignore any results that have
been cached in this directory using the '-f' or '--force' flags.

Each file name is the md5 hash of the parameters that were passed to the 
optimization. If a subsequent optimization receives parameters that hash to the 
same md5 value as the saved file name, the contents of that file are used to 
seed the optimization with partial results data. Interrupted optimizations can
be resumed from the point which at which they were stopped. For completed 
optimizations we can return the cached result directly. 