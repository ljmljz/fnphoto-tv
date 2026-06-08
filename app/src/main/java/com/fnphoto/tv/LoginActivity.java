package com.fnphoto.tv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.fragment.app.FragmentActivity;
import com.fnphoto.tv.api.FnConnectApi;
import com.fnphoto.tv.api.FnWebSocketClient;
import com.fnphoto.tv.api.UrlUtils;
import org.json.JSONObject;

public class LoginActivity extends FragmentActivity {
    private EditText editUrl, editUser, editPass;
    private CheckBox cbRemember;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private FnWebSocketClient wsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
        boolean hasCredentials = prefs.getBoolean("has_credentials", false);

        if (hasCredentials) {
            String savedUrl = prefs.getString("saved_url", "");
            String savedUser = prefs.getString("saved_user", "");
            String savedPass = prefs.getString("saved_pass", "");

            if (!savedUrl.isEmpty() && !savedUser.isEmpty() && !savedPass.isEmpty()) {
                if (prefs.getString("api_token", "").isEmpty()) {
                    showLoginUI();
                    autoLogin(savedUrl, savedUser, savedPass);
                } else {
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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_login);

        editUrl = findViewById(R.id.edit_nas_url);
        editUser = findViewById(R.id.edit_username);
        editPass = findViewById(R.id.edit_api_token);
        cbRemember = findViewById(R.id.cb_remember);
        progressBar = findViewById(R.id.progress_bar);
        tvStatus = findViewById(R.id.tv_status);

        SharedPreferences prefs = getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
        String savedUrl = prefs.getString("saved_url", "");
        String savedUser = prefs.getString("saved_user", "");

        if (!savedUrl.isEmpty()) {
            editUrl.setText(savedUrl);
        }
        if (!savedUser.isEmpty()) {
            editUser.setText(savedUser);
        }

        editPass.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                clearFocus();
                performLogin();
                return true;
            }
            return false;
        });

        findViewById(R.id.btn_login).setOnClickListener(v -> {
            clearFocus();
            performLogin();
        });
    }

    private void clearFocus() {
        editUrl.clearFocus();
        editUser.clearFocus();
        editPass.clearFocus();
        findViewById(R.id.btn_login).requestFocus();
    }

    private void performLogin() {
        String input = editUrl.getText().toString().trim();
        String user = editUser.getText().toString().trim();
        String pass = editPass.getText().toString().trim();

        if (input.isEmpty() || user.isEmpty() || pass.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.VISIBLE);

        if (FnConnectApi.isFnId(input)) {
            resolveFnIdAndLogin(input, user, pass);
        } else {
            String httpUrl = normalizeUrl(input);
            tvStatus.setText("正在连接 " + input + "...");
            doWebSocketLogin(httpUrl, user, pass);
        }
    }

    private void resolveFnIdAndLogin(String fnId, String user, String pass) {
        tvStatus.setText("正在查询 FN ID: " + fnId + "...");

        FnConnectApi connectApi = new FnConnectApi();
        connectApi.fetchNasList(fnId, new FnConnectApi.NasListCallback() {
            @Override
            public void onSuccess(FnConnectApi.NasListResponse response) {
                if (response.addresses.isEmpty()) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        tvStatus.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "未找到该 FN ID 对应的 NAS 地址", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                runOnUiThread(() -> tvStatus.setText("正在探测可达地址..."));

                String httpUrl = FnConnectApi.findReachableAddrSync(response.addresses);

                if (httpUrl != null) {
                    runOnUiThread(() -> tvStatus.setText("已找到可达地址: " + httpUrl.replace("http://", "")));
                    doWebSocketLogin(httpUrl, user, pass);
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        tvStatus.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "无法连接到该 FN ID 对应的任何地址，请检查网络或直接输入IP/域名", Toast.LENGTH_LONG).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "查询 FN ID 失败: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    String normalizeUrl(String input) {
        return UrlUtils.normalizeUrl(input);
    }

    private void doWebSocketLogin(String httpUrl, String user, String pass) {
        wsClient = new FnWebSocketClient();
        wsClient.startLogin(httpUrl, user, pass, new FnWebSocketClient.LoginCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    android.util.Log.d("FnWebSocket", "Login Successfully");
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setVisibility(View.GONE);
                    saveSession(httpUrl, user, pass, response);
                });
            }

            @Override
            public void onError(String msg) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "登录失败: " + msg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void autoLogin(String url, String user, String pass) {
        editUrl.setText(url);
        editUser.setText(user);
        editPass.setText(pass);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            progressBar.setVisibility(View.VISIBLE);
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("正在自动登录...");

            if (FnConnectApi.isFnId(url)) {
                resolveFnIdAndLogin(url, user, pass);
            } else {
                String httpUrl = normalizeUrl(url);
                doWebSocketLogin(httpUrl, user, pass);
            }
        }, 500);
    }

    private void saveSession(String url, String user, String pass, JSONObject response) {
        try {
            SharedPreferences prefs = getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Normalize to full http/https URL for MainActivity/Retrofit
            String saveUrl = url;
            if (saveUrl.startsWith("ws://")) saveUrl = saveUrl.replace("ws://", "http://");
            if (saveUrl.startsWith("wss://")) saveUrl = saveUrl.replace("wss://", "https://");
            if (!saveUrl.startsWith("http://") && !saveUrl.startsWith("https://")) {
                saveUrl = "http://" + saveUrl;
            }

            editor.putString("nas_url", saveUrl)
                .putString("api_token", response.getString("token"))
                .putString("secret", response.getString("secret"))
                .putString("backId", response.getString("backId"));

            if (cbRemember.isChecked()) {
                editor.putBoolean("has_credentials", true)
                    .putString("saved_url", url)
                    .putString("saved_user", user)
                    .putString("saved_pass", pass);
            } else {
                editor.putBoolean("has_credentials", false)
                    .remove("saved_url")
                    .remove("saved_user")
                    .remove("saved_pass");
            }

            editor.apply();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "保存会话失败", Toast.LENGTH_SHORT).show();
        }
    }
}
