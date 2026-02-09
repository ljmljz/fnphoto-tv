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
    private CheckBox cbRemember;
    private ProgressBar progressBar;
    private FnWebSocketClient wsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 检查是否有保存的登录信息
        SharedPreferences prefs = getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
        boolean hasCredentials = prefs.getBoolean("has_credentials", false);
        
        if (hasCredentials) {
            // 有保存的登录信息，尝试自动登录
            String savedUrl = prefs.getString("saved_url", "");
            String savedUser = prefs.getString("saved_user", "");
            String savedPass = prefs.getString("saved_pass", "");
            
            if (!savedUrl.isEmpty() && !savedUser.isEmpty() && !savedPass.isEmpty()) {
                // 直接跳转到主界面，在主界面中静默登录或恢复token
                if (prefs.getString("api_token", "").isEmpty()) {
                    // Token已过期，需要重新登录
                    showLoginUI();
                    // 自动填充之前保存的信息
                    autoLogin(savedUrl, savedUser, savedPass);
                } else {
                    // Token还有效，直接跳转到主界面
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                    return;
                }
                return;
            }
        }
        
        showLoginUI();
    }
    
    private void showLoginUI() {
        // 设置全屏模式，隐藏状态栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_login);

        editUrl = findViewById(R.id.edit_nas_url);
        editUser = findViewById(R.id.edit_username);
        editPass = findViewById(R.id.edit_api_token);
        cbRemember = findViewById(R.id.cb_remember);
        progressBar = findViewById(R.id.progress_bar);
        
        // 加载之前保存的信息（如果有）
        SharedPreferences prefs = getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
        String savedUrl = prefs.getString("saved_url", "");
        String savedUser = prefs.getString("saved_user", "");
        
        if (!savedUrl.isEmpty()) {
            editUrl.setText(savedUrl);
        }
        if (!savedUser.isEmpty()) {
            editUser.setText(savedUser);
        }

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
                        saveSession(url, user, pass, response);
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
    
    private void autoLogin(String url, String user, String pass) {
        // 自动填充并触发登录
        editUrl.setText(url);
        editUser.setText(user);
        editPass.setText(pass);
        
        // 延迟一点再自动登录，让用户看到界面
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            progressBar.setVisibility(View.VISIBLE);
            wsClient.startLogin(url, user, pass, new FnWebSocketClient.LoginCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        android.util.Log.d("FnWebSocket", "Auto Login Successfully");
                        progressBar.setVisibility(View.GONE);
                        saveSession(url, user, pass, response);
                    });
                }

                @Override
                public void onError(String msg) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Auto Login Failed: " + msg, Toast.LENGTH_LONG).show();
                        // 自动登录失败，让用户手动输入
                    });
                }
            });
        }, 500);
    }

    private void saveSession(String url, String user, String pass, JSONObject response) {
        try {
            SharedPreferences prefs = getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            // 保存token等会话信息
            editor.putString("nas_url", url)
                .putString("api_token", response.getString("token"))
                .putString("secret", response.getString("secret"))
                .putString("backId", response.getString("backId"));
            
            // 如果勾选了记住登录信息，保存用户名密码
            if (cbRemember.isChecked()) {
                editor.putBoolean("has_credentials", true)
                    .putString("saved_url", url)
                    .putString("saved_user", user)
                    .putString("saved_pass", pass);
            } else {
                // 不记住，清除之前保存的凭据
                editor.putBoolean("has_credentials", false)
                    .remove("saved_url")
                    .remove("saved_user")
                    .remove("saved_pass");
            }
            
            editor.apply();
            
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save session", Toast.LENGTH_SHORT).show();
        }
    }
}
