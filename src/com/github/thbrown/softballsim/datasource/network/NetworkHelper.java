package com.github.thbrown.softballsim.datasource.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import com.github.thbrown.softballsim.util.GZIPCompression;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.google.gson.Gson;

/**
 * Class that abstracts away compression and deserialization when writing commands to the network.
 */
public class NetworkHelper {

  private final Gson gson = GsonAccessor.getInstance().getCustom();
  private final OutputStream outputStream;
  private final InputStream inputStream;

  public NetworkHelper(Socket socket) throws IOException {
    outputStream = socket.getOutputStream();
    inputStream = socket.getInputStream();
  }

  public DataSourceNetworkCommand readCommand() throws IOException {
    // Get the next string from the socket
    String json = readString();

    // Deserialize that json to our command object
    DataSourceNetworkCommand command = gson.fromJson(json, DataSourceNetworkCommand.class);
    Logger.log("READ: " + command);

    return command;
  }

  protected String readString() throws IOException {
    // First 4 bytes represent the size of the payload - read that first
    byte[] sizeInBytes = readAllBytes(inputStream, 4);
    int size = NetworkHelper.byteArrayToInt(sizeInBytes);

    // Now read the rest of the payload
    byte[] data = readAllBytes(inputStream, size);

    // Unzip the data, this will give us the json
    String json = GZIPCompression.decompress(data);
    return json;
  }

  public void writeCommand(DataSourceNetworkCommand command) {
    // Serialize the command to json
    String json = gson.toJson(command);

    // Write it to the socket
    writeString(json);

    Logger.log("WRITE: " + command);
  }

  protected void writeString(String json) {
    try {
      // compress
      byte[] data = GZIPCompression.compress(json);

      // determine size
      int dataSize = data.length;
      byte[] dataSizeBytes = intToByteArray(dataSize);

      // send
      outputStream.write(dataSizeBytes);
      outputStream.write(data);
      outputStream.flush();
    } catch (IOException e) {
      throw new RuntimeException("Failed to write to network");
    }
  }

  public void close() throws IOException {
    outputStream.close();
    inputStream.close();
  }

  protected Gson getGson() {
    return this.gson;
  }

  private static int byteArrayToInt(byte[] b) {
    return b[3] & 0xFF |
        (b[2] & 0xFF) << 8 |
        (b[1] & 0xFF) << 16 |
        (b[0] & 0xFF) << 24;
  }

  private static byte[] intToByteArray(int a) {
    return new byte[] {
        (byte) ((a >> 24) & 0xFF),
        (byte) ((a >> 16) & 0xFF),
        (byte) ((a >> 8) & 0xFF),
        (byte) (a & 0xFF)
    };
  }

  /**
   * Read the specified number of bytes from the input stream. Block until all expected bytes have
   * been read.
   * 
   * TODO: add timeout?
   * 
   * @return the read bytes or null if the stream was closed before the specified number of bytes
   *         could be read
   */
  private byte[] readAllBytes(InputStream inputStream, int bytesToRead) throws IOException {
    int totalBytesRead = 0;
    byte[] data = new byte[bytesToRead];
    do {
      int bytesRead = inputStream.read(data, totalBytesRead, bytesToRead - totalBytesRead);
      totalBytesRead += bytesRead;
      if (bytesRead == -1) {
        // Socket is closed
        return null;
      }
    } while (totalBytesRead != bytesToRead);
    return data;
  }

}
