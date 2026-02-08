package com.fnphoto.tv.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.json.JSONObject;

public class FnWebSocketManager {
    private OkHttpClient client;
    private WebSocket mainWs;
    private WebSocket fileWs;
    private WebSocket timerWs;
    private String baseUrl;
    private String token;

    public interface WsCallback {
        void onMessage(String type, JSONObject data);
        void onError(Throwable t);
    }

    public FnWebSocketManager() {
        // 延迟初始化 OkHttpClient，捕获可能的异常
        try {
            this.client = new OkHttpClient();
            android.util.Log.d("FnWebSocketManager", "OkHttpClient initialized successfully");
        } catch (Exception e) {
            android.util.Log.e("FnWebSocketManager", "Failed to initialize OkHttpClient: " + e.getMessage(), e);
            throw e;
        }
    }

    public void connect(String baseUrl, String token, WsCallback callback) {
        this.baseUrl = baseUrl.replace("http", "ws");
        this.token = token;

        // Establish the three specific connections as per fnOS standards
        mainWs = createSocket("main", callback);
        fileWs = createSocket("file", callback);
        timerWs = createSocket("timer", callback);
    }

    private WebSocket createSocket(String endpoint, WsCallback callback) {
        String url = String.format("%s/ws/%s?token=%s", baseUrl, endpoint, token);
        Request request = new Request.Builder().url(url).build();

        return client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                // Connection established
                if ("timer".equals(endpoint)) {
                    startHeartbeat(webSocket);
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject json = new JSONObject(text);
                    callback.onMessage(endpoint, json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                callback.onError(t);
            }
        });
    }

    private void startHeartbeat(WebSocket ws) {
        // Implementation of the specific heartbeat message for the timer socket
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject hb = new JSONObject();
                    hb.put("type", "heartbeat");
                    hb.put("timestamp", System.currentTimeMillis());
                    ws.send(hb.toString());
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 30000);
                } catch (Exception e) {}
            }
        }, 30000);
    }

    public void sendMessage(String endpoint, JSONObject message) {
        WebSocket ws = null;
        if ("main".equals(endpoint)) ws = mainWs;
        else if ("file".equals(endpoint)) ws = fileWs;
        else if ("timer".equals(endpoint)) ws = timerWs;

        if (ws != null) {
            ws.send(message.toString());
        }
    }
}
