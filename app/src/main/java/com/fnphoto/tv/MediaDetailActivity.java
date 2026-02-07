package com.fnphoto.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;
import com.fnphoto.tv.cache.CachedImageLoader;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import java.util.ArrayList;
import java.util.List;

public class MediaDetailActivity extends FragmentActivity {
    private static final String TAG = "MediaDetailActivity";
    private static final long DEBOUNCE_TIME = 300; // 防抖时间300ms

    private FrameLayout container;
    private ImageView imageView;
    private PlayerView playerView;
    private SimpleExoPlayer player;

    private List<MediaItem> mediaList;
    private int currentIndex;
    private Handler debounceHandler = new Handler(Looper.getMainLooper());
    private boolean canSwitch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置全屏模式
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 创建容器
        container = new FrameLayout(this);
        container.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(container);

        // 获取传递的数据
        mediaList = (ArrayList<MediaItem>) getIntent().getSerializableExtra("MEDIA_LIST");
        currentIndex = getIntent().getIntExtra("CURRENT_INDEX", 0);

        if (mediaList == null || mediaList.isEmpty()) {
            Toast.makeText(this, "没有可显示的媒体", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 显示当前媒体
        showCurrentMedia();
    }

    private void showCurrentMedia() {
        if (currentIndex < 0 || currentIndex >= mediaList.size()) {
            return;
        }

        MediaItem item = mediaList.get(currentIndex);
        Log.d(TAG, "Showing media: " + item.getTitle() + " type: " + item.getType());

        // 清除之前的视图
        container.removeAllViews();

        if ("video".equals(item.getType())) {
            showVideo(item);
        } else {
            showPhoto(item);
        }

        // 显示提示
        Toast.makeText(this, (currentIndex + 1) + " / " + mediaList.size(), Toast.LENGTH_SHORT).show();
    }

    private void showPhoto(MediaItem item) {
        imageView = new ImageView(this);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        container.addView(imageView);

        String mediaUrl = item.getMediaUrl();
        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            // 获取 token
            SharedPreferences prefs = getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
            String token = prefs.getString("api_token", "");

            // 使用带缓存的加载器
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;

            CachedImageLoader.loadImage(this, mediaUrl, token, screenWidth, screenHeight,
                    new CachedImageLoader.ImageLoadCallback() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap) {
                            imageView.setImageBitmap(bitmap);
                        }

                        @Override
                        public void onLoadFailed() {
                            Toast.makeText(MediaDetailActivity.this, "图片加载失败", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        // 点击退出
        imageView.setOnClickListener(v -> finish());
    }

    private void showVideo(MediaItem item) {
        playerView = new PlayerView(this);
        playerView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        container.addView(playerView);

        // 初始化播放器
        if (player != null) {
            player.release();
        }
        player = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        String mediaUrl = item.getMediaUrl();
        if (mediaUrl != null) {
            com.google.android.exoplayer2.MediaItem mediaItem = com.google.android.exoplayer2.MediaItem.fromUri(mediaUrl);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        }
    }

    private void switchToPrevious() {
        if (!canSwitch) return;

        if (currentIndex > 0) {
            currentIndex--;
            showCurrentMedia();
            debounceSwitch();
        } else {
            Toast.makeText(this, "已经是第一个", Toast.LENGTH_SHORT).show();
        }
    }

    private void switchToNext() {
        if (!canSwitch) return;

        if (currentIndex < mediaList.size() - 1) {
            currentIndex++;
            showCurrentMedia();
            debounceSwitch();
        } else {
            Toast.makeText(this, "已经是最后一个", Toast.LENGTH_SHORT).show();
        }
    }

    private void debounceSwitch() {
        canSwitch = false;
        debounceHandler.postDelayed(() -> canSwitch = true, DEBOUNCE_TIME);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                switchToPrevious();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                switchToNext();
                return true;
            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        debounceHandler.removeCallbacksAndMessages(null);
    }
}
