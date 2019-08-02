package com.github.thbrown.softballsim.testing.helpers;

import java.io.PrintWriter;
import java.util.Map;

public interface ServerMethods {
  
  public void onReady(PrintWriter out) throws Exception;
  
  public void onComplete(Map<String, String> data) throws Exception;
  
}
