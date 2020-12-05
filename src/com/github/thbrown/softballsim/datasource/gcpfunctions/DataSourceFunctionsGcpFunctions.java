package com.github.thbrown.softballsim.datasource.gcpfunctions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import com.github.thbrown.softballsim.Result;
import com.github.thbrown.softballsim.datasource.ProgressTracker;
import com.github.thbrown.softballsim.datasource.local.DataSourceFunctionsFileSystem;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.gson.Gson;

public class DataSourceFunctionsGcpFunctions extends DataSourceFunctionsFileSystem {

  public static String BUCKET_NAME = "softball-sim-temp";
  public static Charset ENCODING = StandardCharsets.UTF_8;
  private Gson gson;

  public DataSourceFunctionsGcpFunctions(String fileName) {
    super(fileName);
    this.gson = GsonAccessor.getInstance().getCustom();
  }

  @Override
  public void onUpdate(ProgressTracker tracker) {
    super.onUpdate(tracker);
    Result latestResult = tracker.getCurrentResult();
    upsertBlob(gson.toJson(latestResult), this.cacheFileName);
  }

  @Override
  public void onComplete(Result finalResult) {
    super.onComplete(finalResult);
    deleteBlob(this.cacheFileName);
  }

  private void upsertBlob(String data, String blobName) {
    Logger.log("Persist to bucket " + BUCKET_NAME + ":" + blobName);
    Storage storage = StorageOptions.getDefaultInstance().getService();

    BlobId blobId = BlobId.of(BUCKET_NAME, blobName);
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

  private void deleteBlob(String blobName) {
    Logger.log("Delete to blob " + BUCKET_NAME + ":" + blobName);
    Storage storage = StorageOptions.getDefaultInstance().getService();
    BlobId blobId = BlobId.of(BUCKET_NAME, blobName);
    Blob remoteBlob = storage.get(blobId);
    if (remoteBlob != null) {
      remoteBlob.delete();
    }
  }

}
