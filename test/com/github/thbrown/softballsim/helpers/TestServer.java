package com.github.thbrown.softballsim.helpers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import com.github.thbrown.softballsim.SoftballSim;
import com.github.thbrown.softballsim.commands.BaseOptimizationCommand;
import com.github.thbrown.softballsim.commands.OptimizationCommandDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Class for testing the NETWORK data source mode.
 * 
 * This starts a server on the localhost that supplies data to the softball-sim application. The application expects to receive
 * information in this manner when it is started and the 'NETWORK' options is supplied as an argument for with the -s flag.
 */
public class TestServer implements Runnable {
  
  private Object lock;
  private ProcessHooks hooks;
  
  public TestServer(Object lock, ProcessHooks hooks) {
    this.lock = lock;
    this.hooks = hooks;
  }

  @Override
  public void run() {
    try {
      this.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private void start() throws Exception {
    System.out.println("Server starting!");
    ServerSocket serverSocket = new ServerSocket(8414);
    
    synchronized(lock) {
      lock.notify();
    }
    
    Socket clientSocket = serverSocket.accept();
    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    String inputLine;
    
    while ((inputLine = in.readLine()) != null) {
      // System.out.println("SERVER: " + inputLine);
      
      // Each command will process itself, you can pass in desired behavior via a your own implementation of hooks
      final GsonBuilder gsonBuilder = new GsonBuilder();
      gsonBuilder.registerTypeAdapter(BaseOptimizationCommand.class, new OptimizationCommandDeserializer());
      Gson gson = gsonBuilder.create();
      
      BaseOptimizationCommand command = gson.fromJson(inputLine, BaseOptimizationCommand.class);
      boolean done = command.process(hooks, out);
      if (done) {
        break;
      }
    }

    in.close();
    out.close();
    clientSocket.close();
    serverSocket.close();
  }
  
  public static void runSimulationOverNetwork(ProcessHooks sm, boolean invokeCleanupScript) {
    try {
      // Start the server on its own thread
      Object lock = new Object();
      TestServer server = new TestServer(lock, sm);
      Thread t = new Thread(server);
      t.start();
      synchronized(lock) {
        lock.wait();
      }
      SoftballSim.main(new String[] {"-o", "0", "-d", "NETWORK", "-l" , "127.0.0.1", "-i", "0000000000", "-c", String.valueOf(invokeCleanupScript)});
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }
  
}
