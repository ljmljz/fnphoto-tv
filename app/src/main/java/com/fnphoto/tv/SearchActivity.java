package com.fnphoto.tv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
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
        rootLayout.setBackgroundColor(Color.parseColor("#0B0C0F"));

        // 顶部标题区
        TextView tvTitle = new TextView(this);
        tvTitle.setText("搜索");
        tvTitle.setTextColor(Color.parseColor("#F2F4F8"));
        tvTitle.setTextSize(34);
        tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvTitle.setLetterSpacing(0.01f);
        tvTitle.setPadding(dp(64), dp(40), dp(64), 0);
        rootLayout.addView(tvTitle);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText("按文件名在相册中查找");
        tvSubtitle.setTextColor(Color.parseColor("#A8AFBF"));
        tvSubtitle.setTextSize(14);
        tvSubtitle.setPadding(dp(64), dp(8), dp(64), 0);
        rootLayout.addView(tvSubtitle);

        // 胶囊搜索框
        editSearch = new EditText(this);
        editSearch.setHint("输入文件名，回车确认");
        editSearch.setTextColor(Color.parseColor("#F2F4F8"));
        editSearch.setHintTextColor(Color.parseColor("#6E7686"));
        editSearch.setTextSize(17);
        editSearch.setSingleLine(true);
        editSearch.setBackgroundResource(R.drawable.bg_search_field);
        editSearch.setPadding(dp(24), 0, dp(24), 0);
        editSearch.setIncludeFontPadding(false);
        editSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        int searchIcon = dp(16);
        editSearch.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_menu_search, 0, 0, 0);
        editSearch.setCompoundDrawablePadding(dp(12));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        inputLp.topMargin = dp(26);
        inputLp.leftMargin = dp(64);
        inputLp.rightMargin = dp(64);
        editSearch.setLayoutParams(inputLp);
        rootLayout.addView(editSearch);

        // 状态行
        tvStatus = new TextView(this);
        tvStatus.setText("输入关键词开始搜索");
        tvStatus.setTextColor(Color.parseColor("#A8AFBF"));
        tvStatus.setTextSize(13);
        tvStatus.setPadding(dp(72), dp(14), dp(64), dp(6));
        rootLayout.addView(tvStatus);

        // 结果网格
        rvResults = new RecyclerView(this);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        rvResults.setLayoutParams(gridLp);
        rvResults.setClipToPadding(false);
        rvResults.setPadding(dp(48), dp(8), dp(48), dp(40));
        rvResults.setLayoutManager(new GridLayoutManager(this, GRID_SPAN));

        adapter = new SearchAdapter();
        rvResults.setAdapter(adapter);
        rootLayout.addView(rvResults);

        setContentView(rootLayout);

        // 初始不弹软键盘：按 OK / 回车再唤起，避免遮挡画面
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            editSearch.setShowSoftInputOnFocus(false);
        }
        editSearch.postDelayed(new Runnable() {
            @Override
            public void run() {
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager)
                                getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && editSearch.getWindowToken() != null) {
                    imm.hideSoftInputFromWindow(editSearch.getWindowToken(), 0);
                }
            }
        }, 400);
        editSearch.setOnClickListener(v -> showKeyboard());
        editSearch.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                boolean isOk = keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                        || keyCode == KeyEvent.KEYCODE_ENTER;
                if (isOk && event.getAction() == KeyEvent.ACTION_UP) {
                    showKeyboard();
                    return true;
                }
                return false;
            }
        });

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

    private void showKeyboard() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            editSearch.setShowSoftInputOnFocus(true);
        }
        editSearch.requestFocus();
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(editSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
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
