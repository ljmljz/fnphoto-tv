package com.fnphoto.tv.api;

import android.util.Base64;
import okhttp3.*;
import org.json.JSONObject;
import java.security.SecureRandom;

public class FnWebSocketClient {
    private OkHttpClient client = new OkHttpClient();
    private WebSocket mainWs;
    private String serverUrl;
    private String currentSi;
    
    public interface LoginCallback {
        void onSuccess(JSONObject response);
        void onError(String msg);
    }

    public void startLogin(String url, String username, String password, LoginCallback callback) {
        // Ensure URL format is correct for WebSocket
        if (url.startsWith("http://")) {
            this.serverUrl = url.replace("http://", "ws://");
        } else if (url.startsWith("https://")) {
            this.serverUrl = url.replace("https://", "wss://");
        } else {
            this.serverUrl = "ws://" + url;
        }
        this.serverUrl += "/websocket?type=main";
        
        android.util.Log.d("FnWebSocket", "Connecting to: " + serverUrl);
        
        Request request = new Request.Builder().url(serverUrl).build();
        
        mainWs = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                android.util.Log.d("FnWebSocket", "Connection opened, requesting RSA key");
                // Start keepalive ping every 10 seconds to prevent timeout
                startKeepalive(webSocket);
                requestRsaKey(webSocket);
            }
            
            private void startKeepalive(final WebSocket ws) {
                final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                final Runnable pingRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (ws != null) {
                            ws.send("{\"req\":\"ping\"}");
//                            android.util.Log.d("FnWebSocket", "send ping");
                            handler.postDelayed(this, 10000);
                        }
                    }
                };
                handler.postDelayed(pingRunnable, 10000);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                android.util.Log.d("FnWebSocket", "Received: " + text);
                try {
                    JSONObject json = new JSONObject(text);
                    
                    // Handle ping from server
                    if (json.has("res") && "pong".equals(json.optString("res"))) {
//                        android.util.Log.d("FnWebSocket", "received pong");
                        return;
                    }
                    
                    if (json.has("pub")) {
                        currentSi = json.optString("si");
                        android.util.Log.d("FnWebSocket", "Got SI: " + currentSi);
                        performEncryptedLogin(webSocket, json.getString("pub"), currentSi, username, password, callback);
                    } else if ("succ".equals(json.optString("result"))) {
                        FnProtocolUtils.setBackId(json.optString("backId"));
                        callback.onSuccess(json);
                    } else if (json.has("errno")) {
                        int errno = json.optInt("errno");
                        String result = json.optString("result");
                        android.util.Log.e("FnWebSocket", "Server error: errno=" + errno + ", result=" + result);
                        callback.onError("Server error " + errno + ": " + result);
                    }
                } catch (Exception e) {
                    android.util.Log.e("FnWebSocket", "Error processing message: " + e.getMessage(), e);
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                android.util.Log.e("FnWebSocket", "Connection failed: " + t.getMessage(), t);
                callback.onError("Connection failed: " + t.getMessage());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                android.util.Log.w("FnWebSocket", "Closing: " + code + " - " + reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                android.util.Log.w("FnWebSocket", "Closed: " + code + " - " + reason);
            }
        });
    }

    private void requestRsaKey(WebSocket ws) {
        try {
            JSONObject req = new JSONObject();
            req.put("req", "util.crypto.getRSAPub");
            req.put("reqid", FnProtocolUtils.generateReqId());
            String jsonStr = req.toString();
            android.util.Log.d("FnWebSocket", "Sending: " + jsonStr);
            boolean sent = ws.send(jsonStr);
            android.util.Log.d("FnWebSocket", "Send result: " + sent);
        } catch (Exception e) {
            android.util.Log.e("FnWebSocket", "Error sending RSA request", e);
        }
    }

    private void performEncryptedLogin(final WebSocket ws, final String pubKey, final String si, 
                                       final String user, final String pass, final LoginCallback callback) {
        // Run encryption on background thread to avoid blocking WebSocket
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    android.util.Log.d("FnWebSocket", "Starting encrypted login on background thread, si=" + si);
                    
                    // 1. Prepare raw login data - MUST match Python format exactly
                    // Reference: fnnas-api/sdk/encryption.py
                    final JSONObject rawData = new JSONObject();
                    rawData.put("reqid", FnProtocolUtils.generateReqId());
                    rawData.put("user", user);
                    rawData.put("password", pass);
                    rawData.put("deviceType", "AndroidTV");
                    rawData.put("deviceName", "Android-TV-Box");
                    rawData.put("stay", false);  // Must be lowercase 'false' for JSON
                    rawData.put("req", "user.login");
                    rawData.put("si", si);  // Must be String, not Long
                    
                    final String rawDataStr = rawData.toString();
                    android.util.Log.d("FnWebSocket", "Raw data: " + rawDataStr);

                    // 2. Generate AES key (MUST be 32 chars for fnOS) and IV (16 bytes)
                    // Reference: fnnas-api/sdk/base_client.py line 32 - generate_random_string(32)
                    final String aesKey = FnProtocolUtils.generateRandomString(32);
                    final byte[] iv = FnProtocolUtils.generateIV();
                    android.util.Log.d("FnWebSocket", "AES key (32 chars): " + aesKey);
                    android.util.Log.d("FnWebSocket", "IV generated: " + Base64.encodeToString(iv, Base64.NO_WRAP));

                    // 3. Encrypt
                    final String aesEncrypted = FnProtocolUtils.aesEncrypt(rawDataStr, aesKey, iv);
                    final String rsaEncrypted = FnProtocolUtils.rsaEncrypt(pubKey, aesKey);
                    android.util.Log.d("FnWebSocket", "AES encrypted, RSA encrypted");

                    // 4. Build payload - reqid is INSIDE the AES encrypted data, not outside!
                    // Reference: fnnas-api/sdk/encryption.py login_encrypt function
                    final JSONObject encryptedReq = new JSONObject();
                    encryptedReq.put("req", "encrypted");
                    encryptedReq.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP));
                    encryptedReq.put("rsa", rsaEncrypted);
                    encryptedReq.put("aes", aesEncrypted);
                    // Note: reqid is NOT here - it's already inside the AES encrypted payload
                    
                    final String finalPayload = encryptedReq.toString();
                    android.util.Log.d("FnWebSocket", "Payload ready, length: " + finalPayload.length());
                    
                    // Send on main thread
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            boolean sent = ws.send(finalPayload);
                            android.util.Log.d("FnWebSocket", "Login sent: " + sent);
                        }
                    });
                    
                } catch (final Exception e) {
                    android.util.Log.e("FnWebSocket", "Encryption error", e);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError("Encryption failed: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }
}
