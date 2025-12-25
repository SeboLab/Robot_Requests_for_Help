package com.example.misty.Socketconnection;
//package com.example.myapplication.Socketconnection;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {

    public static TCPServer singleton;

    public static TCPServer getSingleton() {
        if (singleton!=null)
            return singleton;
        else {
            try {
                singleton = new TCPServer();
                return singleton;
            } catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
    }

    public TCPServer() throws Exception {
        String clientProblem;
        String passedString;
        ServerSocket welcomeSocket = new ServerSocket(8080);
        System.out.println("Waiting for connection on Port 8080");

        boolean running = true;

        while(running)
        {
            Socket connectionSocket = welcomeSocket.accept();
            System.out.println("Got connection on Port 8080");
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            clientProblem = inFromClient.readLine();
            System.out.println("Received: " + clientProblem);
            passedString = clientProblem.toUpperCase();
            //passedString = MainActivity.toSend;
            outToClient.writeBytes(passedString);
        }
    }

}