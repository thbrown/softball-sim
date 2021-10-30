package com.github.thbrown.softballsim.cloud;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Optional;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.ResultStatusEnum;
import com.github.thbrown.softballsim.datasource.gcpfunctions.DataSourceGcpBuckets;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.*;
import com.google.common.collect.ImmutableList;

/**
 * A GCP function for starting up a preemptible compute instance. Unlike the other functions in this
 * project, this isn't supposed to be called over the public internet. Rather it is called by the
 * start function if the function runtime expires, or by a compute instance shut down script in the
 * case of preemption.
 */
public class GcpFunctionsEntryPointCompute implements HttpFunction {

  final String MACHINE_TYPE = "e2-highcpu-4";
  final String SNAPSHOT_NAME = "optimization-base-2";

  public static final String ARGS_KEY = "args";
  public static final String ID_KEY = "id";
  public static final String ZONES_KEY = "zones";

  @Override
  public void service(HttpRequest request, HttpResponse response) throws Exception {
    try {
      // Setup required variables
      Gson gson = GsonAccessor.getInstance().getCustom();
      final String HOME_DIRECTORY = Optional.ofNullable(System.getenv("HOME_DIRECTORY"))
          .orElseThrow(() -> new RuntimeException("HOME_DIRECTORY is not set in the environment"));
      final String PROJECT = Optional.ofNullable(System.getenv("PROJECT"))
          .orElseThrow(() -> new RuntimeException("PROJECT is not set in the environment"));
      final String PASSWORD_HASH = Optional.ofNullable(System.getenv("PASSWORD_HASH"))
          .orElseThrow(() -> new RuntimeException("PASSWORD_HASH is not set in the environment"));

      // Extract post body into a map for consumption
      byte[] jsonBodyBytes = Optional.ofNullable(request.getInputStream().readAllBytes())
          .orElseThrow(() -> new RuntimeException("Missing POST request body"));
      String jsonBody = new String(jsonBodyBytes, StandardCharsets.UTF_8);
      MapWrapper map = gson.fromJson(jsonBody, MapWrapper.class);

      // Password checking
      String pwd = Optional.ofNullable(map.get(GcpFunctionsEntryPointStart.PASSWORD_KEY)).orElseThrow(() -> {
        try {
          Thread.sleep(3000); // Delay to prevent excessive guessing
        } catch (InterruptedException e) {
        }
        return new RuntimeException("Missing Password");
      });

      String pwdHash = StringUtils.calculateSha256AsHex(pwd.trim());
      if (!pwdHash.equals(PASSWORD_HASH)) {
        throw new RuntimeException("Invalid Password");
      }
      map.remove(GcpFunctionsEntryPointStart.PASSWORD_KEY);

      Logger.log("Body: " + map.toString());

      String args = map.get(ARGS_KEY).replace("\\\"", "\"");
      String id = map.get(ID_KEY);
      String[] zones = map.get(ZONES_KEY).split(",");
      zones = Arrays.stream(zones).filter(x -> !StringUtils.isBlank(x)).toArray(String[]::new); // Filter empty strings

      Logger.log("Args: " + args);
      Logger.log("Id: " + id);
      Logger.log("Zones: " + Arrays.toString(zones) + " " + zones.length);

      // Don't start a new compute instance if the result in a terminal state (e.g.
      // COMPLETE, PAUSED, etc...)
      String resultJson = CloudUtils.readBlob(id, DataSourceGcpBuckets.CACHED_RESULTS_BUCKET);
      Result result = gson.fromJson(resultJson, Result.class);
      if (result.getStatus().isTerminal()) {
        Logger.log("Not starting compute instance, ");
        response.setContentType("application/json");
        response.setStatusCode(200);
        String payload = CloudUtils.getResponseJson("SUCCESS",
            "Success - no new compute instance needed, result is in state " + result.getStatus());
        response.getOutputStream().write(payload.getBytes());
        return;
      }

      // We've tried all the compute zones? mark the current result as ERROR
      if (zones.length == 0) {
        Logger.log("ERROR - Zones exhausted");
        String resultJsonOriginal = CloudUtils.readBlob(id, DataSourceGcpBuckets.CACHED_RESULTS_BUCKET);
        Result resultOriginal = gson.fromJson(resultJsonOriginal, Result.class);
        Result updatedResult = resultOriginal.copyWithNewStatus(ResultStatusEnum.ERROR,
            "Cloud resources unavailable, try again later");
        CloudUtils.upsertBlob(gson.toJson(updatedResult), id, DataSourceGcpBuckets.CACHED_RESULTS_BUCKET);
        throw new RuntimeException("Zones Exhausted");
      }

      String nextZone = zones[0];
      String[] futureZones = Arrays.copyOfRange(zones, 1, zones.length);

      // TODO: retry on failure?
      Operation operation = makeInstance(id, nextZone, PROJECT, MACHINE_TYPE, id + "-" + futureZones.length,
          HOME_DIRECTORY, SNAPSHOT_NAME, args, futureZones, pwd);
      Logger.log(id + " operation: " + operation.getSelfLink());

      // Send the response
      response.setContentType("application/json");
      response.setStatusCode(200);
      String payload = CloudUtils.getResponseJson("SUCCESS", "Success - sent compute request start command.");
      response.getOutputStream().write(payload.getBytes());
    } catch (Exception e) {
      Logger.log(e);
      CloudUtils.send400Error(response, e.toString());
    }
  }

  private Operation makeInstance(String optimizationId, String zone, String project, String machineType,
      String instanceName, String homeDirectory, String snapshotName, String applicationArguments, String[] futureZones,
      String pwd) throws IOException, GeneralSecurityException {

    Logger.log("Starting instance: " + instanceName);

    // @formatter:off 
      String startupScript = new StringBuilder()
      .append("#!/bin/sh\n")
      .append("echo running startup script\n")
      .append("export APP_WRITE_LOG_TO_FILE=true\n")
      .append("HOME_DIRECTORY=\"" + homeDirectory + "\"\n")
      .append("for i in 1 2 3 4 5; do\n")
      .append("  OPTIMIZATION_ID=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/optimization-id -H \"Metadata-Flavor: Google\" --connect-timeout 5 --max-time 10 --retry 5 --retry-delay 0 --retry-max-time 60 -f)\n")
      .append("  if [ ! -z \"$OPTIMIZATION_ID\" ]; then\n")
      .append("      break\n")
      .append("  fi\n")
      .append("  echo \"Metadata values were not set, retrying in 2 seconds\"\n")
      .append("  sleep 2\n")
      .append("done\n")
      .append("echo \"Optimization Id: $OPTIMIZATION_ID\"\n")
      .append("cd $HOME_DIRECTORY\n")
      .append("echo java -jar softball-sim.jar " + applicationArguments + "\n")
      .append("java -jar softball-sim.jar " + applicationArguments + "\n")
      .append("echo deleting self" + "\n")
      .append("gcloud --quiet compute instances delete " + instanceName + " --zone=" + zone + "\n") // Delete the instance after job completes
      .toString();

      JsonObject jsonObject = new JsonObject();
      jsonObject.add(GcpFunctionsEntryPointCompute.ARGS_KEY, new JsonPrimitive(applicationArguments));
      jsonObject.add(GcpFunctionsEntryPointCompute.ID_KEY, new JsonPrimitive(optimizationId));
      jsonObject.add(GcpFunctionsEntryPointCompute.ZONES_KEY, new JsonPrimitive(String.join(",", futureZones)));
      jsonObject.add(GcpFunctionsEntryPointStart.PASSWORD_KEY, new JsonPrimitive(pwd)); // TODO: this is pretty sloppy, switch to use service accounts
      Gson gson = GsonAccessor.getInstance().getCustom();
      String jsonPayload = gson.toJson(jsonObject);
      Logger.log("Payload on shutdown " + jsonPayload);

      String shutdownScript = new StringBuilder()
      .append("#!/bin/sh\n")
      .append("echo running shutdown script\n")
      //.append("killall -9 java")
      .append("curl -X POST 'https://us-central1-optimum-library-250223.cloudfunctions.net/softball-sim-compute' -N -H 'Content-Type:application/json' --data '" + BashEscape.SHELL_ESCAPE.escape(jsonPayload) + "'\n")
      .toString();

    Instance instance = new Instance();
    instance
      .setName(instanceName)
      .setZone(String.format("projects/%s/zones/%s", project, zone))
      .setMachineType(String.format("projects/%s/zones/%s/machineTypes/%s", project, zone, machineType))
      .setMetadata(new Metadata().setItems(new ImmutableList.Builder<Metadata.Items>()
        .add(new Metadata.Items().setKey("startup-script").setValue(startupScript))
        .add(new Metadata.Items().setKey("shutdown-script").setValue(shutdownScript))
        .add(new Metadata.Items().setKey("optimization-id").setValue(optimizationId)).build()))
      .setDisks(new ImmutableList.Builder<AttachedDisk>()
        .add(new AttachedDisk()
          .setType("PERSISTENT")
          .setBoot(true)
          .setMode("READ_WRITE")
          .setAutoDelete(true)
          .setDeviceName(instanceName)
          .setInitializeParams(new AttachedDiskInitializeParams().setSourceSnapshot("global/snapshots/" + snapshotName))).build())
      .setCanIpForward(false)
      .setNetworkInterfaces(new ImmutableList.Builder<NetworkInterface>()
        .add(new NetworkInterface().setNetwork(String.format("projects/%s/global/networks/default", project))
          .setAccessConfigs(new ImmutableList.Builder<AccessConfig>()
            .add(new AccessConfig()
              .setName("External NAT")
              .setType("ONE_TO_ONE_NAT")).build())).build())
      .setScheduling(new Scheduling().setPreemptible(true))
      // It is best practice to set the scope to full access (cloud-platform) and control access using IAM
      // https://cloud.google.com/compute/docs/access/service-accounts#accesscopesiam
      .setServiceAccounts(new ImmutableList.Builder<ServiceAccount>()
        .add(new ServiceAccount()
          .setEmail("default")
          .setScopes(new ImmutableList.Builder<String>().add("https://www.googleapis.com/auth/cloud-platform").build())).build());
    
    // @formatter:on

    // Send the request to create the instance
    Compute compute = GCPComputeClientHelper.getComputeInstance();
    return compute.instances().insert(project, zone, instance).execute();
  }
}
