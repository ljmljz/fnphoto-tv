package com.fnphoto.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;

import com.fnphoto.tv.cache.CachedImageLoader;

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
            // 获取 token
            SharedPreferences prefs = getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
            String token = prefs.getString("api_token", "");

            // 使用带缓存的加载器加载原图
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            
            CachedImageLoader.loadImage(this, photoUrl, token, screenWidth, screenHeight,
                    new CachedImageLoader.ImageLoadCallback() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap) {
                            imageView.setImageBitmap(bitmap);
                        }
                        
                        @Override
                        public void onLoadFailed() {
                            Toast.makeText(PhotoDetailActivity.this, "图片加载失败", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        // 点击退出
        imageView.setOnClickListener(v -> finish());
    }
}
