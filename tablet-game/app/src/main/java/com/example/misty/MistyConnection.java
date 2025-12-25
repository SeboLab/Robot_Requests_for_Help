package com.example.misty;

import android.util.Log;

import androidx.annotation.WorkerThread;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class MistyConnection {
    private static final String TAG = "MistyConnection";

    // Use OkHttp's MediaType
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String ip;
    private final int port;

    // Reuse the same OkHttpClient
    private final OkHttpClient client = new OkHttpClient();

    public MistyConnection(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    /**
     * Speaks the provided message. Must be called off the main thread.
     */
    @WorkerThread
    public boolean speak(String message, boolean flush, String utteranceId) {
        String url = "http://" + ip + ":" + port + "/api/tts/speak";
        // Build JSON payload. (For older Java versions, use String.format or concatenation)
        String jsonPayload = String.format(
                "{" +
                        "\"Text\":\"<speak>%s</speak>\"," +
                        "\"Flush\":%b," +
                        "\"UtteranceId\":\"%s\"" +
                        "}",
                message, flush, utteranceId
        );

        Log.d(TAG, "Sending speech request to Misty with URL: " + url);
        Log.d(TAG, "JSON Payload: " + jsonPayload);

        RequestBody requestBody = RequestBody.create(jsonPayload, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Log.d(TAG, "Response Code: " + response.code());
            String responseBody = response.body() != null ? response.body().string() : "";
            Log.d(TAG, "Response Body: " + responseBody);

            return response.isSuccessful(); // true if in 200..299 range
        } catch (IOException e) {
            Log.e(TAG, "Failed to send speech request to Misty: ", e);
            return false;
        }
    }

    /**
     * Moves Misty's head. Must be called off the main thread.
     */
    @WorkerThread
    public boolean moveHead(int pitch, int roll, int yaw, int velocity) {
        String url = "http://" + ip + ":" + port + "/api/head";
        String jsonPayload = String.format(
                "{" +
                        "\"Pitch\":%d," +
                        "\"Roll\":%d," +
                        "\"Yaw\":%d," +
                        "\"Velocity\":%d" +
                        "}",
                pitch, roll, yaw, velocity
        );

        Log.d(TAG, "Sending head movement request to Misty with URL: " + url);
        Log.d(TAG, "JSON Payload: " + jsonPayload);

        RequestBody requestBody = RequestBody.create(jsonPayload, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Log.d(TAG, "Response Code: " + response.code());
            String responseBody = response.body() != null ? response.body().string() : "";
            Log.d(TAG, "Response Body: " + responseBody);

            return response.isSuccessful();
        } catch (IOException e) {
            Log.e(TAG, "Failed to send head movement request to Misty: ", e);
            return false;
        }
    }

    /**
     * Moves Misty's arm. Must be called off the main thread.
     */
    @WorkerThread
    public boolean moveArm(String arm, int position, int velocity) {
        String url = "http://" + ip + ":" + port + "/api/arms";
        String jsonPayload = String.format(
                "{" +
                        "\"Arm\":\"%s\"," +
                        "\"Position\":%d," +
                        "\"Velocity\":%d" +
                        "}",
                arm, position, velocity
        );

        Log.d(TAG, "Sending arm movement request to Misty with URL: " + url);
        Log.d(TAG, "JSON Payload: " + jsonPayload);

        RequestBody requestBody = RequestBody.create(jsonPayload, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Log.d(TAG, "Response Code: " + response.code());
            String responseBody = response.body() != null ? response.body().string() : "";
            Log.d(TAG, "Response Body: " + responseBody);

            return response.isSuccessful();
        } catch (IOException e) {
            Log.e(TAG, "Failed to send arm movement request to Misty: ", e);
            return false;
        }
    }
}
