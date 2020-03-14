package com.github.thbrown.softballsim.datasource.local;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.datasource.DataSourceFunctions;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;
import com.google.gson.Gson;

/**
 * Local data source prints data to the console via the logger on each event.
 */
public class DataSourceFunctionsFileSystem implements DataSourceFunctions {

  private final Gson gson = GsonAccessor.getInstance().getCustom();
  private final String cacheFileName;

  public DataSourceFunctionsFileSystem(String fileName) {
    cacheFileName = fileName;
  }

  @Override
  public void onUpdate(ProgressTracker tracker) {
    Result currentResult = tracker.getCurrentResult();
    if (currentResult != null) {
      double progressPercentage =
          ((double) currentResult.getCountCompleted()) / ((double) currentResult.getCountTotal()) * 100;
      String progress = StringUtils.formatDecimal(progressPercentage, 2);
      Logger.log(progress + "% complete -- Estimated Seconds Remaining: " + tracker.getEstimatedSecondsRemaining()
          + " (Estimated time total: " + tracker.getEstimatedSecondsTotal() + ")");
    }

    // Save the most recent result to the file system so we can start the optimization from this point
    // if it gets inturrupted
    String result = gson.toJson(currentResult);
    writeFile(result, DataSourceFileSystem.CACHED_RESULTS_FILE_PATH, cacheFileName);
  }

  @Override
  public void onComplete(Result finalResult) {
    Logger.log(finalResult);
    String resultString = gson.toJson(finalResult);
    writeFile(resultString, DataSourceFileSystem.CACHED_RESULTS_FILE_PATH, cacheFileName);
  }

  @Override
  public void onEstimationReady(ProgressTracker tracker) {
    Logger.log("Estimated completion time in seconds: " + tracker.getEstimatedSecondsTotal());
  }

  private void writeFile(String value, String directory, String fileName) {
    if (fileName == null) {
      return;
    }

    File dir = new File(directory);
    if (!dir.exists())
      dir.mkdirs();
    File file = new File(directory + File.separatorChar + fileName);
    try {
      FileWriter fw = new FileWriter(file.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(value);
      bw.close();
    } catch (IOException e) {
      Logger.log("There was a problem writing partial results to the cache file");
    }
  }
}
