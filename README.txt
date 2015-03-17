Adam Cankaya, cankayaa@onid.oregonstate.edu
CS 372 Project 2
Due Mar 8, 2015

# Client-server FTP-like program for getting directory contents and transfering text files. 
    # Server starts and waits for client to request a control connection on <PORTNUM>
    # Client sends comment to server to either display current directory contents or send a file
    # Server begins listening for data connection on <DATAPORT> and once established sends requested data
    # Server closes data connection, client closes control connection, and server waits for next client to connect

Run both client and server on flip1.engr.oregonstate.edu

Start Java server by compiling and then providing a port on the command line:
    javac chatserver.java
    java chatserver <PORTNUM>

Start Client and connect to server by providing hostname, port number, command, data port and optional filename:
    python chatclient.py <HOST> <PORTNUM> <COMMAND> <DATAPORT> <FILENAME (optional)>

The following commands are supported:
    -l <DATAPORT>: Request server to list current directory contents via data port
    -g <DATAPORT> <FILENAME>: Request server to send filename via data port

Once the initial TCP control connection is established, the client sends the command and waits for the server's response. The server will initiate a second TCP data connection and either list the directory contents or send the requested file over this data connection. When complete the server and client close all connections.

Server will continue to run and wait for next connection on <PORTNUM>.

Server can be shut down by sending a SIG_INT (Ctrl+C).