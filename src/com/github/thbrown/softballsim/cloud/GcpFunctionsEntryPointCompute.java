package com.github.thbrown.softballsim.cloud;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.github.thbrown.softballsim.datasource.gcpfunctions.DataSourceGcpBuckets;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.*;
import com.google.common.collect.ImmutableList;

/**
 * A GCP function for starting up a preemptable compute instance. Unlike the
 * other this isn't supposed to be called over the public internet. The is
 * intended to be called by the start function if the function runtime expires,
 * or by a compute instance shut down script in the case of preemption.
 */
public class GcpFunctionsEntryPointCompute implements HttpFunction {

  @Override
  public void service(HttpRequest request, HttpResponse response) throws Exception {

    // TODO: the actual call
    try {
      response.setContentType("application/json");
      response.setStatusCode(200);
      response.getOutputStream().write("".getBytes());
    } catch (Exception e) {
      send400Error(response, e.toString());
    }
  }

  private void send400Error(HttpResponse response, String message) throws IOException {
    response.setContentType("text/plain");
    response.setStatusCode(400);
    response.getOutputStream().write(message.getBytes());
  }

  private Operation makeInstance(String optimizationId, String zone, String project, String machingType,
      Compute compute, String workerId, String instanceName) throws IOException {

    // @formatter:off 
      String startupScript = new StringBuilder()
      .append("#!/bin/sh")
      .append("HOME_DIRECTORY=\"your_home_directory\"\n") // TODO: env variable?
      .append("for i in 1 2 3 4 5; do\n")
      .append("  REMOTE_IP=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/remote-ip -H \"Metadata-Flavor: Google\" --connect-timeout 5 --max-time 10 --retry 5 --retry-delay 0 --retry-max-time 60 -f)\n")
      .append("  OPTIMIZATION_ID=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/optimization-id -H \"Metadata-Flavor: Google\" --connect-timeout 5 --max-time 10 --retry 5 --retry-delay 0 --retry-max-time 60 -f)\n")
      .append("  if [ ! -z \"$OPTIMIZATION_ID\" ]; then\n")
      .append("      break\n")
      .append("  fi\n")
      .append("  echo \"Metadata values were not set, retrying in 2 seconds\"\n")
      .append("  sleep 2\n")
      .append("done\n")
      .append("echo \"Optimization Id: $OPTIMIZATION_ID\"\n")
      .append("\n")
      .append("cd \"/home/$HOME_DIRECTORY\"\n") // TODO: download jar
      .append("java -jar /home/$HOME_DIRECTORY/softball-sim.jar NETWORK $REMOTE_IP $OPTIMIZATION_ID true\n")
      .toString();

    Instance instance = new Instance();
    instance
      .setName(instanceName)
      .setZone(String.format("projects/%s/zones/%s", project, zone))
      .setMachineType(String.format("projects/%s/zones/%s/machineTypes/%s", project, zone, machingType))
      .setMetadata(new Metadata().setItems(new ImmutableList.Builder<Metadata.Items>()
        .add(new Metadata.Items().setKey("startup-script").setValue(startupScript))
        .add(new Metadata.Items().setKey("optimization-id").setValue(optimizationId)).build()))
      .setDisks(new ImmutableList.Builder<AttachedDisk>()
        .add(new AttachedDisk()
          .setType("PERSISTENT")
          .setBoot(true)
          .setMode("READ_WRITE")
          .setAutoDelete(true)
          .setDeviceName(instanceName)
          .setSource(String.format("projects/%s/zones/%s/disks/%s", project, zone, instanceName))).build())
      .setCanIpForward(false)
      .setNetworkInterfaces(new ImmutableList.Builder<NetworkInterface>()
        .add(new NetworkInterface().setNetwork(String.format("projects/%s/global/networks/default", project))
          .setAccessConfigs(new ImmutableList.Builder<AccessConfig>()
            .add(new AccessConfig()
              .setName("External NAT")
              .setType("ONE_TO_ONE_NAT")).build())).build())
      .setScheduling(new Scheduling().setPreemptible(true))
      .setServiceAccounts(new ImmutableList.Builder<ServiceAccount>()
        .add(new ServiceAccount()
          .setEmail("default")
          .setScopes(new ImmutableList.Builder<String>().add("https://www.googleapis.com/auth/cloud-platform").build())).build());
    
    // @formatter:on

    return compute.instances().insert(project, zone, instance).execute();
  }

}
