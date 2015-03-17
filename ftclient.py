# Adam Cankaya, cankayaa@onid.oregonstate.edu
# CS 372 Project 2
# Due Mar 8, 2015

# References: 
#   Boiler plate socket code from CS372 class lecture videos
#   Deleting last char from a file - http://stackoverflow.com/questions/18857352/python-remove-very-last-character-in-file

# Client-server FTP-like program for getting directory contents and transfering text files. 
    # Server starts and waits for client to request a control connection on <PORTNUM>
    # Client sends comment to server to either display current directory contents or send a file
    # Server begins listening for data connection on <DATAPORT> and once established sends requested data
    # Server closes data connection, client closes control connection, and server waits for next client to connect

#   Run on flip1.engr.oregonstate.edu
#   Run Python server: python chatserver.py <PORTNUM>
#   Run Python client: python chatclient.py <HOST> <PORTNUM> <COMMAND> <DATAPORT> <FILENAME>
#   Send SIG_INT (Ctrl+C) to shut down server

import sys
from socket import *
import time
import os

buf_size = 2048 # 2048 bytes / 4 bytes per character = up to 512 characters per message
host_len = 50   # limit host address to 30 chars
comm_len = 3    # limit command to 3 chars
file_len = 15   # file name limited to 15 chars
handle = "MrClient"

# Validate command arguments
def validate():
    global serverName, serverPort, clientCommand, dataPort, fileName, file_len
    if(len(sys.argv) < 4 or len(sys.argv) > 6):
        sys.exit("Invalid number of parameters - please enter 3 to 5 parameters.")
    serverName = str(sys.argv[1])
    serverPort = int(sys.argv[2])
    clientCommand = str(sys.argv[3])
    if(len(sys.argv) > 4):
        try:
            dataPort = int(sys.argv[4])    
            # Validate data port is int between 0 and 65535
            if(dataPort >= 1 and dataPort <= 65535):
                pass
            else:
                Sys.exit("Invalid data port - please enter a port between 1 and 65535.")
        except ValueError, IndexError:
            print "%s> Invalid data port - please enter a data port between 1 and 65535" % (handle)
    if(len(sys.argv) > 5):
        try:
            fileName = str(sys.argv[5])
            # Validate if filename is present it is a string of acceptable length
            if(len(fileName) <= file_len):
                pass
            else:
                Sys.exit("Invalid filename - please enter a valid file to get.")
        except ValueError, IndexError:
            print "%s> Invalid file name - please enter a file name up to %d length" % (handle, file_len)
    # Validate server host is valid string
    if(len(serverName) <= host_len):
        pass
    else:
        Sys.exit("Invalid server name - please enter a valid host.")
    # Validate server port is int between 0 and 65535
    if(serverPort >= 1 and serverPort <= 65535):
        pass
    else:
        Sys.exit("Invalid server port - please enter a server port between 1 and 65535.")
    # Validate command is valid string
    if(len(clientCommand) <= comm_len):
        pass
    else:
        Sys.exit("Invalid command - please enter -l to get directory contents or -g to get a file.")

# Establish control connection to server host:port, print and exchange intro sentenceit
def connect():
    global serverName, serverPort, clientCommand, dataPort, fileName, clientSocket, buf_size, file_len
    validate()
    print "%s> Connecting to %s:%d..." % (handle, serverName, serverPort)
    clientSocket = socket(AF_INET, SOCK_STREAM)
    clientSocket.connect((serverName, serverPort))
    sentence = "%s> Hello, I'd like to connect on port %d please.\n" % (handle, serverPort)
    clientSocket.send(sentence)
    response = clientSocket.recv(buf_size)
    print response

# Send either -l or -g <filename> command to server
def sendCommand():
    global clientSocket, clientCommand, fileName, dataPort, handle
    # If command is to list directory
    if("-l" in clientCommand):
        clientSocket.send(clientCommand + " " + str(dataPort) + "\n")
        getDirectory()
        return
    # If command is to get <filename>
    if("-g" in clientCommand):
        if(len(sys.argv) > 5):
            getFile()
        else:
            print "%s> Invalid command, please try -l <dataport> or -g <dataport> <filename> ." % (handle)
    else:
        print "%s> Invalid command, please try -l <dataport> or -g <dataport> <filename> ." % (handle)

def dataConnection():
    global dataSocket, serverName, dataPort
    # Wait for server to start listening on data port then initiate connection
    print "Initiating data connection to %s:%s" % (serverName, dataPort)
    time.sleep(3)   # to ensure server has started listening
    dataSocket = socket(AF_INET, SOCK_STREAM)
    dataSocket.connect((serverName, dataPort))
    print "Data connection established, waiting on download"

# Establish data connection with server and wait for directory contents to be sent, then close both connections
def getDirectory():
    global clientSocket, serverName, dataPort, handle, dataSocket, buf_size
    dataConnection()
    # Get directory contents as response and print
    response = dataSocket.recv(buf_size)
    while(response):
        print response
        response = dataSocket.recv(buf_size)
    # Close both connection sockets
    dataSocket.close()
    clientSocket.close()
    print "%s> Connections closed!" % (handle)

# Establish data connection with server and wait for text file contents to be sent, then close both connections
def getFile():
    global clientSocket, serverName, dataPort, handle, dataSocket, buf_size
    # Check for existing file
    files = os.listdir(".")
    for file in files:
        if fileName in file:
            print "File with name %s already exists in current directory - please try again" % (fileName)
            return
    print "%s> Requesting to get file %s on data port %d ..." % (handle, fileName, dataPort)
    clientSocket.send(clientCommand + " " + str(dataPort) + " " + fileName  + "\n")
    dataConnection()
    response = dataSocket.recv(buf_size)
    print response
    # Check for file not found message from
    if "File not found" in response:
        return
    bool = "true"
    # Open file and write response from server line-by-line
    with open(fileName, 'w') as f:
        response = dataSocket.recv(buf_size)
        while(bool == "true"):
            line = f.write(response)
            response = dataSocket.recv(buf_size)
            if "File transfer complete" in response:    # do not write to file!
                print response
                bool = "false"    # break while loop
    # Remove final new line character from file
    with open(fileName, 'rb+') as f:
        f.seek(-1, os.SEEK_END)
        f.truncate()
    # Close both connection sockets
    dataSocket.close()
    clientSocket.close()
    print "%s> Connections closed!" % (handle)



connect()
sendCommand()