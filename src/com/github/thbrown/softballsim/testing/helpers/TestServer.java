package com.github.thbrown.softballsim.testing.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import com.github.thbrown.softballsim.SoftballSim;
import com.google.gson.Gson;

public class TestServer implements Runnable {
  
  private Object lock;
  private ServerMethods methods;
  
  public TestServer(Object lock, ServerMethods methods) {
    this.lock = lock;
    this.methods = methods;
  }

  @Override
  public void run() {
    try {
      this.start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private void start() throws IOException {
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
      //System.out.println("SERVER: " + inputLine);
      
      Gson gson = new Gson();
      Map<String, String> data = gson.fromJson(inputLine, Map.class);
      String command = data.get("command");

      if(command.equals("READY")) {
        methods.onReady(out);
      } else if (command.equals("COMPLETE")) {
        methods.onComplete(data);
        break;
      }
    }
    
    in.close();
    out.close();
    clientSocket.close();
    serverSocket.close();
  }
  
  public static void runSimulationOverNetwork(ServerMethods sm) {
    try {
      
      // Start the server in its own thread
      Object lock = new Object();
      TestServer server = new TestServer(lock, sm);
      Thread t = new Thread(server);
      t.start();
      synchronized(lock) {
        lock.wait();
      }
      
      // Then start the simulator (the client)
      SoftballSim.main(new String[] {"NETWORK"});
      
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }
  
}
