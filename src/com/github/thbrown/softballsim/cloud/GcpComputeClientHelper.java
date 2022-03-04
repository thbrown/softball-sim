package com.github.thbrown.softballsim.cloud;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class GcpComputeClientHelper {
  private static Compute compute = null;

  protected GcpComputeClientHelper() {
    // Exists only to defeat instantiation
  }

  public static Compute getComputeInstance() throws GeneralSecurityException, IOException {
    if (compute == null) {
      compute = build();
    }
    return compute;
  }

  private static Compute build() throws GeneralSecurityException, IOException {
    // Create http transporter needed for Compute client
    HttpTransport HTTP_TRANSPORTER = GoogleNetHttpTransport.newTrustedTransport();

    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

    HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

    // Create and return GCP Compute client
    return new Compute.Builder(HTTP_TRANSPORTER, JacksonFactory.getDefaultInstance(), requestInitializer)
        .setApplicationName("softball-app").build();
  }

}
