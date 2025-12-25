package com.example.misty;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.misty.Socketconnection.TCPClient;
import com.example.misty.Socketconnection.TCPServer;

// importing libraries for setting up TCP/IP connection between the python and tablet
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class HomeActivity extends Activity implements View.OnClickListener, TCPClient.OnMessageReceived {
    private String selectedDifficulty = "Easy"; // Default selection
    private EditText iPandPort;
    private Button connectButton;
    private TextView connectionStatus;

    private boolean isConnected = false;

    private TCPClient mTcpClient = TCPClient.getInstance();
    private ConnectTask mConnectTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mTcpClient.addMessageListener(this);

        Spinner difficultySpinner = findViewById(R.id.difficultySpinner);
        String[] difficultyLevels = getResources().getStringArray(R.array.difficulty_levels);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, difficultyLevels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(adapter);

        difficultySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDifficulty = difficultyLevels[position]; // Store selected difficulty
                //Toast.makeText(HomeActivity.this, "Selected: " + selectedDifficulty, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> {
            new Thread(() -> {
                if (mTcpClient != null && mTcpClient.isConnected()) {
                    mTcpClient.sendMessage("Game Started");
                }
            }).start();
            // Go to Avatar Selection Page before starting the game
            Intent intent = new Intent(HomeActivity.this, AvatarSelectionActivity.class);
            intent.putExtra("difficulty", selectedDifficulty);
            startActivity(intent);
        });

        Button practiceButton = findViewById(R.id.practiceButton);
        practiceButton.setOnClickListener(v->{
            new Thread (() -> {
                if(mTcpClient != null && mTcpClient.isConnected()){
                    mTcpClient.sendMessage("Practice;started");
                }
            }).start();
            Intent intent = new Intent(HomeActivity.this, PracticeActivity.class);
            intent.putExtra("diFfficulty", selectedDifficulty);
            startActivity(intent);
        });

        // setting up the ip port address and connect button
        iPandPort = findViewById(R.id.IPandPort);
        iPandPort.setText("Enter IP address:8080"); //set default port asks for IP address
        connectButton = findViewById(R.id.ConnectButton);
        //connectionStatus = findViewById(R.id.ConnectionStatus);

        // listener on when the connect button is clicked
        connectButton.setOnClickListener(this);
    }


    public void connected() {
        // to display the connected status
        //connectionStatus.setText("Connected.");
        runOnUiThread(() -> {
            //Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show();
            connectButton.setText("Connected");
            connectButton.setEnabled(false); // disable button when connected
        });
    }

    public void connectTablet(View view) {

        //gets IP input from EditText and trims whitespace
        String ipInput = iPandPort.getText().toString().trim();

        if (ipInput.isEmpty()) {
            Toast.makeText(this, "Please enter IP address and port", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isValidInput = mTcpClient.setIpAndPort(ipInput);

        if (!isValidInput) {
            Toast.makeText(this, "Invalid IP address or port format.", Toast.LENGTH_LONG).show();
        }

        iPandPort.setText(mTcpClient.getIpAddress() + ":" + mTcpClient.getIpPortVar());
        //updates input field with validated/corrected IP and port

        //disable button show connecting status

        connectButton.setText("Connecting...");
        connectButton.setEnabled(false);

        //start connection task
        mConnectTask = new ConnectTask();
        mConnectTask.execute();
        //creates and executes background connection task


        //Toast.makeText(this, "trying to connect to server", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onClick(View v) {
        //onclick function to start the connect tablet function defined above
        if (v.getId() == R.id.ConnectButton) {
            connectTablet(v);
        }
    }

    private class ConnectTask extends AsyncTask<String, String, Boolean> {
        private boolean isConnectionSuccessful = false;

        @Override
        protected void onPreExecute() {
            Log.d("ConnectTask", "onPreExecute called");
        }
        @Override
        protected Boolean doInBackground(String... message) {
            Log.d("ConnectTask", "doInBackground called");
            mTcpClient.run();
            if (mTcpClient.isConnected()) {
                mTcpClient.sendMessage("Session started!");
                Log.d("ConnectTask", "session started message sent");
                isConnectionSuccessful= true;
            } else {
                isConnectionSuccessful= false;
            }
            return isConnectionSuccessful;
        }
        @Override
        protected void onPostExecute(Boolean isSuccessful) {
            super.onPostExecute(isSuccessful);
            if (isSuccessful) {
                // If connection is successful, update the button on the UI thread
                runOnUiThread(() -> {
                    connectButton.setText("Connected");
                    connectButton.setEnabled(false);
                    //Toast.makeText(HomeActivity.this,"Connected successfully!", Toast.LENGTH_SHORT).show();
                });
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mTcpClient.startListening();
                    }
                }).start();
            } else {
                runOnUiThread(() -> {
                    connectButton.setText("Connection failed");
                    connectButton.setEnabled(true);
                    Toast.makeText(HomeActivity.this, "Connection failed please check IP and try again!", Toast.LENGTH_LONG).show();
                });
            } Log.d("ConnectTask", "onPostExecute called");
        }
    }
    @Override
    public void messageReceived(String message){
        runOnUiThread(() -> {
            //update UI
            Log.e("HomeActivity", "Message received: " + message);
            //Toast.makeText(this,"Message " + message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //remove listener in on destroy
        mTcpClient.removeMessageListener(this);
        mTcpClient.disconnect();
        Log.d("HomeActivity", "Listener removed in onDestroy");
    }
}