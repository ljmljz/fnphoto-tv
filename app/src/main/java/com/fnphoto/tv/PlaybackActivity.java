package com.fnphoto.tv;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import androidx.fragment.app.FragmentActivity;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;

public class PlaybackActivity extends FragmentActivity {
    private SimpleExoPlayer player;
    private PlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置全屏模式，隐藏状态栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_playback);

        playerView = findViewById(R.id.player_view);
        player = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        String videoUrl = getIntent().getStringExtra("VIDEO_URL");
        if (videoUrl != null) {
            MediaItem mediaItem = MediaItem.fromUri(videoUrl);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }
}
