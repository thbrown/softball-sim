package com.github.thbrown.softballsim.server;

import java.io.IOException;
import java.net.Socket;
import com.github.thbrown.softballsim.datasource.network.NetworkHelper;
import com.github.thbrown.softballsim.helpers.TestGsonAccessor;
import com.github.thbrown.softballsim.util.GsonAccessor;
import com.github.thbrown.softballsim.util.Logger;
import com.google.gson.Gson;

public class ServerNetworkHelper extends NetworkHelper {

  private Gson gson;

  public ServerNetworkHelper(Socket socket) throws IOException {
    super(socket);
    gson = TestGsonAccessor.getInstance().getCustom();
  }

  public ServerCommand readServerCommand() throws IOException {
    // Get the next string from the socket
    String json = readString();

    // Deserialize that json to our command
    Logger.log(json);
    ServerCommand command = gson.fromJson(json, ServerCommand.class);
    return command;
  }

  public void writeServerCommand(ServerCommand command) throws IOException {
    // Serialize the command to json
    String json = gson.toJson(command);

    // Write it to the socket
    writeString(json);
  }

}
