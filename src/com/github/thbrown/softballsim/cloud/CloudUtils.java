package com.github.thbrown.softballsim.cloud;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.github.thbrown.softballsim.util.StringUtils;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Collection of utils for cloud operations.
 * 
 * Some thoughts: 1) Does this belong in the utils package? or the cloud package? 2) At some point
 * we'll probably want to mock this for test purposes, so it might be good for this to be a
 * singleton.
 */
public class CloudUtils {

  public static Charset ENCODING = StandardCharsets.UTF_8;

  /**
   * Updates an existing blob in the specified bucket such that it now contains 'data'. If the
   * specified blob does not exist in the specified bucket, a new blob will be created. This method
   * will not create a new bucket, however
   */
  public static void upsertBlob(String data, String blobName, String bucketName) {
    Logger.log("Persist to bucket " + bucketName + ":" + blobName);
    Storage storage = StorageOptions.getDefaultInstance().getService();

    BlobId blobId = BlobId.of(bucketName, blobName);
    Blob blob = storage.get(blobId);
    if (blob == null) {
      BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build();
      blob = storage.create(blobInfo, data.getBytes(ENCODING));
    } else {
      // byte[] prevContent = blob.getContent();
      // System.out.println(new String(prevContent, StandardCharsets.UTF_8));
      WritableByteChannel channel = blob.writer();
      try {
        channel.write(ByteBuffer.wrap(data.getBytes(ENCODING)));
        channel.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Deletes the specified blob from the specified GCP Storage Bucket.
   */
  public static void deleteBlob(String blobName, String bucketName) {
    Logger.log("Delete to blob " + bucketName + ":" + blobName);
    Storage storage = StorageOptions.getDefaultInstance().getService();
    BlobId blobId = BlobId.of(bucketName, blobName);
    Blob remoteBlob = storage.get(blobId);
    if (remoteBlob != null) {
      remoteBlob.delete();
    }
  }

  /**
   * Reads the entire content of the GCP Storage Bucket blob into a String and returns the result.
   */
  public static String readBlob(String blobName, String bucketName) {
    Logger.log("Read blob " + bucketName + ":" + blobName);
    Storage storage = StorageOptions.getDefaultInstance().getService();

    BlobId blobId = BlobId.of(bucketName, blobName);
    try {
      byte[] content = storage.readAllBytes(blobId);
      return new String(content, CloudUtils.ENCODING);
    } catch (StorageException e) {
      Logger.log("Unable to read " + bucketName + ":" + blobName + " returning null " + e.toString());
      return null;
    }
  }

  public static void send400Error(HttpResponse response, String message) throws IOException {
    response.setContentType("application/json");
    response.setStatusCode(400);
    String payload = getResponseJson("ERROR", message);
    response.getOutputStream().write(payload.getBytes());
    response.getOutputStream().flush();
  }

  public static String getResponseJson(String status, String message) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.add("status", new JsonPrimitive(status));
    jsonObject.add("message", new JsonPrimitive(message));
    return GsonAccessor.getInstance().getDefault().toJson(jsonObject);
  }

}
