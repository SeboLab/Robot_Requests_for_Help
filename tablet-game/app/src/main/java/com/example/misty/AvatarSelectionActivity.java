package com.example.misty;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.text.TextUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
//TCP imports
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.Toast;

import com.example.misty.Socketconnection.TCPClient;
import com.example.misty.Socketconnection.TCPClientOwner;

import java.util.ArrayList;
import java.util.List;

public class AvatarSelectionActivity extends AppCompatActivity implements  TCPClient.OnMessageReceived {

    private TCPClient mTcpClient = TCPClient.getInstance();
    private String selectedDifficulty;
    private String selectedAvatar; // Variable to store the chosen avatar
    private boolean isRedAvatarSelected;

    private Button nextPageButton; // declare at class level
    private Button redAvatarButton;
    private Button blueAvatarButton;

    //keeps track of the currently selected button
    private Button currentlySelectedAvatarButton = null;

    private int defaultRedButtonColor;
    private int defaultBlueButtonColor;
    //private int highlightColor;



    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_selection);


        Log.d("AvatarSelectionActivity", "onCreate called - mTcpClient instance: " + mTcpClient.toString());
        mTcpClient.addMessageListener(this);


        selectedDifficulty = getIntent().getStringExtra("difficulty");


        TextView avatarText = findViewById(R.id.avatarText);
        avatarText.setText("Choose Your Avatar");

        defaultRedButtonColor = ContextCompat.getColor(this, android.R.color.holo_red_light);
        defaultBlueButtonColor = ContextCompat.getColor(this, android.R.color.holo_blue_light);
        //highlightColor = ContextCompat.getColor(this, R.color.bright_yellow);


        Button redAvatarButton = findViewById(R.id.redAvatarButton);


        Button blueAvatarButton = findViewById(R.id.blueAvatarButton);


        Button nextPageButton = findViewById(R.id.nextPageButton);

        // Set click listeners for the avatar buttons
        redAvatarButton.setOnClickListener(v -> {
            if(redAvatarButton.isPressed()){
                blueAvatarButton.setEnabled(false);
                redAvatarButton.setText("Selected");
                redAvatarButton.setEnabled(false);
                selectedAvatar = "Red";
                sendAvatarChoice();// Call method to send choice to TCPClient
                //handleAvatarSelection(blueAvatarButton, "Blue");
            }
        });


        blueAvatarButton.setOnClickListener(v -> {
            if(blueAvatarButton.isPressed()){
                redAvatarButton.setEnabled(false);
                blueAvatarButton.setText("Selected");
                blueAvatarButton.setEnabled(false);
                selectedAvatar = "Blue";
                sendAvatarChoice();// Call method to send choice to TCPClient
                //handleAvatarSelection(blueAvatarButton, "Blue");
            }
        });


        nextPageButton.setOnClickListener(v -> {
            if (selectedAvatar != null) {
                new Thread(() -> {
                    if (mTcpClient != null && mTcpClient.isConnected()) {
                        mTcpClient.sendMessage("Difficulty;" + selectedDifficulty + "\n");


                        // Safely switch to next activity **after** connection is done
                        runOnUiThread(() -> {
                            Intent intent = new Intent(AvatarSelectionActivity.this, GamePage.class);
                            intent.putExtra("avatar", selectedAvatar);
                            intent.putExtra("difficulty", selectedDifficulty);
                            startActivity(intent);
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show());
                    }
                }).start();
            }
        });
        // disable the next page button by doing false
        nextPageButton.setEnabled(false);
        // Set up a delay to allow sending before going to the next page.
        new Handler().postDelayed(() -> {
            nextPageButton.setEnabled(true);
        }, 10000);
        //redAvatarButton.setOnClickListener(v -> startGame("Red"));
        //blueAvatarButton.setOnClickListener(v -> startGame("Blue"));
    }

    private void sendAvatarChoice() {
        if (selectedAvatar != null) {
            //prepare list for TextUtils.Join
            List<String> buttonChoiceList = new ArrayList<>();
            buttonChoiceList.add(selectedAvatar);
            // Join the button choice into a string
            String buttonIdsString = TextUtils.join(",", buttonChoiceList) + ";";
            // message to send to python
            String message = "avatar;"+buttonIdsString;
            // Send the avatar choice via TCP
            Log.d("AvatarSelectionActivity", "sendAvatarChoice: sending " + message);
            sendMessageToPython(message);
            Log.d("AvatarSelectionActivity", "sendAvatarChoice: message sent" + message);
        }
        else {
            Log.e("AvatarSelectionActivity", "Connection not established yet!");
        }
    }
    public void sendMessageToPython(String message) {
        Log.d("AvatarSelectionActivity", "sendMessageToPython called, message: " + message);
        new SendMessageTask().execute(message);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTcpClient.removeMessageListener(this);
    }


    @Override
    public void messageReceived(String message) {
        runOnUiThread(() -> {
            // Update UI or perform actions based on the received message
            Log.d("AvatarSelectionActivity", "Message received: " + message);
            // Example: If the message contains "Start", change a TextView
            if (message.contains("Start")) {
                // TextView textView = findViewById(R.id.someTextView);
                // textView.setText("Game Started!");
            }
        });
    }


    //  @Override
    public void disableButtons() {
        // Implement button disabling logic here if needed
    }

    private class SendMessageTask extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPreExecute(){
            Log.d("SendMessageTask", "onPreExecute called");
        }
        protected Void doInBackground(String... messages) {
            Log.d("SendMessageTask", "doInBackground called");
            String message = messages[0];
            mTcpClient.sendMessage(message);
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            Log.d("SendMessageTask", "onPostExecute called");
        }
    }
}