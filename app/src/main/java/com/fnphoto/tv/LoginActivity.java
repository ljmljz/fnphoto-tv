package com.fnphoto.tv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import androidx.fragment.app.FragmentActivity;
import com.fnphoto.tv.api.FnWebSocketClient;
import org.json.JSONObject;

public class LoginActivity extends FragmentActivity {
    private EditText editUrl, editUser, editPass;
    private ProgressBar progressBar;
    private FnWebSocketClient wsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置全屏模式，隐藏状态栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_login);

        editUrl = findViewById(R.id.edit_nas_url);
        editUser = findViewById(R.id.edit_username);
        editPass = findViewById(R.id.edit_api_token);
        progressBar = findViewById(R.id.progress_bar);

        wsClient = new FnWebSocketClient();

        findViewById(R.id.btn_login).setOnClickListener(v -> {
            String url = editUrl.getText().toString().trim();
            String user = editUser.getText().toString().trim();
            String pass = editPass.getText().toString().trim();

            if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) return;

            progressBar.setVisibility(View.VISIBLE);
            wsClient.startLogin(url, user, pass, new FnWebSocketClient.LoginCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        android.util.Log.d("FnWebSocket", "Login Successfully");
                        progressBar.setVisibility(View.GONE);
                        saveSession(url, response);
                    });
                }

                @Override
                public void onError(String msg) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Login Error: " + msg, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private void saveSession(String url, JSONObject response) {
        try {
            SharedPreferences prefs = getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
            prefs.edit()
                .putString("nas_url", url)
                .putString("api_token", response.getString("token"))
                .putString("secret", response.getString("secret"))
                .putString("backId", response.getString("backId"))
                .apply();
            
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save session", Toast.LENGTH_SHORT).show();
        }
    }
}
