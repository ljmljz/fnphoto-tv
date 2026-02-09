package com.fnphoto.tv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fnphoto.tv.api.FnAuthUtils;
import com.fnphoto.tv.api.FnHttpApi;
import com.fnphoto.tv.cache.CachedImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 文件夹浏览Activity
 * 支持混合视图：文件夹列表 + 照片墙
 */
public class FolderBrowseActivity extends FragmentActivity {
    private static final String TAG = "FolderBrowseActivity";
    private static final int GRID_SPAN_COUNT = 4; // 照片墙每行显示4个
    private static final int FOLDER_GRID_SPAN_COUNT = 2; // 文件夹每行显示2个
    private static final int PAGE_LIMIT = 100; // 每页加载数量

    private TextView tvTitle;
    private RecyclerView rvFolders;
    private RecyclerView rvPhotos;
    private LinearLayout layoutContent;
    private TextView tvLoading;

    private FnHttpApi api;
    private String token;
    private String baseUrl;
    private String currentFolderPath;
    private String currentFolderName;

    private FolderAdapter folderAdapter;
    private PhotoGridAdapter photoAdapter;

    private List<FnHttpApi.SubFolderItem> folderList = new ArrayList<>();
    private List<FnHttpApi.FolderMediaItem> photoList = new ArrayList<>();

    private int loadedCount = 0;
    private boolean hasMorePhotos = true;
    private boolean isLoading = false;
    private AtomicInteger pendingRequests = new AtomicInteger(2); // 两个请求待完成

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置全屏模式
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 获取传递的参数
        currentFolderPath = getIntent().getStringExtra("FOLDER_PATH");
        currentFolderName = getIntent().getStringExtra("FOLDER_NAME");

        if (currentFolderPath == null) {
            Toast.makeText(this, "文件夹路径无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化视图
        initViews();

        // 初始化API
        initApi();

        // 加载文件夹内容
        loadFolderContent();
    }

    private void initViews() {
        // 创建根布局
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.setBackgroundColor(Color.parseColor("#0a0a0c"));

        // 标题栏
        tvTitle = new TextView(this);
        tvTitle.setText(currentFolderName != null ? currentFolderName : "浏览文件夹");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(24);
        tvTitle.setPadding(32, 24, 32, 16);
        rootLayout.addView(tvTitle);

        // 加载提示
        tvLoading = new TextView(this);
        tvLoading.setText("加载中...");
        tvLoading.setTextColor(Color.parseColor("#9ca3af"));
        tvLoading.setTextSize(18);
        tvLoading.setPadding(32, 16, 32, 16);
        rootLayout.addView(tvLoading);

        // 内容区域
        layoutContent = new LinearLayout(this);
        layoutContent.setOrientation(LinearLayout.VERTICAL);
        layoutContent.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // 文件夹列表区域
        TextView tvFoldersTitle = new TextView(this);
        tvFoldersTitle.setText("子文件夹");
        tvFoldersTitle.setTextColor(Color.parseColor("#9ca3af"));
        tvFoldersTitle.setTextSize(18);
        tvFoldersTitle.setPadding(32, 16, 32, 8);
        tvFoldersTitle.setVisibility(View.GONE);
        tvFoldersTitle.setTag("folders_title");
        layoutContent.addView(tvFoldersTitle);

        rvFolders = new RecyclerView(this);
        rvFolders.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        rvFolders.setPadding(16, 8, 16, 8);
        rvFolders.setVisibility(View.GONE);
        folderAdapter = new FolderAdapter();
        rvFolders.setAdapter(folderAdapter);
        layoutContent.addView(rvFolders);

        // 照片墙区域
        TextView tvPhotosTitle = new TextView(this);
        tvPhotosTitle.setText("照片和视频");
        tvPhotosTitle.setTextColor(Color.parseColor("#9ca3af"));
        tvPhotosTitle.setTextSize(18);
        tvPhotosTitle.setPadding(32, 16, 32, 8);
        tvPhotosTitle.setVisibility(View.GONE);
        tvPhotosTitle.setTag("photos_title");
        layoutContent.addView(tvPhotosTitle);

        rvPhotos = new RecyclerView(this);
        rvPhotos.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        rvPhotos.setPadding(16, 8, 16, 16);
        rvPhotos.setVisibility(View.GONE);
        photoAdapter = new PhotoGridAdapter();
        rvPhotos.setAdapter(photoAdapter);
        layoutContent.addView(rvPhotos);

        rootLayout.addView(layoutContent);
        setContentView(rootLayout);
    }

    private void initApi() {
        SharedPreferences prefs = getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
        baseUrl = prefs.getString("nas_url", "");
        token = prefs.getString("api_token", "");

        if (baseUrl.isEmpty() || token.isEmpty()) {
            Toast.makeText(this, "未登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(FnHttpApi.class);
    }

    private void loadFolderContent() {
        isLoading = true;
        pendingRequests.set(2);

        // 并行加载子文件夹和媒体文件
        loadSubFolders();
        loadMediaFiles(0);
    }

    private void loadSubFolders() {
        String params = "desc=false&orderBy=2&folderPath=" + currentFolderPath;
        String authx = FnAuthUtils.generateAuthX("/p/api/v1/folder_view/getFolderList", "GET", params);

        api.getSubFolders(token, authx, currentFolderPath, false, 2)
                .enqueue(new Callback<FnHttpApi.SubFolderListResponse>() {
                    @Override
                    public void onResponse(Call<FnHttpApi.SubFolderListResponse> call,
                                           Response<FnHttpApi.SubFolderListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            FnHttpApi.SubFolderListResponse result = response.body();
                            if (result.code == 0 && result.data != null && result.data.list != null) {
                                folderList.clear();
                                folderList.addAll(result.data.list);
                                updateFoldersUI();
                            }
                        } else {
                            Log.e(TAG, "加载子文件夹失败: HTTP " + response.code());
                        }
                        checkLoadingComplete();
                    }

                    @Override
                    public void onFailure(Call<FnHttpApi.SubFolderListResponse> call, Throwable t) {
                        Log.e(TAG, "加载子文件夹失败", t);
                        checkLoadingComplete();
                    }
                });
    }

    private void loadMediaFiles(int offset) {
        String params = "folderPath=" + currentFolderPath + "&desc=false&orderBy=2&limit=" + PAGE_LIMIT + "&offset=" + offset;
        String authx = FnAuthUtils.generateAuthX("/p/api/v1/folder_view/getFileList", "GET", params);

        api.getFolderFiles(token, authx, currentFolderPath, false, 2, PAGE_LIMIT, offset)
                .enqueue(new Callback<FnHttpApi.FolderFileListResponse>() {
                    @Override
                    public void onResponse(Call<FnHttpApi.FolderFileListResponse> call,
                                           Response<FnHttpApi.FolderFileListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            FnHttpApi.FolderFileListResponse result = response.body();
                            if (result.code == 0 && result.data != null && result.data.list != null) {
                                photoList.addAll(result.data.list);
                                loadedCount += result.data.list.size();
                                hasMorePhotos = result.data.list.size() >= PAGE_LIMIT;
                                updatePhotosUI();
                            }
                        } else {
                            Log.e(TAG, "加载媒体文件失败: HTTP " + response.code());
                        }
                        checkLoadingComplete();
                    }

                    @Override
                    public void onFailure(Call<FnHttpApi.FolderFileListResponse> call, Throwable t) {
                        Log.e(TAG, "加载媒体文件失败", t);
                        checkLoadingComplete();
                    }
                });
    }

    private void checkLoadingComplete() {
        int remaining = pendingRequests.decrementAndGet();
        if (remaining <= 0) {
            isLoading = false;
            tvLoading.setVisibility(View.GONE);

            if (folderList.isEmpty() && photoList.isEmpty()) {
                Toast.makeText(this, "该文件夹为空", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateFoldersUI() {
        if (!folderList.isEmpty()) {
            findViewWithTag("folders_title").setVisibility(View.VISIBLE);
            rvFolders.setVisibility(View.VISIBLE);
            folderAdapter.notifyDataSetChanged();

            rvFolders.setLayoutManager(new GridLayoutManager(this, FOLDER_GRID_SPAN_COUNT));
            rvFolders.setHasFixedSize(true);
        }
    }

    private void updatePhotosUI() {
        if (!photoList.isEmpty()) {
            findViewWithTag("photos_title").setVisibility(View.VISIBLE);
            rvPhotos.setVisibility(View.VISIBLE);
            photoAdapter.notifyDataSetChanged();

            rvPhotos.setLayoutManager(new GridLayoutManager(this, GRID_SPAN_COUNT));
            rvPhotos.setHasFixedSize(true);
        }
    }

    private View findViewWithTag(String tag) {
        for (int i = 0; i < layoutContent.getChildCount(); i++) {
            View child = layoutContent.getChildAt(i);
            if (tag.equals(child.getTag())) {
                return child;
            }
        }
        return null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ========== 文件夹适配器 ==========
    private class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_folder_preview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FnHttpApi.SubFolderItem folder = folderList.get(position);
            holder.bind(folder);
        }

        @Override
        public int getItemCount() {
            return folderList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPreview;
            TextView tvName;
            TextView tvCount;

            ViewHolder(View itemView) {
                super(itemView);
                ivPreview = itemView.findViewById(R.id.iv_folder_preview);
                tvName = itemView.findViewById(R.id.tv_folder_name);
                tvCount = itemView.findViewById(R.id.tv_folder_count);

                itemView.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        FnHttpApi.SubFolderItem folder = folderList.get(pos);
                        Intent intent = new Intent(FolderBrowseActivity.this, FolderBrowseActivity.class);
                        intent.putExtra("FOLDER_PATH", folder.path);
                        intent.putExtra("FOLDER_NAME", folder.name);
                        startActivity(intent);
                    }
                });

                itemView.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        v.setBackgroundResource(R.drawable.menu_item_focused);
                    } else {
                        v.setBackgroundResource(0);
                    }
                });
            }

            void bind(FnHttpApi.SubFolderItem folder) {
                tvName.setText(folder.name);
                tvCount.setText("文件夹");
                ivPreview.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
    }

    // ========== 照片网格适配器 ==========
    private class PhotoGridAdapter extends RecyclerView.Adapter<PhotoGridAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_photo_grid, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FnHttpApi.FolderMediaItem item = photoList.get(position);
            holder.bind(item);

            if (position == photoList.size() - 5 && hasMorePhotos && !isLoading) {
                loadMediaFiles(loadedCount);
            }
        }

        @Override
        public int getItemCount() {
            return photoList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPhoto;
            View vVideoIndicator;

            ViewHolder(View itemView) {
                super(itemView);
                ivPhoto = itemView.findViewById(R.id.iv_photo);
                vVideoIndicator = itemView.findViewById(R.id.v_video_indicator);

                itemView.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        openMediaDetail(pos);
                    }
                });

                itemView.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        v.setBackgroundResource(R.drawable.menu_item_focused);
                    } else {
                        v.setBackgroundResource(0);
                    }
                });
            }

            void bind(FnHttpApi.FolderMediaItem item) {
                String thumbUrl = baseUrl + "/p/api/v1/stream/p/t/" + item.id + "/s/" + item.photoUUID;

                CachedImageLoader.loadImage(FolderBrowseActivity.this, thumbUrl, token,
                        ivPhoto.getWidth(), ivPhoto.getHeight(),
                        new CachedImageLoader.ImageLoadCallback() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap) {
                                ivPhoto.setImageBitmap(bitmap);
                            }

                            @Override
                            public void onLoadFailed() {
                                ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery);
                            }
                        });

                vVideoIndicator.setVisibility("video".equals(item.category) ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void openMediaDetail(int position) {
        List<MediaItem> mediaItems = new ArrayList<>();
        for (FnHttpApi.FolderMediaItem item : photoList) {
            String thumbUrl = baseUrl + "/p/api/v1/stream/p/t/" + item.id + "/s/" + item.photoUUID;
            String mediaUrl;
            if ("video".equals(item.category)) {
                mediaUrl = baseUrl + "/p/api/v1/stream/v/" + item.id;
            } else {
                mediaUrl = baseUrl + "/p/api/v1/stream/p/t/" + item.id + "/o/" + item.photoUUID;
            }

            MediaItem mediaItem = new MediaItem(
                    String.valueOf(item.id),
                    item.fileName,
                    item.category,
                    thumbUrl,
                    mediaUrl
            );
            mediaItems.add(mediaItem);
        }

        Intent intent = new Intent(this, MediaDetailActivity.class);
        intent.putExtra("MEDIA_LIST", new ArrayList<>(mediaItems));
        intent.putExtra("CURRENT_INDEX", position);
        startActivity(intent);
    }
}
