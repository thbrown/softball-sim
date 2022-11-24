package com.github.thbrown.softballsim.datasource;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public interface DataSource {

  final int MAX_RETRY_COUNT = 9;

  /**
   * Command line options specific to this optimizer
   */
  public List<Option> getCommandLineOptions();

  /**
   * Retrieves stats data from the data source location. That should be parsed and returned as a
   * DataStats object.
   * 
   * @param cmd
   */
  public DataStats getData(CommandLine cmd);

  /**
   * Retrieves the cache (if available) if this optimizer has been run with the same options on the
   * same data before.
   * 
   * @param stats
   */
  public Result getCachedResult(CommandLine cmd, DataStats stats);

  /**
   * Called on a set interval while an optimizer is running.
   */
  public default void onUpdate(CommandLine cmd, DataStats stats, ProgressTracker tracker) {
    Result currentResult = tracker.getCurrentResult();
    if (currentResult != null) {
      double progressPercentage = ((double) currentResult.getCountCompleted())
          / ((double) currentResult.getCountTotal()) * 100;
      String progress = StringUtils.formatDecimal(progressPercentage, 2);
      Logger.log(progress + "% complete -- Estimated Seconds Remaining: " + tracker.getEstimatedSecondsRemaining()
          + " (Estimated time total: " + tracker.getEstimatedSecondsTotal() + ")");
    }

    // Call the update urls if specified
    if (cmd.hasOption(CommandLineOptions.UPDATE_URL)) {
      boolean success = signalUpdate(cmd.getOptionValue(CommandLineOptions.UPDATE_URL),
          cmd.getOptionValue(CommandLineOptions.ID),
          cmd.getOptionValue(CommandLineOptions.UPDATE_API_KEY),
          cmd.getOptionValue(CommandLineOptions.UPDATE_STUFF));
      if (!success) {
        Logger.warn("UPDATE: Call to update url failed");
      }
    }
  }

  /**
   * Called once after an optimizer has completed (whether is ends successfully or in error).
   */
  public default void onComplete(CommandLine cmd, DataStats stats, Result finalResult) {
    // Call the update url, if specified
    if (cmd.hasOption(CommandLineOptions.UPDATE_URL)) {
      int retryCount = 0;
      boolean success = false;
      while (true) {
        success = signalUpdate(
            cmd.getOptionValue(CommandLineOptions.UPDATE_URL),
            cmd.getOptionValue(CommandLineOptions.ID),
            cmd.getOptionValue(CommandLineOptions.UPDATE_API_KEY),
            cmd.getOptionValue(CommandLineOptions.UPDATE_STUFF));

        if (success) {
          break;
        } else {
          // Wait and re-try
          // retryCount - wait time (ms)
          // 1 - 7000 to 10000
          // 2 - 9000 to 12000
          // 3 - 13000 to 16000
          // 4 - 21000 to 24000
          // 5 - 37000 to 40000
          // 6 - 69000 to 72000
          // 7 - 133000 to 136000
          // 8 - 261000 to 264000
          // 9 - 517000 to 520000
          // App servers can be down for ~17 min without affecting optimization updates
          try {
            int sleepTime = 5000 + (int) (1000 * Math.pow(2, retryCount)) + (int) (Math.random() * 3000);
            Logger.log("Will retry update call in " + sleepTime + "ms attempt #" + retryCount);
            Thread.sleep(sleepTime);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          retryCount++;
        }
      }
    }
  }

  /**
   * Retrieves control false from data source, currently only used to halt.
   */
  public String getControlFlag(CommandLine cmd, DataStats stats);

  private boolean signalUpdate(String inputUrl, String optimizationId, String apiKey, String stuff) {
    try {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("optimizationId", optimizationId);
      jsonObject.addProperty("apiKey", apiKey);
      jsonObject.addProperty("stuff", stuff);
      Gson gson = GsonAccessor.getInstance().getCustom();
      String jsonPayload = gson.toJson(jsonObject);
      Logger.log("Sending " + jsonPayload + " to " + inputUrl);
      byte[] out = jsonPayload.getBytes(StandardCharsets.UTF_8);
      int length = out.length;

      URL url = new URL(inputUrl);
      URLConnection con = url.openConnection();
      HttpURLConnection http = (HttpURLConnection) con;
      http.setRequestMethod("POST"); // PUT is another valid option
      http.setDoOutput(true);
      http.setFixedLengthStreamingMode(length);
      http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
      http.connect();
      OutputStream os = http.getOutputStream();
      os.write(out);
      int code = http.getResponseCode();

      // TODO: don't retry 400 or 403
      if (code != 204 && code != 200) {
        Logger.error("Bad response from update URL");
        Logger.error(String.valueOf(code));
        Logger.error(http.getResponseMessage());
        return false;
      } else {
        Logger.log("Update call succeeded");
      }
      http.disconnect();
    } catch (Exception e) {
      Logger.error("Failed to call update URL");
      Logger.error(e);
      return false;
    }
    return true;
  }

}
