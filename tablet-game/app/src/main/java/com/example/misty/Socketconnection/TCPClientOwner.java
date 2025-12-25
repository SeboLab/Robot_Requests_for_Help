package com.example.misty.Socketconnection;
//package com.example.myapplication.Socketconnection;

import android.os.Looper;

public interface TCPClientOwner {
    Looper getMainLooper();
    void messageReceived(String message);

    //void getSessionOwner();

    void disableButtons();
}