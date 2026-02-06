package com.fnphoto.tv;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.*;

import com.fnphoto.tv.api.FnAuthUtils;
import com.fnphoto.tv.api.FnHttpApi;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainFragment extends BrowseSupportFragment {
    private static final String TAG = "MainFragment";

    private FnHttpApi api;
    private String token;
    private String baseUrl;
    private ArrayObjectAdapter mRowsAdapter;
    private CardPresenter mCardPresenter;
    private List<FnHttpApi.TimelineItem> timelineItems;  // 保存时间线数据
    private boolean isPhotoListView = false;  // 标记当前是否在照片列表视图
    private List<MediaItem> currentMediaList;  // 当前显示的媒体列表（用于左右切换）

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 从参数获取
        if (getArguments() != null) {
            baseUrl = getArguments().getString("nas_url", "");
            token = getArguments().getString("api_token", "");
        }

        // 初始化 UI
        setupUI();
        
        // 初始化 API
        if (baseUrl != null && !baseUrl.isEmpty()) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl + "/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            api = retrofit.create(FnHttpApi.class);
        }

        // 设置事件监听
        setupEventListeners();
    }

    private void setupUI() {
        // 设置标题
        setTitle("飞牛相册");
        
        // 设置 HeadersState - 禁用左侧标题栏
        setHeadersState(BrowseSupportFragment.HEADERS_DISABLED);
        
        // 设置品牌颜色
        setBrandColor(getResources().getColor(android.R.color.black, null));
        
        // 设置搜索颜色
        setSearchAffordanceColor(getResources().getColor(android.R.color.white, null));
        
        // 初始化 Adapter
        mCardPresenter = new CardPresenter();
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                      RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (item instanceof MediaItem) {
                    MediaItem mediaItem = (MediaItem) item;
                    
                    if ("date".equals(mediaItem.getType())) {
                        // 点击日期，加载该日期的照片
                        loadPhotosByDate(mediaItem.getDateStr(), mediaItem.getPhotoCount());
                    } else if ("video".equals(mediaItem.getType()) || "photo".equals(mediaItem.getType())) {
                        // 查看照片或视频，支持左右切换
                        openMediaDetail(mediaItem);
                    }
                }
            }
        });

        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
                // 选中项变化时的处理
            }
        });
    }
    
    /**
     * 处理返回键，如果在照片列表视图则返回时间线
     * @return true 如果处理了返回键，false 否则
     */
    public boolean onBackPressed() {
        if (isPhotoListView && timelineItems != null) {
            // 在照片列表视图，返回到时间线
            Log.d(TAG, "Returning to timeline");
            displayTimeline(timelineItems);
            return true;
        }
        return false;
    }

    public void loadTimeline() {
        if (api == null || token == null || token.isEmpty()) {
            Log.e(TAG, "API未初始化");
            return;
        }

        String authx = FnAuthUtils.generateAuthX("/p/api/v1/gallery/timeline", "GET", null);

        api.getTimeline(token, authx).enqueue(new Callback<FnHttpApi.TimelineResponse>() {
            @Override
            public void onResponse(Call<FnHttpApi.TimelineResponse> call, 
                                   Response<FnHttpApi.TimelineResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FnHttpApi.TimelineResponse result = response.body();
                    if (result.code == 0 && result.data != null && result.data.list != null) {
                        displayTimeline(result.data.list);
                    }
                }
            }

            @Override
            public void onFailure(Call<FnHttpApi.TimelineResponse> call, Throwable t) {
                Log.e(TAG, "加载时间线失败", t);
            }
        });
    }

    private void displayTimeline(List<FnHttpApi.TimelineItem> items) {
        // 保存时间线数据以便返回
        timelineItems = items;
        isPhotoListView = false;
        
        mRowsAdapter.clear();
        
        // 按年月分组
        String currentYearMonth = "";
        ArrayObjectAdapter currentRowAdapter = null;
        
        for (FnHttpApi.TimelineItem item : items) {
            String yearMonth = item.year + "年" + item.month + "月";
            String dateStr = item.year + "-" + String.format("%02d", item.month) + "-" + String.format("%02d", item.day);
            
            if (!yearMonth.equals(currentYearMonth)) {
                // 新月份，创建新行
                currentYearMonth = yearMonth;
                HeaderItem header = new HeaderItem(yearMonth);
                currentRowAdapter = new ArrayObjectAdapter(mCardPresenter);
                mRowsAdapter.add(new ListRow(header, currentRowAdapter));
            }
            
            // 添加日期卡片
            MediaItem mediaItem = new MediaItem(
                dateStr,
                item.day + "日 (" + item.itemCount + "张)",
                item.itemCount
            );
            currentRowAdapter.add(mediaItem);
        }
    }

    public void loadFolders() {
        if (api == null || token == null || token.isEmpty()) {
            Log.e(TAG, "API未初始化");
            return;
        }

        String authx = FnAuthUtils.generateAuthX("/p/api/v1/photo/folder/view", "GET", null);

        api.getManagedFolders(token, authx).enqueue(new Callback<FnHttpApi.FolderViewResponse>() {
            @Override
            public void onResponse(Call<FnHttpApi.FolderViewResponse> call,
                                   Response<FnHttpApi.FolderViewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FnHttpApi.FolderViewResponse result = response.body();
                    if (result.data != null && result.data.folders != null) {
                        displayFolders(result.data.folders);
                    }
                }
            }

            @Override
            public void onFailure(Call<FnHttpApi.FolderViewResponse> call, Throwable t) {
                Log.e(TAG, "加载文件夹失败", t);
            }
        });
    }

    private void displayFolders(List<FnHttpApi.ManagedFolder> folders) {
        isPhotoListView = false;
        timelineItems = null;  // 清空时间线数据
        mRowsAdapter.clear();
        
        HeaderItem header = new HeaderItem("文件夹");
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(mCardPresenter);
        
        for (FnHttpApi.ManagedFolder folder : folders) {
            MediaItem item = new MediaItem(
                folder.id,
                folder.name,
                "folder",
                null,
                null
            );
            listRowAdapter.add(item);
        }
        
        mRowsAdapter.add(new ListRow(header, listRowAdapter));
    }

    public void loadAlbums() {
        if (api == null || token == null || token.isEmpty()) {
            Log.e(TAG, "API未初始化");
            return;
        }

        String authx = FnAuthUtils.generateAuthX("/api/v1/photos/albums", "GET", null);

        api.getAlbums(token, authx).enqueue(new Callback<FnHttpApi.AlbumResponse>() {
            @Override
            public void onResponse(Call<FnHttpApi.AlbumResponse> call,
                                   Response<FnHttpApi.AlbumResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FnHttpApi.AlbumResponse result = response.body();
                    if (result.data != null && result.data.albums != null) {
                        displayAlbums(result.data.albums);
                    }
                }
            }

            @Override
            public void onFailure(Call<FnHttpApi.AlbumResponse> call, Throwable t) {
                Log.e(TAG, "加载相册失败", t);
            }
        });
    }

    private void displayAlbums(List<FnHttpApi.Album> albums) {
        isPhotoListView = false;
        timelineItems = null;  // 清空时间线数据
        mRowsAdapter.clear();
        
        HeaderItem header = new HeaderItem("相册");
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(mCardPresenter);
        
        for (FnHttpApi.Album album : albums) {
            String coverUrl = album.cover != null ? baseUrl + album.cover : null;
            MediaItem item = new MediaItem(
                album.id,
                album.name,
                "album",
                coverUrl,
                coverUrl
            );
            listRowAdapter.add(item);
        }
        
        mRowsAdapter.add(new ListRow(header, listRowAdapter));
    }

    public void loadPhotosByDate(String dateStr, int itemCount) {
        if (api == null || token == null || token.isEmpty()) {
            Log.e(TAG, "API未初始化");
            return;
        }

        Log.d(TAG, "Loading photos for date: " + dateStr);

        // 构建参数值
        String dateTime = dateStr.replace("-", ":");
        String startTime = dateTime + " 00:00:00";
        String endTime = dateTime + " 23:59:59";
        int limit = itemCount;
        int offset = 0;
        String mode = "index";

        // 构建参数字符串用于生成 authx (按key字母顺序排序)
        // end_time, limit, mode, offset, start_time
        StringBuilder paramsBuilder = new StringBuilder();
        paramsBuilder.append("end_time=").append(endTime);
        paramsBuilder.append("&limit=").append(limit);
        paramsBuilder.append("&mode=").append(mode);
        paramsBuilder.append("&offset=").append(offset);
        paramsBuilder.append("&start_time=").append(startTime);
        
        String params = paramsBuilder.toString();
        String path = "/p/api/v1/gallery/getList";
        
        Log.d(TAG, "Path: " + path);
        Log.d(TAG, "Params: " + params);

        // 生成认证头 - 传入路径和参数
        String authx = FnAuthUtils.generateAuthX(path, "GET", params);

        // GET 请求使用 @Query 参数
        api.getPhotosByTimeRange(token, authx, startTime, endTime, limit, offset, mode)
            .enqueue(new Callback<FnHttpApi.GalleryListResponse>() {
                @Override
                public void onResponse(Call<FnHttpApi.GalleryListResponse> call,
                                       Response<FnHttpApi.GalleryListResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        FnHttpApi.GalleryListResponse result = response.body();
                        if (result.code == 0 && result.data != null && result.data.list != null) {
                            displayPhotosByDate(dateStr, result.data.list);
                        }
                    }
                }

                @Override
                public void onFailure(Call<FnHttpApi.GalleryListResponse> call, Throwable t) {
                    Log.e(TAG, "加载照片列表失败", t);
                }
            });
    }

    private void displayPhotosByDate(String dateStr, List<FnHttpApi.GalleryPhoto> photos) {
        isPhotoListView = true;
        mRowsAdapter.clear();

        HeaderItem header = new HeaderItem(dateStr + " (" + photos.size() + "张)");
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(mCardPresenter);

        // 创建媒体列表
        currentMediaList = new java.util.ArrayList<>();

        for (FnHttpApi.GalleryPhoto photo : photos) {
            String thumbUrl = null;
            String originalUrl = null;

            if (photo.additional != null && photo.additional.thumbnail != null) {
                FnHttpApi.GalleryThumbnail thumbnail = photo.additional.thumbnail;
                // 使用中等尺寸缩略图
                thumbUrl = thumbnail.sUrl != null ? baseUrl + thumbnail.sUrl : null;
                // 原图链接
                originalUrl = thumbnail.mUrl != null ? baseUrl + thumbnail.mUrl : null;
            }

            MediaItem item = new MediaItem(
                String.valueOf(photo.id),
                photo.fileName,
                photo.category,  // "photo" or "video"
                thumbUrl,
                originalUrl
            );
            currentMediaList.add(item);
            listRowAdapter.add(item);
        }

        mRowsAdapter.add(new ListRow(header, listRowAdapter));
    }

    private void openMediaDetail(MediaItem mediaItem) {
        if (currentMediaList == null || currentMediaList.isEmpty()) {
            // 如果列表为空，创建一个只包含当前项的列表
            currentMediaList = new java.util.ArrayList<>();
            currentMediaList.add(mediaItem);
        }

        // 找到当前项在列表中的索引（通过ID匹配）
        int index = 0;
        for (int i = 0; i < currentMediaList.size(); i++) {
            if (currentMediaList.get(i).getId().equals(mediaItem.getId())) {
                index = i;
                break;
            }
        }

        // 启动详情页面
        Intent intent = new Intent(getActivity(), MediaDetailActivity.class);
        intent.putExtra("MEDIA_LIST", new java.util.ArrayList<>(currentMediaList));
        intent.putExtra("CURRENT_INDEX", index);
        startActivity(intent);
    }
}
