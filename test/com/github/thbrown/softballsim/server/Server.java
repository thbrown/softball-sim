package com.github.thbrown.softballsim.server;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Test class for testing the NETWORK data source mode.
 * 
 * This starts a server on the localhost that supplies data to the softball-sim application. The
 * application expects to receive information in this manner when it is started with the 'NETWORK'
 * option (-s flag).
 */
public class Server implements Runnable {

  private Object lock;
  private ServerCommandHooks hooks;

  private Server(Object lock, ServerCommandHooks hooks) {
    this.lock = lock;
    this.hooks = hooks;
  }

  @Override
  public void run() {
    try {
      this.runServer();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void runServer() throws Exception {
    System.out.println("Server starting!");
    ServerSocket serverSocket = new ServerSocket(8414);

    // We are ready to begin listening, report that the server is ready
    synchronized (lock) {
      lock.notify();
    }

    Socket clientSocket = serverSocket.accept();
    ServerNetworkHelper network = new ServerNetworkHelper(clientSocket);
    ServerCommand inputCommand;

    while ((inputCommand = network.readServerCommand()) != null) {
      // System.out.println("SERVER: " + inputLine);

      // Each command will process itself, you can pass in desired behavior via a your own implementation
      // of hooks
      boolean done = inputCommand.process(hooks, network);
      if (done) {
        break;
      }
    }

    network.close();
    clientSocket.close();
    serverSocket.close();
  }

  public static void start(ServerCommandHooks sm) {
    Thread t = null;
    try {
      // Start the server on its own thread
      Object lock = new Object();
      Server server = new Server(lock, sm);
      t = new Thread(server);

      // Don't return until the server has started
      synchronized (lock) {
        t.start();
        lock.wait();
      }
    } catch (Exception e) {
      t.interrupt();
      throw new RuntimeException(e);
    }
  }

}
