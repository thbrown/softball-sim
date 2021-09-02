package com.github.thbrown.softballsim.datasource.gcpfunctions;

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
import com.google.gson.Gson;

public class DataSourceGcpBuckets implements DataSource {

  public final static String ID = "i";

  public final static String STATS_DATA_BUCKET = "optimization-stats";
  public final static String CACHED_RESULTS_BUCKET = "optimization-results";
  public final static String ADDITIONAL_OPTIONS_BUCKET = "optimization-options";
  public final static String CONTROL_FLAGS_BUCKET = "compute-control-flags";

  private final Gson gson = GsonAccessor.getInstance().getCustom();

  @Override
  public List<Option> getCommandLineOptions() {
    List<Option> options = new ArrayList<>();
    options.add(Option.builder(ID)
        .longOpt("Id")
        .desc(DataSourceEnum.FILE_SYSTEM
            + ": Required. An arbitrary id associted with this request. The same id can be used to query for intermediate results.")
        .hasArg(true)
        .required(false)
        .build());
    return options;
  }

  @Override
  public DataStats getData(CommandLine cmd) {
    String result = CloudUtils.readBlob(cmd.getOptionValue(ID), STATS_DATA_BUCKET);
    return gson.fromJson(result, DataStats.class);
  }

  @Override
  public String[] getAdditionalOptions(CommandLine cmd) {
    List<String> data = new ArrayList<>(1);
    data.add(CloudUtils.readBlob(cmd.getOptionValue(ID), ADDITIONAL_OPTIONS_BUCKET));
    return data.toArray(new String[0]);
  }

  @Override
  public Result getCachedResult(CommandLine cmd, DataStats stats) {
    String result = CloudUtils.readBlob(cmd.getOptionValue(ID), CACHED_RESULTS_BUCKET);
    return gson.fromJson(result, Result.class);
  }

  @Override
  public void onUpdate(CommandLine cmd, DataStats stats, ProgressTracker tracker) {
    Result latestResult = tracker.getCurrentResult();
    if (!cmd.hasOption(CommandLineOptions.ESTIMATE_ONLY)) {
      CloudUtils.upsertBlob(gson.toJson(latestResult), cmd.getOptionValue(ID), CACHED_RESULTS_BUCKET);
    }
  }

  @Override
  public void onComplete(CommandLine cmd, DataStats stats, Result finalResult) {
    CloudUtils.upsertBlob(gson.toJson(finalResult), cmd.getOptionValue(ID), CACHED_RESULTS_BUCKET);
    CloudUtils.deleteBlob(cmd.getOptionValue(ID), STATS_DATA_BUCKET);
    CloudUtils.deleteBlob(cmd.getOptionValue(ID), ADDITIONAL_OPTIONS_BUCKET);
    CloudUtils.deleteBlob(cmd.getOptionValue(ID), CONTROL_FLAGS_BUCKET);
  }

  @Override
  public String getControlFlag(CommandLine cmd, DataStats stats) {
    return CloudUtils.readBlob(DataSourceGcpBuckets.CONTROL_FLAGS_BUCKET, cmd.getOptionValue(DataSourceGcpBuckets.ID));
  }
}
