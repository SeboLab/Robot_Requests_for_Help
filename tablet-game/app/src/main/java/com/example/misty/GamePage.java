package com.example.misty;

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
import com.example.misty.GameTimer;

import androidx.appcompat.app.AppCompatActivity;

import com.example.misty.Socketconnection.TCPClient;
import com.example.misty.Socketconnection.TCPClientOwner;
// after misty's turn message is too slow
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class GamePage extends AppCompatActivity implements TCPClient.OnMessageReceived {
    private static int ROWS;
    private static int COLUMNS;

    private Button[][] leftButtons;
    private char[][] leftNumbers;
    private boolean[][] leftRevealed;

    private Button[][] rightButtons;
    private char[][] rightNumbers;
    private boolean[][] rightRevealed;

    private Board leftGame;
    private Board rightGame;
    private boolean interrupt = false;

    //variables for the left and right row and column numbers
    private LinearLayout leftRowNumbers, leftColumnNumbers, rightRowNumbers, rightColumnNumbers;
    private TextView leftGoldCountText;
    private TextView rightGoldCountText;
    private int leftGoldCount = 0;
    private int rightGoldCount = 0;

    //variable for TCP connection, outcome
    // private String choiceOutcome;

    private TCPClient mTcpClient = TCPClient.getInstance();
    private Handler delayHandler;
    private TextView turnIndicatorText;
    private boolean mistyTurnOver = true;
    private boolean mistySpeaking = false;
    private int specifiedRow = -1;
    private int specifiedCol = -1;
    private String playerAvatarColor = "Red";
    private String mistyAvatarColor = "Blue";
    private boolean gameOver = false;


    private boolean hasTimerStarted = false;
    private boolean timerExpired = false;
    private int buttonSize = 0;
    String difficulty;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("GamePage", "onCreate called - mTcpClient instance: " + mTcpClient.toString());
        mTcpClient.addMessageListener(this);

        if (delayHandler == null) { // Check if it's already initialized
            delayHandler = new Handler(Looper.getMainLooper());
        }
        setContentView(R.layout.game_board);

        TextView textViewMode = findViewById(R.id.textViewMode);
        difficulty = getIntent().getStringExtra("difficulty");
        if (difficulty == null) difficulty = "Easy"; // Default value if null
        textViewMode.setText("");
        leftGoldCountText = findViewById(R.id.leftGoldCountText);
        rightGoldCountText = findViewById(R.id.rightGoldCountText);

        turnIndicatorText = findViewById(R.id.turnIndicatorText);
        //put avatar method HERE
        avatarChosen();
        //calling from the xml file with the linear layout
        leftRowNumbers = findViewById(R.id.leftRowNumbers);
        leftColumnNumbers = findViewById(R.id.leftColumnNumbers);
        rightRowNumbers = findViewById(R.id.rightRowNumbers);
        rightColumnNumbers = findViewById(R.id.rightColumnNumbers);


        // Set grid size
        switch (difficulty) {
            case "Medium":
                ROWS = 5;
                COLUMNS = 5;
                break;
            case "Hard":
                ROWS = 7;
                COLUMNS = 7;
                break;
            default:
                ROWS = 3;
                COLUMNS = 3;
                break;
        }


        leftButtons = new Button[ROWS][COLUMNS];
        leftNumbers = new char[ROWS][COLUMNS];
        leftRevealed = new boolean[ROWS][COLUMNS];

        rightButtons = new Button[ROWS][COLUMNS];
        rightNumbers = new char[ROWS][COLUMNS];
        rightRevealed = new boolean[ROWS][COLUMNS];

        leftGame = new Board(ROWS, COLUMNS);
        rightGame = new Board(ROWS, COLUMNS);

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
        //the method in which we can set the row and column numbers
        setupGrid();

        int shiftAmount = 0; // default

        switch (difficulty) {
            case "Easy":
                shiftAmount = 55; // shift more
                break;
            case "Medium":
                shiftAmount = 40; // shift a little
                break;
            case "Hard":
                shiftAmount = 40; // no shift
                break;
        }
// Shift the right board slightly to the right
        rightColumnNumbers.setTranslationX(shiftAmount);
        leftColumnNumbers.setTranslationX(shiftAmount);

        Button backHomeButton = findViewById(R.id.backHomeButton);
        backHomeButton.setOnClickListener(v -> {
            sendHomeButtonClick();
            backHomeButton.setEnabled(false);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(GamePage.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, 5000);
        });
        showTurnMessage(true,3000);
    }

    @Override
    public void messageReceived(String message) {
        runOnUiThread(() -> {
            Log.d("GamePage", "message received: " + message);
            String[] parts = message.split(";");
            Log.d("GamePage", "messageReceived: parts length: " + parts.length);

            if (parts.length >= 2) {
                String type = parts[0].trim();
                Log.d("GamePage", "Message type: " + type);

                if (type.equals("GameFinished")) { // message from python: GameFinished;true;
                    Log.d("GamePage", "Game finished signal received.");
                    new Handler(Looper.getMainLooper()).postDelayed(this::autoClickBackHomeButton, 5000); // 5000 ms = 5 seconds delay
                    return;
                }

                if (type.equals("Mistychoice")) {
                    String coordinates = parts[1].trim();
                    Log.d("GamePage", "messageReceived: coordinates: " + coordinates);

                    if (!coordinates.isEmpty() && coordinates.length() >= 2) {
                        try {
                            char rowChar = coordinates.charAt(0);
                            String colStr = coordinates.substring(1);

                            if (difficulty.equals("Easy")) {
                                if (rowChar == 'D') specifiedRow = 0;
                                else if (rowChar == 'J') specifiedRow = 1;
                                else if (rowChar == 'Z') specifiedRow = 2;
                            } else {
                                if (rowChar == 'S') specifiedRow = 0;
                                else if (rowChar == 'T') specifiedRow = 1;
                                else if (rowChar == 'V') specifiedRow = 2;
                                else if (rowChar == 'W') specifiedRow = 3;
                                else if (rowChar == 'X') specifiedRow = 4;
                                else if (rowChar == 'Y') specifiedRow = 5;
                                else if (rowChar == 'Z') specifiedRow = 6;
                            }

                            specifiedCol = Integer.parseInt(colStr) - 1;

                            Log.d("GamePage", "Parsed to row: " + specifiedRow + ", col: " + specifiedCol);
                            Log.d("GamePage", "current turn state mistyturnover:" + mistyTurnOver + ", mistyspeaking" + mistySpeaking);

                            mistyTurn();
                        } catch (NumberFormatException | IndexOutOfBoundsException e) {
                            Log.e("GamePage", "Error parsing coordinate: " + coordinates, e);
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
        runOnUiThread(() -> Toast.makeText(GamePage.this, "", Toast.LENGTH_SHORT).show());
    }

    public void mistyTurn() {
        Log.d("GamePage", "mistyTurn() called mistySpeaking: " + mistySpeaking + "mistyTurnOver:" + mistyTurnOver);

        if (mistySpeaking) {
            Log.d("GamePage", "mistyTurn() blocked because misty is speaking");
            return;
        }

        // Log.e("GamePage", "mistyTurn: Called");
        if (specifiedRow != -1 && specifiedCol != -1) {
            Log.d("GamePage", "Misty started playing: row" + specifiedRow + ", col " + specifiedCol);
            // Use the specified row and column

            mistyTurnOver = false;
            mistySpeaking = false;
            disableUserBoard();

            flipSpecifiedSquare(specifiedRow, specifiedCol);

            // Reset the specified row and column
            specifiedRow = -1;
            specifiedCol = -1;
        } else {
            Log.d("Gamepage", "mistyTurn() called but no valid coordinates specified");
        }
    }

    private void flipSpecifiedSquare(int row, int col) {
        System.out.println("Misty is playing by specified move...");

        if (row < 0 || row >= ROWS || col < 0 || col >= COLUMNS) {
            Log.e("Gamepage", "invalid coordinates: row" + row + ", col= " + col);
            showTurnMessage(true,3000);
            mistySpeaking = false;
            mistyTurnOver = true;
            enableUserBoard();
            return;
        }
        if (leftRevealed[row][col]) {
            Log.d("Gamepage", "Tile already revealed at row" + row + ", col= " + col);
            showTurnMessage(true,3000);
            mistySpeaking = false;
            mistyTurnOver = false;
            enableUserBoard();
            return;
        }

        Log.d("GamePage", " Misty flipping tile at row" + row + ",col " + col);
        Log.d("DEBUG", "Trying to flip square at row=" + row + ", col=" + col);

        char mv = flipButton(row, col, false); // Misty plays on right board

        //we check that v was not equal to a, since a is returned if the button has already been clicked.
        if (mv != 'a') {
            mistySpeaking = true;
            mistyTurnOver = false;

            sendMOutcomeOverTCP(mv); // sends result to python

            if (delayHandler != null) {
                delayHandler.postDelayed(() -> {
                    Log.d("GamePage", "misty finished turn delay");
                    showTurnMessage(true,3000);
                    mistySpeaking = false;
                    mistyTurnOver = true;
                    enableUserBoard();
                    Log.d("GamePage", "Misty finished speaking. Player turn resumed.");
                }, 12000);
            }
        } else {
            Log.e("GamePage", "flipButton returned a tile may already be revealed");
            showTurnMessage(true,3000);
            //if move invalid reset mity's turn state
            mistySpeaking = false;
            mistyTurnOver = true;
            enableUserBoard();
        }
    }

    private void flipRandomSquare() {
        // Log.e("GamePage", "flipRandomSquare: Called");
        System.out.println("Misty is playing...");

        Random random = new Random();
        int randRow, randCol;

        do {
            Solver solver = new Solver(ROWS, COLUMNS, rightNumbers, rightRevealed);
            int[] move = solver.getMove(getIntent().getStringExtra("difficulty"));

            randRow = move[0];
            randCol = move[1];
            //    Log.e("GamePage", "flipRandomSquare: Checking row: " + randRow + " and column: " + randCol);
        } while (rightRevealed[randRow][randCol]);

        char mv = flipButton(randRow, randCol, false); // Misty plays on right board
        //flipButton(randRow, randCol, false); // Misty plays on right board

        //  int finalRandRow = randRow;
        //  int finalRandCol = randCol;
        //we check that v was not equal to a, since a is returned if the button has already been clicked.
        if (mv != 'a') {
            long delayMillis = 2000; // 2 seconds
            sendMOutcomeOverTCP(mv);
        }

    }

    private void sendMOutcomeOverTCPWithDelay(final char mv, long delayMillis) {
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // This code will run after the specified delay
                sendMOutcomeOverTCP(mv);
            }
        }, delayMillis);
    }

    private void sendMOutcomeOverTCP(char mv) {
        // Prepare the message string
        String message = "Moutcome;" + mv + ";";

        // Send the message using the existing sendMessageToPython method
        System.out.println("message being sent: " + message);
        sendMessageToPython(message);

        System.out.println(message);
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
        buttonSize = Math.min(maxBoardWidth / (COLUMNS + 1), maxBoardHeight / (ROWS + 2));

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

    //sends signals to the misty regarding row + column picked
    //the format needs to be in (row, column) or (column, row) where both has to be valid
    //row is displayed in letters and column are numbers
    private void buttonClick(int row, int column) {
        Log.d("GamePage", " button click called misty turnover: " + mistyTurnOver + ", mistyspeaking: " + mistySpeaking);

        if (!mistyTurnOver || mistySpeaking) {
            Log.d("GamePage", "Blocked: Misty is playing or speaking.");
            //Toast.makeText(this, "Wait for Misty to finish!", Toast.LENGTH_SHORT).show();
            return;
        }

        //if (timerExpired) {
        //Log.d("GamePage", "buttonClick: Main timer already expired. No action.");
        //Toast.makeText(GamePage.this, "Time's UP!", Toast.LENGTH_SHORT).show();
        // return;
        if (rightRevealed[row][column]) {
            Log.d("Gamepage", "Tile already revealed at row " + row + "col," + column);
            return;
        }
        if (!hasTimerStarted) {
            hasTimerStarted = true;
            GameTimer.getInstance().startTimer(() -> {
                //runOnUiThread(() ->
                //      Toast.makeText(GamePage.this, "Time's up!", Toast.LENGTH_SHORT).show()
                //);
                Log.d("GamePage", "Timer expired");
            });
        }
        Log.d("GamePage", "Player making move at row" + row + "col " + column);

        char v = flipButton(row, column, true); //player's turn
        //we check that v was not equal to a, since a is returned if the button has already been clicked.
        if (v != 'a') {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                showTurnMessage(false, 4000);
            }, 4000);
            //set misty turn state
            mistyTurnOver = false; // Misty's turn now
            mistySpeaking = false;
            disableUserBoard();

            sendCOutcomeOverTCP(v); // Send the value over TCP
            Log.d("GamePage", "userboard disabled after player's move.");
        }
    }

    private void sendCOutcomeOverTCP(char v) {
        if (mTcpClient != null) {
            // Prepare the message string
            String messageToSend = "Coutcome;" + v + ";";

            sendMessageToPython(messageToSend);
            Log.i("GamePage", "message being sent: " + messageToSend);

            final String mistyTurnMessage = "Mistyturn;" + "started";
            long delayMillis = 2500; // 2.5 seconds

            delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendMessageToPython(mistyTurnMessage);
                    Log.i("GamePage", "message being sent: " + mistyTurnMessage);
                }
            }, delayMillis);
        }
    }

    //the method that sets up the grid with the numbers/letters
    private void setupGrid() {
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
            columnPadding = 70; // if number == 1 move it more to the right same iwth 2 and 3
            rowPadding = 30;
        } else { // Easy mode (3x3)
            columnPadding = 70;
            rowPadding = 50;
        }

        // Add column numbers
        // Add column numbers
        for (int i = 0; i < COLUMNS; i++) {
            TextView colLabelLeft = new TextView(this);
            colLabelLeft.setText(String.valueOf(i + 1));
            colLabelLeft.setGravity(Gravity.CENTER);
            colLabelLeft.setTextColor(Color.WHITE);
            colLabelLeft.setTextSize(22);

            // Match column label width to game button width
            LinearLayout.LayoutParams paramsLeft = new LinearLayout.LayoutParams(buttonSize, ViewGroup.LayoutParams.WRAP_CONTENT);
            colLabelLeft.setLayoutParams(paramsLeft);
            leftColumnNumbers.addView(colLabelLeft);

            TextView colLabelRight = new TextView(this);
            colLabelRight.setText(String.valueOf(i + 1));
            colLabelRight.setGravity(Gravity.CENTER);
            colLabelRight.setTextColor(Color.WHITE);
            colLabelRight.setTextSize(22);

            LinearLayout.LayoutParams paramsRight = new LinearLayout.LayoutParams(buttonSize, ViewGroup.LayoutParams.WRAP_CONTENT);
            colLabelRight.setLayoutParams(paramsRight);
            rightColumnNumbers.addView(colLabelRight);
        }

        // Add row numbers
        char[] rowLettersEasy = {'D','J','Z'};
        char[] rowLetters = {'S', 'T', 'V','W','X','Y','Z'};
        for (int i = 0; i < ROWS; i++) {
            TextView rowLabelLeft = new TextView(this);
            if (difficulty.equals("Easy")) {
                rowLabelLeft.setText(String.valueOf(rowLettersEasy[i]));
            }else {
                rowLabelLeft.setText(String.valueOf(rowLetters[i]));
            }
            rowLabelLeft.setTextSize(25f);
            rowLabelLeft.setTextColor(Color.WHITE);

            rowPadding = (ROWS == 3) ? 45 : (ROWS == 5) ? 20 : 12;
            rowLabelLeft.setPadding(50, rowPadding, 10, rowPadding);
            leftRowNumbers.addView(rowLabelLeft);

            TextView rowLabelRight = new TextView(this);
            if(difficulty.equals("Easy")) {
                rowLabelRight.setText(String.valueOf(rowLettersEasy[i]));
            }else{
                rowLabelRight.setText(String.valueOf(rowLetters[i]));
            }
            rowLabelRight.setTextSize(25f);
            rowLabelRight.setTextColor(Color.WHITE);
            rowLabelRight.setPadding(50, rowPadding, 10, rowPadding);
            rightRowNumbers.addView(rowLabelRight);
        }
    }


    private char flipButton(int row, int col, boolean isRightBoard) {
        Button[][] buttons = isRightBoard ? rightButtons : leftButtons;
        char[][] numbers = isRightBoard ? rightNumbers : leftNumbers;
        boolean[][] revealed = isRightBoard ? rightRevealed : leftRevealed;

        if (!revealed[row][col]) {

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

    //private void resetGame() {
    //Intent intent = new Intent(GamePage.this, GamePage.class);
    //intent.putExtra("difficulty", getIntent().getStringExtra("difficulty"));
    //startActivity(intent);
    // finish();
    //}

    //utilized the boolean flag isRightBoard that was in the flipButton
    //if it's the left board then go into that condition
    //if not then it'd be the right baord that'd get updated
    //above was the reset game but it resets the whole game
    //so i changed it to reset board so it changes the individual board
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
                showTurnMessage(true,3000);
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
    private void sendHomeButtonClick() {
        String messageToSend = "Homebutton; true";
        Log.d("GamePage", "Sending: " + messageToSend);
        sendMessageToPython(messageToSend);
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

    //loop through all buttons on left board
    //disables each button so player cannot click any tiles
    //used right after player makes a move during misty's turn
    private void disableUserBoard() {
        Log.d("GamePage", "disabling user board");
        runOnUiThread(() -> {
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    rightButtons[row][col].setEnabled(false);
                }
            }
        });
    }

    //loops through all buttons on left board
    //enables only unrevealed tiles
    //makes sure players can click only hidden tiles
    //used after misty finishes her turn and speaking to give control back to player
    private void enableUserBoard() {
        Log.d("GamePage", "enabling user board");
        runOnUiThread(() -> {
            if (mistyTurnOver && !mistySpeaking) {
                for (int row = 0; row < ROWS; row++) {
                    for (int col = 0; col < COLUMNS; col++) {
                        if (!rightRevealed[row][col]) {
                            rightButtons[row][col].setEnabled(true);
                        }
                    }
                }
                Log.d("GamePage", "userboard enabled player can move");
            } else {
                Log.d("GamePage", "Userboard not enabled misty turn over" + mistyTurnOver + " mistySpeaking " + mistySpeaking);
            }
        });
    }
    //showTurnMessage(false); --> misty's turn
    //showTurnMessage(true); --> player's turn
    private void showTurnMessage(boolean isPlayerTurn, int delayTime) {
        String turnText = isPlayerTurn ? "      " + playerAvatarColor + " Avatar's Turn " :"      " + mistyAvatarColor + " Avatar's Turn!";
        runOnUiThread(() -> {
            turnIndicatorText.setText(turnText);
            turnIndicatorText.setVisibility(View.VISIBLE);
            turnIndicatorText.bringToFront();

            ImageView leftAvatarOverlay = findViewById(R.id.leftAvatarImage1);
            ImageView rightAvatarOverlay = findViewById(R.id.rightAvatarImage1);

            // Show the correct avatar overlay
            if (isPlayerTurn) {
                rightAvatarOverlay.setVisibility(View.VISIBLE);
                leftAvatarOverlay.setVisibility(View.GONE);
                rightAvatarOverlay.setTranslationX(-270);
                rightAvatarOverlay.setTranslationY(425);
                rightAvatarOverlay.bringToFront();
            } else {
                rightAvatarOverlay.setVisibility(View.GONE);
                leftAvatarOverlay.setVisibility(View.VISIBLE);
                leftAvatarOverlay.setTranslationX(-270);
                leftAvatarOverlay.setTranslationY(425);
                leftAvatarOverlay.bringToFront();
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                turnIndicatorText.setVisibility(View.GONE);
                leftAvatarOverlay.setVisibility(View.GONE);
                rightAvatarOverlay.setVisibility(View.GONE);
            }, delayTime);
        });
    }
    private void avatarChosen(){

        String avatar = getIntent().getStringExtra("avatar");
        if (avatar == null) avatar = "Red";

        if ("Red".equalsIgnoreCase(avatar)) {
            playerAvatarColor = "Red";
            mistyAvatarColor = "Blue";
        } else {
            playerAvatarColor = "Blue";
            mistyAvatarColor = "Red";
        }
        ImageView leftAvatarImage = findViewById(R.id.leftAvatarImage);
        ImageView rightAvatarImage = findViewById(R.id.rightAvatarImage);

        ImageView leftAvatarOverlay = findViewById(R.id.leftAvatarImage1);
        ImageView rightAvatarOverlay = findViewById(R.id.rightAvatarImage1);

        leftAvatarImage.setTranslationX(30);
        rightAvatarImage.setTranslationX(30);

        if ("Red".equalsIgnoreCase(avatar)) {
            rightAvatarImage.setImageResource(R.drawable.fire);     // Player (red)
            leftAvatarImage.setImageResource(R.drawable.water);   // Misty (blue)

            rightAvatarOverlay.setImageResource(R.drawable.fire2);  // Player (red overlay)
            leftAvatarOverlay.setImageResource(R.drawable.water2); // Misty (blue overlay)
        } else {
            rightAvatarImage.setImageResource(R.drawable.water);     // Player (blue)
            leftAvatarImage.setImageResource(R.drawable.fire);     // Misty (red)

            rightAvatarOverlay.setImageResource(R.drawable.water2);  // Player (blue overlay)
            leftAvatarOverlay.setImageResource(R.drawable.fire2);  // Misty (red overlay)
        }

        // Hide both overlays by default
        leftAvatarOverlay.setVisibility(View.GONE);
        rightAvatarOverlay.setVisibility(View.GONE);
    }
    private void autoClickBackHomeButton(){
        runOnUiThread(() -> {
            Toast.makeText(GamePage.this, "Game Over! returning home.", Toast.LENGTH_SHORT).show();
            Button backHomeButton = findViewById(R.id.backHomeButton);
            backHomeButton.setEnabled(false); // disable to prevent manual cliking
            sendHomeButtonClick(); // send message to python that home is clicked

            // delay before going to Homeactivity
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(GamePage.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }, 5000); // 5 seconds delay
        });
    }
}
