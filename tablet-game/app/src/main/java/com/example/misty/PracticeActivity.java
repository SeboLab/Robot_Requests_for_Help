package com.example.misty;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.misty.Socketconnection.TCPClient;
import com.example.misty.Socketconnection.TCPClientOwner;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;

public class PracticeActivity extends AppCompatActivity implements TCPClient.OnMessageReceived {
    private static final int ROWS = 3;
    private static final int COLUMNS = 3;

    private Button[][] leftButtons = new Button[ROWS][COLUMNS];
    private char[][] leftNumbers = new char[ROWS][COLUMNS];
    private boolean[][] leftRevealed = new boolean[ROWS][COLUMNS];

    private Button[][] rightButtons = new Button[ROWS][COLUMNS];
    private char[][] rightNumbers = new char[ROWS][COLUMNS];
    private boolean[][] rightRevealed = new boolean[ROWS][COLUMNS];

    private Board leftGame = new Board(ROWS, COLUMNS);
    private Board rightGame = new Board(ROWS, COLUMNS);

    private LinearLayout leftRowNumbers, leftColumnNumbers, rightRowNumbers, rightColumnNumbers;


    private TCPClient mTcpClient = TCPClient.getInstance();
    private Handler delayHandler;
    private boolean mistyTurnOver = true;
    private boolean mistySpeaking = false;

    private int specifiedRow = -1;
    private int specifiedCol = -1;

    private TextView leftGoldCountText;
    private TextView rightGoldCountText;
    private TextView turnIndicatorText;

    private int leftGoldCount = 0;
    private int rightGoldCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("PracticePage", "onCreate called - mTcpClient instance: " + mTcpClient.toString());
        mTcpClient.addMessageListener(this);

        if (delayHandler == null) { // Check if it's already initialized
            delayHandler = new Handler(Looper.getMainLooper());
        }
        setContentView(R.layout.game_board);

        turnIndicatorText = findViewById(R.id.turnIndicatorText);

        leftGoldCountText = findViewById(R.id.leftGoldCountText);
        rightGoldCountText = findViewById(R.id.rightGoldCountText);

        leftRowNumbers = findViewById(R.id.leftRowNumbers);
        leftColumnNumbers = findViewById(R.id.leftColumnNumbers);
        rightRowNumbers = findViewById(R.id.rightRowNumbers);
        rightColumnNumbers = findViewById(R.id.rightColumnNumbers);

        GridLayout leftGridLayout = findViewById(R.id.leftBoard);
        GridLayout rightGridLayout = findViewById(R.id.rightBoard);

        leftGridLayout.setRowCount(ROWS);
        leftGridLayout.setColumnCount(COLUMNS);
        rightGridLayout.setRowCount(ROWS);
        rightGridLayout.setColumnCount(COLUMNS);

        generateShuffledNumbers(leftGame, leftNumbers, leftRevealed);
        generateShuffledNumbers(rightGame, rightNumbers, rightRevealed);

        initializeBoard(leftGridLayout, leftButtons, leftNumbers, leftRevealed, false);
        initializeBoard(rightGridLayout, rightButtons, rightNumbers, rightRevealed, true);

        setupGridLabels();

        Button backHomeButton = findViewById(R.id.backHomeButton);
        backHomeButton.setOnClickListener(v -> {
            sendHomeButtonClick();
            backHomeButton.setEnabled(false);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(PracticeActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, 5000);
        });
        showTurnMessage("      Blue Avatar's turn",3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTcpClient.removeMessageListener(this);
        // It's good practice to remove any pending callbacks from the handler
        // when the activity is destroyed to prevent memory leaks or unwanted execution.
        if (delayHandler != null) {
            delayHandler.removeCallbacksAndMessages(null);
        }
    }

    private void generateShuffledNumbers(Board board, char[][] numbers, boolean[][] revealed) {
        List<Character> values = new ArrayList<>();
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {
                values.add(board.passSquare(i, j));
            }
        }
        int index = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                numbers[row][col] = values.get(index++);
                revealed[row][col] = false;
            }
        }
    }

    private void initializeBoard(GridLayout gridLayout, Button[][] buttons, char[][] numbers, boolean[][] revealed, boolean isRightBoard) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        int maxBoardWidth = (int) (screenWidth * 0.9); // 90% of screen width
        int maxBoardHeight = (int) (screenHeight * 0.6); // 60% of screen height

// Adjust button size based on rows/columns, ensuring it fits within limits
        int buttonSize = Math.min(maxBoardWidth / (COLUMNS + 1), maxBoardHeight / (ROWS + 2));

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                final int r = row, c = col;
                buttons[row][col] = new Button(this);
                //buttons[row][col].setScaleType(ImageView.ScaleType.CENTER_CROP);
                //buttons[row][col].setAdjustViewBounds(true);
                buttons[row][col].setText("");
                buttons[row][col].setTextSize(20);
                buttons[row][col].setTypeface(null, Typeface.BOLD);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col);
                params.width = buttonSize;
                params.height = buttonSize;
                params.setMargins(1, 1, 1, 1);
                buttons[row][col].setLayoutParams(params);

                buttons[row][col].setBackgroundResource(R.drawable.dirt);

                if (isRightBoard) {
                    buttons[row][col].setOnClickListener(v -> buttonClick(r, c));
                } else {
                    buttons[row][col].setEnabled(false);
                }

                gridLayout.addView(buttons[row][col]);
            }
        }
    }

    private void setupGridLabels() {
        // Clear existing row and column labels to avoid duplication
        leftRowNumbers.removeAllViews();
        leftColumnNumbers.removeAllViews();
        rightRowNumbers.removeAllViews();
        rightColumnNumbers.removeAllViews();

        // Adjust padding dynamically based on difficulty
        int columnPadding, rowPadding;
        if (ROWS == 7) { // Hard mode (7x7)
            columnPadding = 40; // Reduce column padding
            rowPadding = 30;    // Reduce row padding
        } else if (ROWS == 5) { // Medium mode (5x5)
            columnPadding = 60;
            rowPadding = 40;
        } else { // Easy mode (3x3)
            columnPadding = 60;
            rowPadding = 50;
        }

        // Add column numbers
        for (int i = 0; i < COLUMNS; i++) {
            TextView colLabelLeft = new TextView(this);
            colLabelLeft.setText(String.valueOf(i + 1));
            colLabelLeft.setGravity(Gravity.CENTER);
            colLabelLeft.setTextColor(Color.WHITE);
            colLabelLeft.setTextSize(22); // Reduce text size slightly
            colLabelLeft.setPadding(columnPadding, 10, columnPadding, 10);
// Set wider layout params
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            labelParams.width = 190; // adjust as needed
            colLabelLeft.setLayoutParams(labelParams);

// Translate only after layout is handled
            if (i == 0) {
                colLabelLeft.setTranslationX(90); // shift right
            } else if (i == 1) {
                colLabelLeft.setTranslationX(30); // shift more
            }else if(i == 2){
                colLabelLeft.setTranslationX(-20);
            }

            leftColumnNumbers.addView(colLabelLeft);

            TextView colLabelRight = new TextView(this);
            colLabelRight.setText(String.valueOf(i + 1));
            colLabelRight.setTextSize(22f);
            colLabelRight.setTextColor(Color.WHITE);
            colLabelRight.setGravity(Gravity.CENTER);
            colLabelRight.setPadding(columnPadding, 10, columnPadding, 10);
            // Set wider layout params
            labelParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            labelParams.width = 190; // adjust as needed
            colLabelRight.setLayoutParams(labelParams);
            colLabelRight.setLayoutParams(labelParams);

// Translate only after layout is handled
            if (i == 0) {
                colLabelRight.setTranslationX(90); // shift right
            } else if (i == 1) {
                colLabelRight.setTranslationX(30); // shift more
            }else if(i == 2){
                colLabelRight.setTranslationX(-20);
            }
            rightColumnNumbers.addView(colLabelRight);
        }
        // Add row letters: S, T, V (instead of S, T, U)
        char[] rowLetters = {'D', 'J', 'Z'};
        for (int i = 0; i < ROWS; i++) {
            TextView rowLabelLeft = new TextView(this);
            rowLabelLeft.setText(String.valueOf(rowLetters[i]));
            rowLabelLeft.setTextSize(25f);
            rowLabelLeft.setTextColor(Color.WHITE);

            rowPadding = (ROWS == 3) ? 45 : (ROWS == 5) ? 40 : 20;
            rowLabelLeft.setPadding(50, rowPadding, 10, rowPadding);
            leftRowNumbers.addView(rowLabelLeft);

            TextView rowLabelRight = new TextView(this);
            rowLabelRight.setText(String.valueOf(rowLetters[i]));
            rowLabelRight.setTextSize(25f);
            rowLabelRight.setTextColor(Color.WHITE);
            rowLabelRight.setPadding(50, rowPadding, 10, rowPadding);
            rightRowNumbers.addView(rowLabelRight);
        }

    }

    private void buttonClick(int row, int column) {
        Log.d("PracticeActivity", "button click called misty turnover "+mistyTurnOver + ", mistyspeaking: " + mistySpeaking );

        if (!mistyTurnOver || mistySpeaking) {
            Log.d("PracticeActivity", "Blocked: Misty is playing or speaking.");
            //Toast.makeText(this, "Wait for Misty to finish!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rightRevealed[row][column]) {
            Log.d("PracticeActivity", "Tile already revealed at row " + row + "col," + column);
            return;
        }
        Log.d("PracticeActivity", "Player making move at row" + row + "col " + column);

        char v = flipButton(row, column, true); //player's turn
        //we check that v was not equal to a, since a is returned if the button has already been clicked.
        if (v != 'a') {
            //set misty turn state
            mistyTurnOver = false; // Misty's turn now
            mistySpeaking = false;
            disableUserBoard();

            sendCOutcomeOverTCP(v); // Send the value over TCP
            Log.d("PracticeActivity", "userboard disabled after player's move.");
        }
    }

    private char flipButton(int row, int col, boolean isRightBoard) {
        Button[][] buttons = isRightBoard ? rightButtons : leftButtons;
        char[][] numbers = isRightBoard ? rightNumbers : leftNumbers;
        boolean[][] revealed = isRightBoard ? rightRevealed : leftRevealed;

        if (!revealed[row][col]) {
            //buttons[row][col].setText(String.valueOf(numbers[row][col]));
            revealed[row][col] = true;


            runOnUiThread(() -> {
                if (numbers[row][col] == 'G') {
                    if (isRightBoard) {
                        rightGoldCount++;
                        if (rightGoldCountText != null) {
                            rightGoldCountText.setText(" " + rightGoldCount);
                        }

                    } else {
                        leftGoldCount++;
                        if (leftGoldCountText != null) {
                            leftGoldCountText.setText(" " + leftGoldCount);
                        }

                    }
                    buttons[row][col].setBackgroundResource(R.drawable.gold); // this will show gold image
                    new Handler(Looper.getMainLooper()).postDelayed(() -> resetBoard(isRightBoard), 10000);
                    // mistyTurnOver = true;
                } else if (numbers[row][col] == 'B') {
                    buttons[row][col].setBackgroundResource(R.drawable.bomb); // this will show bomb image
                    new Handler(Looper.getMainLooper()).postDelayed(() -> resetBoard(isRightBoard), 10000);
                    //mistyTurnOver = true;
                } else {
                    int num = Character.getNumericValue(numbers[row][col]);

                    int resID = getResources().getIdentifier("dirt" + num, "drawable",getPackageName());

                    if(resID != 0){
                        buttons[row][col].setBackgroundResource(resID);
                        buttons[row][col].setText("");
                    }else{
                        buttons[row][col].setText(num); // this will show the number of squares away from the bomb
                        buttons[row][col].setTextColor(Color.BLACK);
                        buttons[row][col].setBackgroundColor(Color.parseColor("#c58e61"));
                    }
                }
            });

            return numbers[row][col];
        }
        return 'a';
    }

    public void mistyTurn() {
        Log.d("PracticeActivity", "mistyTurn() called mistySpeaking: " + mistySpeaking + "mistyTurnOver:" + mistyTurnOver);
        // Log.e("PracticeActivity", "mistyTurn: Called");
        if(mistySpeaking){
            Log.d("PracticeActivity", "mistyTurn() blocked because misty is speaking");
            return;
        }
        if (specifiedRow != -1 && specifiedCol != -1) {
            Log.d("PracticeActivity", "Misty started playing: row" + specifiedRow + ", col " + specifiedCol);
            // Use the specified row and column
            mistyTurnOver = false;
            mistySpeaking = false;
            disableUserBoard();
            flipSpecifiedSquare(specifiedRow, specifiedCol);

            // Reset the specified row and column
            specifiedRow = -1;
            specifiedCol = -1;
            mistyTurnOver = true;

        } else {
            Log.d("PracticeActivity", "mistyTurn() called but no valid coordinates specified");
        }
    }

    private void flipSpecifiedSquare(int row, int col) {
        System.out.println("Misty is playing by specified move...");

        if (row < 0 || row >= ROWS || col < 0 || col >= COLUMNS) {
            Log.e("PracticeActivity", "invalud coordinates: row" + row + ", col= " + col);
            showTurnMessage("      Blue Avatar's Turn!",3000);
            mistySpeaking = false;
            mistyTurnOver = true;
            enableUserBoard();
            return;
        }
        if (leftRevealed[row][col]) {
            Log.d("PracticeActivity", "Tile already revealed at row" + row + ", col= " + col);
            showTurnMessage("      Blue Avatar's Turn!!",3000);
            mistySpeaking = false;
            mistyTurnOver = false;
            enableUserBoard();
            return;
        }

        Log.d("PracticeActivity", " Misty flipping tile at row" + row + ",col " + col);

        char mv = flipButton(row, col, false); // Misty plays on left board

        //we check that v was not equal to a, since a is returned if the button has already been clicked.
        if (mv != 'a') {
            mistySpeaking = true;
            mistyTurnOver = false;

            sendMOutcomeOverTCP(mv); // sends result to python

            if (delayHandler != null) {
                delayHandler.postDelayed(() -> {
                    Log.d("PracticeActivity", "misty finished turn delay");
                    showTurnMessage("      Blue Avatar's Turn!!",3000);
                    mistySpeaking = false;
                    mistyTurnOver = true;
                    enableUserBoard();
                    Log.d("PracticeActivity", "Misty finished speaking. Player turn resumed.");
                }, 10000);
            }
        } else {
            Log.e("PracticeActivity", "flipButton returned a tile may already be revealed");
            showTurnMessage("      Blue Avatar's Turn!!",3000);
            //if move invalid reset mity's turn state
            mistySpeaking = false;
            mistyTurnOver = true;
            enableUserBoard();
        }
    }

    private void sendPracticeMessage(String msg) {
        if (TCPClient.singleton != null) {
            new Thread(() -> {
                Log.d("PracticeActivity", "Sending: " + msg);
                TCPClient.singleton.sendMessage(msg);
            }).start();
        }
    }

    private void sendHomeButtonClick() {
        String messageToSend = "Homebutton; true";
        Log.d("PracticeActivity", "Sending: " + messageToSend);
        sendMessageToPython(messageToSend);
    }

    private void resetGame() {
        Intent intent = new Intent(PracticeActivity.this, PracticeActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void messageReceived(String message) {
        runOnUiThread(() -> {
            Log.d("PracticeActivity", "message received: " + message);
            String[] parts = message.split(";");
            Log.d("PracticeActivity", "messageReceived: parts length: " + parts.length);

            if (parts.length >= 2) {
                String type = parts[0].trim();
                Log.d("PracticeActivity", "Message type: " + type);
                if (type.equals("Mistychoice")) {
                    String coordinates = parts[1].trim();
                    Log.d("PracticeActivity", "messageReceived: coordinates: " + coordinates);

                    if (!coordinates.isEmpty() && coordinates.length() >= 2) {
                        try {
                            char rowChar = coordinates.charAt(0);
                            String colStr = coordinates.substring(1);

                            if(rowChar == 'D'){
                                specifiedRow = 0;
                            }else if(rowChar == 'J'){
                                specifiedRow = 1;
                            }else if(rowChar == 'Z'){
                                specifiedRow = 2;
                            }
                            specifiedCol = Integer.parseInt(colStr) - 1;

                            Log.d("PracticeActivity", "Parsed to row: " + specifiedRow + ", col: " + specifiedCol);

                            mistyTurn();
                        } catch (NumberFormatException | IndexOutOfBoundsException e) {
                            Log.e("PracticeActivity", "Error parsing coordinate: " + coordinates, e);
                        }
                    }
                }
            }
        });
    }


    private class SendMessageTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... messages) {
            String message = messages[0];
            // Call the method to send the message through the TCPClient
            mTcpClient.sendMessage(message);
            return null;
        }
    }

    public void sendMessageToPython(String message) {
        new SendMessageTask().execute(message);
    }

    private void performTimedAction() {
        runOnUiThread(() -> Toast.makeText(PracticeActivity.this, "", Toast.LENGTH_SHORT).show());
    }

    private void sendMOutcomeOverTCP(char mv) {
        // Prepare the message string
        String message = "Moutcome;" + mv + ";";

        // Send the message using the existing sendMessageToPython method
        System.out.println("message being sent: " + message);
        sendMessageToPython(message);

        System.out.println(message);
    }

    private void sendCOutcomeOverTCP(char v) {
        if (mTcpClient != null) {
            // Prepare the message string
            String messageToSend = "Coutcome;" + v + ";";

            sendMessageToPython(messageToSend);
            Log.i("PracticePage", "message being sent: " + messageToSend);

            final String mistyTurnMessage = "Mistyturn;" + "started";
            long delayMillis = 2500; // 2.5 seconds

            delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendMessageToPython(mistyTurnMessage);
                    Log.i("PracticeActivity", "message being sent: " + mistyTurnMessage);
                }
            }, delayMillis);
        }
    }
    private void resetBoard(boolean isRightBoard) {
        if (isRightBoard) {
            rightGame = new Board(ROWS, COLUMNS);
            generateShuffledNumbers(rightGame, rightNumbers, rightRevealed);

            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    rightButtons[row][col].setText("");
                    rightButtons[row][col].setBackgroundResource(R.drawable.dirt);
                    rightRevealed[row][col] = false;
                }
            }
            //only enable board if player's turn and misty isn't speaking
            if (mistyTurnOver && !mistySpeaking) {
                showTurnMessage("      Red Avatar's turn!",3000);
                enableUserBoard();
            }
            //if misty active board stays disabled until her turn ends
        } else {
            leftGame = new Board(ROWS, COLUMNS);
            generateShuffledNumbers(leftGame, leftNumbers, leftRevealed);

            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    leftButtons[row][col].setText("");
                    leftButtons[row][col].setBackgroundResource(R.drawable.dirt);
                    leftRevealed[row][col] = false;
                }
            }
        }
    }
    //loop through all buttons on right board
    //disables each button so player cannot click any tiles
    //used right after player makes a move during misty's turn
    private void disableUserBoard() {
        Log.d("PracticeActivity", "disabling user board");
        runOnUiThread(() -> {
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    rightButtons[row][col].setEnabled(false);
                }
            }
        });
    }
    //loops through all buttons on right board
    //enables only unrevealed tiles
    //makes sure players can click only hidden tiles
    //used after misty finishes her turn and speaking to give control back to player
    private void enableUserBoard() {
        Log.d("PracticeActivity", "enabling user board");
        runOnUiThread(() -> {
            if (mistyTurnOver && !mistySpeaking) {
                for (int row = 0; row < ROWS; row++) {
                    for (int col = 0; col < COLUMNS; col++) {
                        if (!rightRevealed[row][col]) {
                            rightButtons[row][col].setEnabled(true);
                        }
                    }
                }
                Log.d("PracticeActivity", "userboard enabled player can move");
            } else {
                Log.d("PracticeActivity", "Userboard not enabled misty turn over" + mistyTurnOver + " mistySpeaking " + mistySpeaking);
            }
        });
    }
    @SuppressLint("ResourceType")
    private void showTurnMessage(String message, int delayTime) {
        runOnUiThread(() -> {
            turnIndicatorText.setText(message);
            turnIndicatorText.setVisibility(View.VISIBLE);
            turnIndicatorText.bringToFront();
            ImageView rightAvatarImage1 = findViewById(R.id.rightAvatarImage1);
            ImageView leftAvatarImage1 = findViewById(R.id.leftAvatarImage1);

            rightAvatarImage1.setTranslationX(-270);
            rightAvatarImage1.setTranslationY(425);
            rightAvatarImage1.setImageResource(R.drawable.water2);

            leftAvatarImage1.setTranslationX(-270);
            leftAvatarImage1.setTranslationY(425);
            leftAvatarImage1.setImageResource(R.drawable.fire2);

            if (message.toLowerCase().contains("blue")) {
                rightAvatarImage1.setVisibility(View.VISIBLE);
                leftAvatarImage1.setVisibility(View.GONE);
                rightAvatarImage1.bringToFront();
            } else {
                leftAvatarImage1.setVisibility(View.VISIBLE);
                rightAvatarImage1.setVisibility(View.GONE);
                leftAvatarImage1.bringToFront();
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                turnIndicatorText.setVisibility(View.GONE);
                rightAvatarImage1.setVisibility(View.GONE);
                leftAvatarImage1.setVisibility(View.GONE);
            }, delayTime);
        });
    }
}