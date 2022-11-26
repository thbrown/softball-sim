package com.github.thbrown.softballsim.datasource.gcpbuckets;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import com.github.thbrown.softballsim.CommandLineOptions;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.cloud.CloudUtils;
import com.github.thbrown.softballsim.data.gson.DataStats;
import com.github.thbrown.softballsim.datasource.DataSource;
import com.github.thbrown.softballsim.datasource.DataSourceEnum;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.google.gson.Gson;

public class DataSourceGcpBuckets implements DataSource {

  public final static String NAME = "n";

  public final static String STATS_DATA_BUCKET = "optimization-stats";
  public final static String CACHED_RESULTS_BUCKET = "optimization-results";
  public final static String CONTROL_FLAGS_BUCKET = "optimization-flags";

  private final Gson gson = GsonAccessor.getInstance().getCustom();

  @Override
  public List<Option> getCommandLineOptions() {
    List<Option> options = new ArrayList<>();
    options.add(Option.builder(NAME).longOpt("Name").desc(DataSourceEnum.GCP_BUCKETS
        + ": Required. An arbitrary, but unique name associated with this request. Case insensitive. The same name can be used to query for intermediate results.")
        .hasArg(true).required(false).build());
    return options;
  }

  @Override
  public DataStats getData(CommandLine cmd) {
    String result = CloudUtils.readBlob(cmd.getOptionValue(DataSourceGcpBuckets.NAME), STATS_DATA_BUCKET);
    return gson.fromJson(result, DataStats.class);
  }

  @Override
  public Result getCachedResult(CommandLine cmd, DataStats stats) {
    String result = CloudUtils.readBlob(cmd.getOptionValue(DataSourceGcpBuckets.NAME), CACHED_RESULTS_BUCKET);
    Logger.log("cached result " + result);
    if (result == null) {
      return null;
    } else {
      return GsonAccessor.getInstance().getCustomWithStatsLookup(stats).fromJson(result, Result.class);
    }
  }

  @Override
  public void onUpdate(CommandLine cmd, DataStats stats, ProgressTracker tracker) {
    Result latestResult = tracker.getCurrentResult();
    try {
      CloudUtils.upsertBlob(gson.toJson(latestResult), cmd.getOptionValue(DataSourceGcpBuckets.NAME),
          CACHED_RESULTS_BUCKET);
    } catch (Exception e) {
      // It's not so bad if we skip an update
      Logger.log(e);
    }
    DataSource.super.onUpdate(cmd, stats, tracker);
  }

  @Override
  public void onComplete(CommandLine cmd, DataStats stats, Result finalResult) {
    // In the GCP function we can return the result of the estimation directlys
    // instead of reading it from the bucket, skip the write
    if (!cmd.hasOption(CommandLineOptions.ESTIMATE_ONLY)) {
      Logger.log(finalResult);
      CloudUtils.upsertBlob(gson.toJson(finalResult), cmd.getOptionValue(DataSourceGcpBuckets.NAME),
          CACHED_RESULTS_BUCKET);

      CloudUtils.deleteBlob(cmd.getOptionValue(DataSourceGcpBuckets.NAME), STATS_DATA_BUCKET);
      CloudUtils.deleteBlob(cmd.getOptionValue(DataSourceGcpBuckets.NAME), CONTROL_FLAGS_BUCKET);
    }
    DataSource.super.onComplete(cmd, stats, finalResult);
  }

  @Override
  public String getControlFlag(CommandLine cmd, DataStats stats) {
    return CloudUtils.readBlob(cmd.getOptionValue(DataSourceGcpBuckets.NAME),
        DataSourceGcpBuckets.CONTROL_FLAGS_BUCKET);
  }

}
