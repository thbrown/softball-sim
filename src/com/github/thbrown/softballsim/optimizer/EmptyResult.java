package com.github.thbrown.softballsim.optimizer;

import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;

/**
 * Used as a placeholder to mark status before any results have been computed
 */
public class EmptyResult extends Result {

  public EmptyResult(OptimizerEnum optimizer, ResultStatusEnum status) {
    super(optimizer, null, 0, 0, 0, 0, status);
  }

}
