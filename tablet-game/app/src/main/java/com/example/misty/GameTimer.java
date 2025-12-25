package com.example.misty;

import android.os.CountDownTimer;
import android.util.Log;

import com.example.misty.Socketconnection.TCPClient;

public class GameTimer {
    private static GameTimer instance;
    private CountDownTimer timer;


    private long remainingTime = 5* 60000; //5 minutes in milliseconds
    private boolean isRunning = false;
    private GameTimerListener listener;

    public interface GameTimerListener {
        void onTimerFinish();
    }

    private GameTimer() {}

    public static GameTimer getInstance() {
        if (instance == null) {
            instance = new GameTimer();
        }
        return instance;
    }

    public void startTimer(GameTimerListener listener) {
        if (isRunning) return;

        this.listener = listener;
        isRunning = true;

        timer = new CountDownTimer(remainingTime, 60000) {
            int tickCount = 0;

            @Override
            public void onTick(long millisUntilFinished) {
                tickCount++;
                remainingTime = millisUntilFinished;

            int minutePassed = tickCount;

            new Thread(() -> {
                TCPClient client = TCPClient.getInstance();
                if (!client.isConnected()) {
                    client.run();
                }
                if (client.isConnected()) {
                    client.sendMessage("Timer;minute" + minutePassed + "up");
                    Log.d("GameTimer", "Sent minute " + minutePassed + "up");

                } else {
                    Log.e("GameTimer", "TCPClient not connected. Could not update minute");
                }
            }).start();
        }

            @Override
            public void onFinish() {
                isRunning = false;
                if (listener != null) {
                    listener.onTimerFinish();
                }
                new Thread(() -> {
                    TCPClient client = TCPClient.getInstance();
                    if (!client.isConnected()) {
                        client.run();
                    }
                    if (client.isConnected()) {
                        client.sendMessage("Timer;" + "timer finished");
                        Log.d("GameTimer", "Sent TIMER_FINISHED over TCP");

                    } else {
                        Log.e("GameTimer", "TCPClient not connected. Could not send TIMER_FINISHED");
                    }

                }).start();
            }
        };
        timer.start();
    }

    public void resetTimer() {
        if (timer != null) {
            timer.cancel();
        }
        remainingTime = 60000;
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }
}
