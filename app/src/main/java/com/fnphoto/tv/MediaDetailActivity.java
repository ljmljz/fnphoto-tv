package com.fnphoto.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;

import com.fnphoto.tv.cache.CachedImageLoader;
import com.fnphoto.tv.player.AuthenticatedHttpDataSourceFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
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
    private boolean isVideoPlaying = false;
    private MediaItem currentVideoItem; // 当前视频项，用于遥控器播放控制

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
        
        // 重置视频播放状态
        isVideoPlaying = false;
        currentVideoItem = null; // 重置当前视频项

        // 清除之前的视图
        container.removeAllViews();
        
        // 停止之前的播放器
        if (player != null) {
            player.release();
            player = null;
        }

        if ("video".equals(item.getType())) {
            showVideoPreview(item);
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

    /**
     * 显示视频预览图和播放按钮
     */
    private void showVideoPreview(MediaItem item) {
        // 保存当前视频项，用于遥控器控制
        currentVideoItem = item;
        
        // 使用 thumbnail 的 mUrl 作为预览图
        String previewUrl = item.getThumbnailUrl();
        
        if (previewUrl == null || previewUrl.isEmpty()) {
            // 没有预览图，直接播放
            startVideoPlayback(item);
            return;
        }
        
        // 创建 ImageView 显示预览
        imageView = new ImageView(this);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        container.addView(imageView);

        // 获取 token
        SharedPreferences prefs = getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("api_token", "");

        // 加载预览图并添加播放按钮
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        CachedImageLoader.loadImage(this, previewUrl, token, screenWidth, screenHeight,
                new CachedImageLoader.ImageLoadCallback() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap) {
                        // 创建带播放按钮的预览图
                        Bitmap composite = createVideoPreviewWithPlayButton(bitmap, screenWidth, screenHeight);
                        imageView.setImageBitmap(composite);
                    }

                    @Override
                    public void onLoadFailed() {
                        // 加载失败，直接播放
                        startVideoPlayback(item);
                    }
                });

        // 点击开始播放
        imageView.setOnClickListener(v -> {
            if (!isVideoPlaying) {
                startVideoPlayback(item);
            }
        });
    }

    /**
     * 创建带播放按钮的视频预览图
     */
    private Bitmap createVideoPreviewWithPlayButton(Bitmap thumbnail, int width, int height) {
        Bitmap composite = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(composite);
        
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        
        // 绘制缩略图（居中裁剪填充）
        int thumbWidth = thumbnail.getWidth();
        int thumbHeight = thumbnail.getHeight();
        
        float scale = Math.max((float) width / thumbWidth, (float) height / thumbHeight);
        float srcLeft = Math.max(0, (thumbWidth - width / scale) / 2);
        float srcTop = Math.max(0, (thumbHeight - height / scale) / 2);
        float srcRight = Math.min(thumbWidth, srcLeft + width / scale);
        float srcBottom = Math.min(thumbHeight, srcTop + height / scale);
        
        Rect srcRect = new Rect((int) srcLeft, (int) srcTop, (int) srcRight, (int) srcBottom);
        Rect dstRect = new Rect(0, 0, width, height);
        
        canvas.drawBitmap(thumbnail, srcRect, dstRect, paint);
        
        // 绘制半透明遮罩
        paint.setColor(Color.parseColor("#60000000"));
        canvas.drawRect(0, 0, width, height, paint);
        
        // 计算播放按钮尺寸
        int playButtonRadius = Math.min(width, height) / 16;
        int centerX = width / 2;
        int centerY = height / 2;
        
        // 绘制圆形背景（带半透明）
        paint.setColor(Color.parseColor("#CCFFFFFF"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, playButtonRadius, paint);
        
        // 绘制播放三角形（红色）
        paint.setColor(Color.parseColor("#FF0000"));
        int triangleSize = playButtonRadius / 2;
        android.graphics.Path path = new android.graphics.Path();
        // 三角形的三个顶点
        path.moveTo(centerX - triangleSize / 2, centerY - triangleSize);
        path.lineTo(centerX - triangleSize / 2, centerY + triangleSize);
        path.lineTo(centerX + triangleSize, centerY);
        path.close();
        canvas.drawPath(path, paint);
        
        return composite;
    }

    /**
     * 开始播放视频
     */
    private void startVideoPlayback(com.fnphoto.tv.MediaItem item) {
        isVideoPlaying = true;
        currentVideoItem = item; // 保存当前视频项

        // 清除预览图
        container.removeAllViews();

        // 创建播放器视图
        playerView = new PlayerView(this);
        playerView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        container.addView(playerView);

        // 构建视频流地址：/p/api/v1/stream/v/{id}
        SharedPreferences prefs = getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
        String baseUrl = prefs.getString("nas_url", "");
        String videoId = item.getId();
        String videoUrl = baseUrl + "/p/api/v1/stream/v/" + videoId;

        Log.d(TAG, "Playing video: " + videoUrl);

        // 初始化播放器 (ExoPlayer 2.11.8 适配 API 19)
        player = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // 创建带认证的 DataSource Factory
        AuthenticatedHttpDataSourceFactory dataSourceFactory =
                new AuthenticatedHttpDataSourceFactory(this, "ExoPlayer");

        // 添加错误监听器，自动处理播放错误
        player.addListener(new Player.EventListener() {
            @Override
            public void onPlayerError(com.google.android.exoplayer2.ExoPlaybackException error) {
                Log.e(TAG, "Player error: " + error.getMessage(), error);
                // 显示错误提示但不崩溃
                android.widget.Toast.makeText(MediaDetailActivity.this,
                        "视频播放失败，请尝试在其他设备上播放",
                        android.widget.Toast.LENGTH_LONG).show();
            }
        });

        // 创建 MediaSource 并播放 (ExoPlayer 2.11.8 API)
        // 使用 Uri 直接创建 MediaSource
        android.net.Uri uri = android.net.Uri.parse(videoUrl);
        ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);
        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
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
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                handleOkKey();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 处理确定键（OK键）事件
     * - 在预览状态：开始播放视频
     * - 在播放状态：暂停/继续播放
     */
    private void handleOkKey() {
        if (player != null && isVideoPlaying) {
            // 已在播放状态，切换暂停/播放
            boolean isPlaying = player.getPlayWhenReady();
            player.setPlayWhenReady(!isPlaying);
            String message = isPlaying ? "已暂停" : "继续播放";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } else if (currentVideoItem != null && !isVideoPlaying) {
            // 在预览状态，开始播放
            startVideoPlayback(currentVideoItem);
        }
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
