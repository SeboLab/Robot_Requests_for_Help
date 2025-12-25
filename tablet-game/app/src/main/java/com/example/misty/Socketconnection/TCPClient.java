package com.example.misty.Socketconnection;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.misty.HomeActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class TCPClient {

    private String serverMessage;
    // Constants for default server IP and port
    public static final String SERVERIP = "192.168.1.103";
    public static final int SERVERPORT = 8080;

    private String ipAddress;
    private int ipPortVar;
    private PrintWriter bufferOut;
    private BufferedReader bufferIn;
    private Socket socket;
    private List<OnMessageReceived> messageListeners = new ArrayList<>(); // List of listeners

    private boolean isConnected = false;
    public static TCPClient singleton = null; // Singleton instance

    private TCPClient() {
        // Private constructor to enforce singleton pattern
        // Initialize with default values
        this.ipAddress = SERVERIP;
        this.ipPortVar = SERVERPORT;
    }
    public static TCPClient getInstance() {
        if (singleton == null)
            singleton = new TCPClient();
        return singleton;
    }
    //method to add listeners
    public void addMessageListener(OnMessageReceived listener) {
        messageListeners.add(listener);
    }
    //method to remove listeners
    public void removeMessageListener(OnMessageReceived listener) {
        messageListeners.remove(listener);
    }
    public interface OnMessageReceived {
        void messageReceived(String message);
    }
    private void sendMessageToListeners(String message) {
        for (OnMessageReceived listener : messageListeners) {
            listener.messageReceived(message);
        }
    }

    public boolean setIpAndPort(String ipAndPort) {
        //set default ip and port if empty
        if (ipAndPort == null || ipAndPort.trim().isEmpty()) {
            this.ipAddress = SERVERIP;
            this.ipPortVar = SERVERPORT;
            return true;
        }
        try {
            //format ip:port
            //parses port as int
            String[] parts = ipAndPort.trim().split(":");
            if (parts.length == 2) {
                String ip = parts[0].trim();
                int port = Integer.parseInt(parts[1].trim());

                //checks if port is valid range (1-65535)
                //sets value if true
                if (isValidIP(ip) && port > 0 && port <= 65535) {
                    this.ipAddress = ip;
                    this.ipPortVar = port;
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            Log.e("TCPClient", "Invalid port number format" + e);
        }
        this.ipAddress = SERVERIP;
        this.ipPortVar = SERVERPORT;
        return false;
    }

    private boolean isValidIP(String ip){
        if(ip == null || ip.isEmpty()) return false;
        String[] parts = ip.split("\\.");
        if(parts.length != 4) return false;
        //validates ip address format
        try{
            for(String part:parts){
                int num = Integer.parseInt(part);
                if(num < 0 || num >255) return false;
            }
            return true;
        }catch(NumberFormatException e){
            return false;
        }
        //check each part of ip each part must be number between 0- 225
    }

    //getter methods
    public String getIpAddress(){
        return ipAddress;
    }

    public int getIpPortVar(){
        return ipPortVar;
    }

    public void sendMessage(String message) {
        Log.d("TCPClient", "sendMessage called"); // Log that this method is being reached.
        if (socket == null) {
            Log.e("TCPClient", "Socket is null!");
        } else {
            Log.d("TCPClient", "Socket is not null!");
        }
        if (bufferOut == null) {
            Log.e("TCPClient", "BufferOut is null!");
        } else {
            Log.d("TCPClient", "BufferOut is not null!");
        }
        if (bufferOut != null && !bufferOut.checkError()) {
            bufferOut.println(message);
            bufferOut.flush();
            Log.e("TCPClient", "Sent Message: " + message);
        } else {
            Log.e("TCPClient", "Error: Could not send message - Output stream is null or in error state.");
        }
    }

    public void run() {
        Log.d("TCPClient", "C: Attempting to connect with IP: " + ipAddress + " and Port:" + ipPortVar);

        try {
            isConnected = false;

            InetAddress serverAddr = InetAddress.getByName(ipAddress);
            socket = new Socket(serverAddr, ipPortVar);
            Log.d("TCPClient", "C: Connected.");
            isConnected = true;

            // create the buffers
            bufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            Log.d("TCPClient", "bufferOut created");
            bufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Log.d("TCPClient", "bufferIn created");
            Log.e("TCPClient", "C: Session Started!");
            //creates output stream for sending data
            //Creates input stream for receiving data
        } catch (Exception e) {
            Log.e("TCPClient", "C: Error", e);
            Log.e("TCPClient", "C: Could not Connect.");
            isConnected = false;
        }
    }
    public void startListening() {
        String serverMessage;
        try {
            //keep listening for message while there is not an error.
            while ( isConnected && socket != null && !socket.isClosed()) {
                Log.e("TCPClient", "C: Waiting for message...");
                serverMessage = bufferIn.readLine();
                //method to continuously listen for incoming messages
                //loops while connected and socket is valid
                //reads one line at a time from the input stream
                if (serverMessage!=null){
                    //send the message to the listeners
                    if (!serverMessage.isEmpty()) {
                        sendMessageToListeners(serverMessage);
                        Log.e("TCPClient", "C: Received Message - " + serverMessage);
                    }
                }else{
                    break;
                }
                //if message is received and not empty, notifies all listeners
                //logs the received message
                //breaks the loop if null is received (connection closed)
            }
        } catch (Exception e) {
            Log.e("TCPClient", "C: Error Listening", e);
        } finally {
            disconnect();
        }
    }
    //disconnect method
    public void disconnect(){
        isConnected = false;

        try{
            if(bufferOut != null){
                bufferOut.close();
                bufferOut = null;
            }
        }catch(Exception e){
            Log.e("TCPClient", " error closing output stream" + e);
        }

        try{
            if(bufferIn != null){
                bufferIn.close();
                bufferIn = null;
            }
        }catch(Exception e){
            Log.e("TCPClient", " error closing input stream" + e);
        }
        try{
            if(socket != null){
                socket.close();
                socket = null;
            }
        }catch(Exception e){
            Log.e("TCPClient", " error closing socket stream" + e);
        }
        Log.e("TCPClient","C: Connection closed.");
    }
    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }
    // returns true only if all conditions are met:
    //isConnected is true, socket object exists, socket is not closed
}