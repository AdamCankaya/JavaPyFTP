// Adam Cankaya, cankayaa@onid.oregonstate.edu
// CS 372 Project 2
// Due Mar 8, 2015

// References: 
//  Boiler plate socket code from CS372 class lecture videos
//  EOL character help from https://norwied.wordpress.com/2012/04/17/how-to-connect-a-python-client-to-java-server-with-tcp-sockets/

// Client-server FTP-like program for getting directory contents and transfering text files. 
// Server starts and waits for client to request a control connection on <PORTNUM>
// Client sends comment to server to either display current directory contents or send a file
// Server begins listening for data connection on <DATAPORT> and once established sends requested data
// Server closes data connection, client closes control connection, and server waits for next client to connect

//   Run on flip1.engr.oregonstate.edu
//   Compile Java server: javac chatserver.java
//   Run Java server: java chatserver <PORTNUM>
//   Run Python client: python chatclient.py <HOST> <PORTNUM>
//   Send SIG_INT (Ctrl+C) to shut down server

import java.net.*;
import java.io.*;
import java.util.*;

public class ftserver {
    // Server properties
    public static String handle = "MrServer> ";
    public static int serverPort = 0;
    public static String[] clientArgs; // [<command>, <dataPort>, <filename>]
    public static int dataPort = 0;
    public static String dataPortStr = "";
    public static String clientCommand = "";
    public static String fileName = null;
    // Socket variables
    public static ServerSocket serverSocket = null;
    public static Socket clientSocket = null;
    public static ServerSocket serverDataSocket = null;
    public static Socket clientDataSocket = null;
    // I/O variables
    public static BufferedReader in = null;
    public static PrintWriter out = null;
    public static BufferedReader inData = null;
    public static PrintWriter outData = null;
    public static Scanner in_scan = null;
    public static String clientInput, serverInput;
    // Other variables
    public static boolean loop = true;
    public static String newline = System.getProperty("line.separator");
    
    // Initialize shutdown protocol and get server port from command line
    public static void setupServer(String[] args) {
        // Install shutdown hook MyShutdown
        MyShutdown sh = new MyShutdown();
        Runtime.getRuntime().addShutdownHook(sh);
        
        // Get server port
        in_scan = new Scanner(System.in);
        serverPort = Integer.parseInt(args[0]);
        
        return;
    }
    
    // Start server and wait for client to initiate connection then create socket and exchange introduction
    public static void startServer() {
        try {
            System.out.println("Waiting on a new client to connect on port " + serverPort);
            serverSocket = new ServerSocket(serverPort);
            clientSocket = serverSocket.accept();
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));      // default buffer > 512 chars
            out = new PrintWriter(clientSocket.getOutputStream(), true);
    
            // Listen for intro message from client, print it, send intro message to client, print it
            clientInput = in.readLine();
            System.out.println(clientInput);
            out.println(handle + "> I'm ready on port " + serverPort + "!");
        }
        catch(IOException e) {
            System.out.println("startServer(): Exception caught when trying to listen on port " + serverPort);
            System.out.println(e.getMessage()); 
            System.exit(0);  // Run shutdown hook
        }

        return;
    }
    
    // Listen for command message from client and split args by spaces
    public static void getCommands() {
        fileName = null;
        try {
            clientInput = in.readLine();
            clientArgs = clientInput.split("\\s+");
            clientCommand = clientArgs[0];
            dataPortStr =  clientArgs[1];
            dataPort =  Integer.parseInt(dataPortStr);
            if(clientArgs.length > 2)
                fileName = clientArgs[2];
            if(fileName != null)
                System.out.println("fileName: " + fileName);
            
            // If command is to send directory contents
            if(clientCommand.contains("l")) {
                dataConnection();
                sendDirectory();
            }
            // Else if command is to send a file
            else if(clientCommand.contains("g")) {
                dataConnection();
                sendFile();
            }
            else {
                out.println("Please enter a valid command");
                System.out.println("Please enter a valid command");
            }
        }
        catch(Exception e) {
            System.out.println("getCommands(): Exception caught while trying to close sockets.");
            System.out.println(e.getMessage());
        }
        
        return;
    }
    
    // Start listening for a data connection to be requested by client
    public static void dataConnection() {
        try {
            System.out.println("Waiting on client to connect on port " + dataPort);
            serverDataSocket = new ServerSocket(dataPort);
            clientDataSocket = serverDataSocket.accept();
            inData = new BufferedReader(new InputStreamReader(clientDataSocket.getInputStream()));      // default buffer > 512 chars
            outData = new PrintWriter(clientDataSocket.getOutputStream(), true);
        }
        catch(IOException e) {
            System.out.println("startServer(): Exception caught when trying to listen on port " + serverPort);
            System.out.println(e.getMessage()); 
        }
        return;
    }
    
    // Get current directory contents and send it to client, then close the data connection
    public static void sendDirectory() {
        System.out.println("Sending directory contents...");
        // Start listening for data connection on dataPort
        outData.println(handle + "> Directory contents: ");
        
        // Get file contents as array and send names to client
        File dir = new File(".");
        File[] files = dir.listFiles();
        for(int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                outData.println(files[i].getName());
            }
        }
        try{Thread.sleep(3000);}    // to ensure client has received all data
        catch(InterruptedException ex){Thread.currentThread().interrupt();}
        System.out.println("Directory contents transfer complete - closing data connection.");
        closeDataConnection();
        return;
    }
    
    // Get text file contents and send it line-by-line to client, then close the data connection
    public static void sendFile() {
        // Make sure file exists
        Boolean bool = false;
        File dir = new File(".");
        File[] files = dir.listFiles();
        for(int i = 0; i < files.length; i++) {
            if( (files[i].getName()).equals(fileName) ) {
                bool = true;        // matching file name found
                i = files.length;   // break for loop
            }
        }
        
        // if file not found
        if(bool == false) {
            outData.println(handle + "> File not found - please try again. Closing all connections.");
            System.out.println(handle + "> File not found - please try again. Closing all connections.");
            return;
        }
        
        System.out.println(handle + "> Sending " + fileName + "...");
        outData.println(handle + "> Sending " + fileName + "...");
       
        try { 
            FileReader fr = new FileReader(fileName);
            BufferedReader br = new BufferedReader(fr);
            String line = null;
            while( (line = br.readLine()) != null) {
                outData.print(line + newline);
                //outData.println();
            }
            fr.close();
            br.close();
        }
        catch(Exception e) {
            System.out.println("sendFile(): Exception caught while trying to send file contents");
            System.out.println(e.getMessage());
        }

        outData.flush();
        try{Thread.sleep(3000);}    // to ensure client has received all data
        catch(InterruptedException ex){Thread.currentThread().interrupt();}
        outData.println("File transfer complete - closing both connections.");
        System.out.println("File transfer complete - closing both connections.");
        closeDataConnection();
        return;
    }
    
    // Close buffers and sockets associated with the data connection
    public static void closeDataConnection() {
        try {
            if(serverDataSocket != null)
                serverDataSocket.close();
            if(clientDataSocket != null)
                clientDataSocket.close();
            if(inData != null)
                inData.close();
            if(outData != null)
                outData.close();
        }
        catch(Exception e) {
            System.out.println("closeDataConnection(): Exception caught while trying to close sockets.");
            System.out.println(e.getMessage());
        }
        return;
    }
    
    // Close all buffers and sockets for both data and control connections
    public static void closeEverything() {
        try {
            // Be sure all sockets and buffers are closed
            if(serverSocket != null)
                serverSocket.close();
            if(clientSocket != null)
                clientSocket.close();
            if(in != null)
                in.close();
            if(out != null)
                out.close();
            if(serverDataSocket != null)
                serverDataSocket.close();
            if(clientDataSocket != null)
                clientDataSocket.close();
            if(inData != null)
                inData.close();
            if(outData != null)
                outData.close();
        }
        catch(Exception e) {
            System.out.println("closeEverything(): Exception caught while trying to close sockets.");
            System.out.println(e.getMessage());
        }

        return;
    }
    
    // Close all sockets & buffers and shutdown server if interupt signal (Ctrl+C) is received    
    public static class MyShutdown extends Thread {
        public void run() {
            System.out.println("\nServer shutting down.");
            if(out != null)
                out.println(handle + "Connection closed by server.");

            closeEverything();
        }
    }
    
    public static void main(String[] args) throws IOException {
        setupServer(args);
        while(loop) {   // loop until SIG_INT
            try {
                startServer();
                getCommands();
            }
            catch(Exception e) {
                System.out.println("main(): Exception caught when trying to listen on port " + serverPort);
                System.out.println(e.getMessage());
                break;
            }
            finally {
                closeEverything();
            }
        }
    }
}