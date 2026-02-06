package com.fnphoto.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.fragment.app.FragmentActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

public class PhotoDetailActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置全屏模式，隐藏状态栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        setContentView(imageView);

        String photoUrl = getIntent().getStringExtra("PHOTO_URL");
        String title = getIntent().getStringExtra("PHOTO_TITLE");

        if (photoUrl != null && !photoUrl.isEmpty()) {
            // 获取 token 和生成 authx
            SharedPreferences prefs = getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
            String token = prefs.getString("api_token", "");

            // 构建带认证头的 GlideUrl
            GlideUrl glideUrl = new GlideUrl(photoUrl, new LazyHeaders.Builder()
                    .addHeader("accesstoken", token)
                    .build());

            Glide.with(this)
                    .load(glideUrl)
                    .into(imageView);
        }

        // 点击退出
        imageView.setOnClickListener(v -> finish());
    }
}
