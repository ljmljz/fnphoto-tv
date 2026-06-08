package com.fnphoto.tv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fnphoto.tv.api.FnAuthUtils;
import com.fnphoto.tv.api.HttpClientProvider;
import com.fnphoto.tv.api.FnHttpApi;
import com.fnphoto.tv.cache.CachedImageLoader;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SearchActivity extends FragmentActivity {
    private static final String TAG = "SearchActivity";
    private static final int GRID_SPAN = 6;
    private static final int SEARCH_DELAY = 500;
    private static final int PAGE_LIMIT = 100;

    private EditText editSearch;
    private RecyclerView rvResults;
    private TextView tvStatus;
    private SearchAdapter adapter;

    private FnHttpApi api;
    private String token;
    private String baseUrl;
    private List<FnHttpApi.GalleryPhoto> searchResults = new ArrayList<>();
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

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
                .client(HttpClientProvider.getClient(this))
                .build();
        api = retrofit.create(FnHttpApi.class);

        initViews();

        String extraQuery = getIntent().getStringExtra("EXTRA_QUERY");
        if (extraQuery != null && !extraQuery.isEmpty()) {
            editSearch.setText(extraQuery);
            performSearch(extraQuery);
        }
    }

    private void initViews() {
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.setBackgroundColor(Color.parseColor("#0a0a0c"));

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("搜索照片");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(28);
        tvTitle.setPadding(48, 32, 48, 16);
        rootLayout.addView(tvTitle);

        // Search input
        editSearch = new EditText(this);
        editSearch.setHint("输入文件名搜索...");
        editSearch.setTextColor(Color.WHITE);
        editSearch.setHintTextColor(Color.parseColor("#666666"));
        editSearch.setTextSize(20);
        editSearch.setPadding(48, 16, 48, 16);
        editSearch.setBackgroundColor(Color.parseColor("#1a1a1e"));
        editSearch.setSingleLine(true);
        rootLayout.addView(editSearch);

        // Status text
        tvStatus = new TextView(this);
        tvStatus.setText("输入关键词开始搜索");
        tvStatus.setTextColor(Color.parseColor("#9ca3af"));
        tvStatus.setTextSize(16);
        tvStatus.setPadding(48, 12, 48, 8);
        rootLayout.addView(tvStatus);

        // Results grid
        rvResults = new RecyclerView(this);
        rvResults.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rvResults.setPadding(32, 8, 32, 32);
        rvResults.setLayoutManager(new GridLayoutManager(this, GRID_SPAN));

        adapter = new SearchAdapter();
        rvResults.setAdapter(adapter);
        rootLayout.addView(rvResults);

        setContentView(rootLayout);

        // Auto-focus search input
        editSearch.requestFocus();

        // Search on text change with debounce
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacksAndMessages(null);
                String query = s.toString().trim();
                if (query.length() >= 1) {
                    searchHandler.postDelayed(() -> performSearch(query), SEARCH_DELAY);
                } else {
                    searchResults.clear();
                    adapter.notifyDataSetChanged();
                    tvStatus.setText("输入关键词开始搜索");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String keyword) {
        currentQuery = keyword;
        tvStatus.setText("搜索中...");

        if (api == null) return;

        String params = "keyword=" + keyword + "&limit=" + PAGE_LIMIT + "&offset=0";
        String authx = FnAuthUtils.generateAuthX("/p/api/v1/photo/search", "GET", params);

        api.searchPhotos(token, authx, keyword, PAGE_LIMIT, 0)
                .enqueue(new Callback<FnHttpApi.GalleryListResponse>() {
                    @Override
                    public void onResponse(Call<FnHttpApi.GalleryListResponse> call,
                                           Response<FnHttpApi.GalleryListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            FnHttpApi.GalleryListResponse result = response.body();
                            if (result.code == 0 && result.data != null && result.data.list != null) {
                                searchResults = result.data.list;
                                adapter.notifyDataSetChanged();
                                tvStatus.setText("找到 " + searchResults.size() + " 个结果");
                            } else {
                                tvStatus.setText("搜索失败: " + result.msg);
                            }
                        } else {
                            tvStatus.setText("搜索失败: HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<FnHttpApi.GalleryListResponse> call, Throwable t) {
                        Log.e(TAG, "搜索失败", t);
                        tvStatus.setText("搜索失败: " + t.getMessage());
                    }
                });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && editSearch.isFocused()) {
            rvResults.requestFocus();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ========== Search Results Adapter ==========
    private class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(SearchActivity.this);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(320, 240));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(4, 4, 4, 4);
            imageView.setBackgroundColor(Color.parseColor("#333333"));
            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FnHttpApi.GalleryPhoto photo = searchResults.get(position);
            holder.bind(photo, position);
        }

        @Override
        public int getItemCount() {
            return searchResults.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPhoto;

            ViewHolder(ImageView itemView) {
                super(itemView);
                ivPhoto = itemView;

                itemView.setOnFocusChangeListener((v, hasFocus) -> {
                    v.setAlpha(hasFocus ? 1.0f : 0.7f);
                });

                itemView.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        openMediaDetail(pos);
                    }
                });
            }

            void bind(FnHttpApi.GalleryPhoto photo, int position) {
                String thumbUrl = null;
                if (photo.additional != null && photo.additional.thumbnail != null) {
                    String path = photo.additional.thumbnail.sUrl;
                    if (path != null) {
                        thumbUrl = path.startsWith("http") ? path : baseUrl + path;
                    }
                }

                if (thumbUrl != null) {
                    final String url = thumbUrl;
                    CachedImageLoader.loadIntoImageView(ivPhoto, url, token);
                }

                // Show video indicator
                if ("video".equals(photo.category)) {
                    ivPhoto.setBackgroundColor(Color.parseColor("#44000000"));
                }
            }
        }
    }

    private void openMediaDetail(int position) {
        FnHttpApi.GalleryPhoto photo = searchResults.get(position);
        List<MediaItem> mediaItems = new ArrayList<>();

        String thumbUrl = null;
        String mediaUrl = null;
        if (photo.additional != null && photo.additional.thumbnail != null) {
            FnHttpApi.GalleryThumbnail thumb = photo.additional.thumbnail;
            thumbUrl = thumb.sUrl != null ? baseUrl + thumb.sUrl : null;
            if ("video".equals(photo.category)) {
                mediaUrl = baseUrl + "/p/api/v1/stream/v/" + photo.id;
            } else {
                mediaUrl = thumb.originalUrl != null ? baseUrl + thumb.originalUrl : (thumb.mUrl != null ? baseUrl + thumb.mUrl : null);
            }
        }

        MediaItem item = new MediaItem(
                String.valueOf(photo.id),
                photo.fileName,
                photo.category,
                thumbUrl,
                mediaUrl
        );
        mediaItems.add(item);

        MediaListHolder.set(mediaItems);
        Intent intent = new Intent(this, MediaDetailActivity.class);
        intent.putExtra("CURRENT_INDEX", 0);
        startActivity(intent);
    }
}
