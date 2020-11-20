package com.github.thbrown.softballsim.datasource;

import java.util.List;
import org.apache.commons.cli.Option;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.lineupindexer.LineupTypeEnum;
import com.github.thbrown.softballsim.optimizer.OptimizerEnum;

public interface DataSource {

  public List<Option> getCommandLineOptions();

  public Result execute(String[] args, LineupTypeEnum lineupType, List<String> players, OptimizerEnum optimizer);

}
