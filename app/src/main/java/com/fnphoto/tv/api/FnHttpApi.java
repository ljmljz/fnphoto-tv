package com.fnphoto.tv.api;

import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;
import java.util.Map;

/**
 * 飞牛NAS相册系统 HTTP API 接口
 * 所有接口都需要 token + authx 签名认证
 * 
 * 认证头格式:
 *   accesstoken: <登录token>
 *   authx: nonce=<随机数>&timestamp=<时间戳>&sign=<HMAC-SHA256签名>
 */
public interface FnHttpApi {
    
    // ==================== 基础相册接口 ====================
    
    /**
     * 获取相册列表
     * GET /api/v1/photos/albums
     */
    @GET("/api/v1/photos/albums")
    Call<AlbumResponse> getAlbums(
        @Header("accesstoken") String accesstoken,
        @Header("authx") String authx
    );
    
    /**
     * 获取相册内的照片列表
     * GET /api/v1/photos/album/{albumId}
     */
    @GET("/api/v1/photos/album/{albumId}")
    Call<PhotoListResponse> getPhotosByAlbum(
        @Header("accesstoken") String accesstoken,
        @Header("authx") String authx,
        @Path("albumId") String albumId
    );
    
    // ==================== 上传相关接口 ====================
    
    /**
     * 获取照片上传临时文件夹路径
     * GET /p/api/v2/photo/upload/path
     * 用于获取服务器端临时存储路径
     */
    @GET("/p/api/v2/photo/upload/path")
    Call<UploadPathResponse> getUploadPath(
        @Header("accesstoken") String accesstoken,
        @Header("authx") String authx
    );
    
    /**
     * 获取支持的文件类型列表
     * GET /p/api/v1/photo/support/list
     * 返回服务器支持上传的照片/视频格式
     */
    @GET("/p/api/v1/photo/support/list")
    Call<SupportTypeResponse> getSupportedTypes(
        @Header("accesstoken") String accesstoken,
        @Header("authx") String authx
    );
    
    // ==================== 存储管理接口 ====================
    
    /**
     * 存储管理信息
     * GET /p/api/v1/server/folder_manage
     * 获取NAS存储空间使用情况、文件夹管理等
     */
    @GET("/p/api/v1/server/folder_manage")
    Call<StorageResponse> getStorageInfo(
        @Header("accesstoken") String accesstoken,
        @Header("authx") String authx
    );
    
    /**
     * 管理的文件夹视图
     * GET /p/api/v1/photo/folder/view
     * 获取相册系统管理的文件夹列表
     */
    @GET("/p/api/v1/photo/folder/view")
    Call<FolderViewResponse> getManagedFolders(
        @Header("accesstoken") String accesstoken,
        @Header("authx") String authx
    );
    
    // ==================== 用户系统接口 ====================
    
    /**
     * 获取所有用户列表
     * GET /p/api/v1/server/users_all
     * 管理员权限：获取系统所有用户信息
     */
    @GET("/p/api/v1/server/users_all")
    Call<UsersResponse> getAllUsers(
        @Header("accesstoken") String accesstoken,
        @Header("authx") String authx
    );
    
    /**
     * 当前用户信息
     * GET /p/api/v1/user/info
     * 获取登录用户的详细信息
     */
    @GET("/p/api/v1/user/info")
    Call<UserInfoResponse> getUserInfo(
        @Header("accesstoken") String accesstoken,
        @Header("authx") String authx
    );
    
    /**
     * 系统信息
     * GET /p/api/v1/server/sys_info
     * 获取NAS系统信息（版本、硬件等）
     */
    @GET("/p/api/v1/server/sys_info")
    Call<SysInfoResponse> getSystemInfo(
        @Header("accesstoken") String accesstoken,
        @Header("authx") String authx
    );
    
    // ==================== 相册统计接口 ====================
    
    /**
     * 相册统计信息
     * GET /p/api/v1/user_photo/stat
     * 获取用户相册统计数据（照片数、视频数、占用空间等）
     */
    @GET("/p/api/v1/user_photo/stat")
    Call<PhotoStatsResponse> getPhotoStats(
        @Header("accesstoken") String accesstoken,
        @Header("authx") String authx
    );
    
    /**
     * 相册应用版本
     * GET /p/api/v1/app/version
     * 获取相册应用的版本信息
     */
    @GET("/p/api/v1/app/version")
    Call<AppVersionResponse> getAppVersion(
        @Header("accesstoken") String accesstoken,
        @Header("authx") String authx
    );
    
    // ==================== 时间线和智能相册接口 ====================
    
    /**
     * 照片时间线
     * GET /p/api/v1/gallery/timeline
     * 按时间维度聚合照片，生成时间线视图
     */
    @GET("/p/api/v1/gallery/timeline")
    Call<TimelineResponse> getTimeline(
        @Header("accesstoken") String accesstoken,
        @Header("authx") String authx
    );
    
    /**
     * 按时间范围获取照片列表
     * GET /p/api/v1/gallery/getList
     * 请求参数: start_time, end_time, limit, offset, mode
     */
    @GET("/p/api/v1/gallery/getList")
    Call<GalleryListResponse> getPhotosByTimeRange(
        @Header("accesstoken") String accesstoken,
        @Header("authx") String authx,
        @Query("start_time") String startTime,
        @Query("end_time") String endTime,
        @Query("limit") int limit,
        @Query("offset") int offset,
        @Query("mode") String mode
    );
    
    // ==================== 数据模型类 ====================
    
    /**
     * 通用响应基类
     */
    class BaseResponse {
        public int errno;      // 错误码，0表示成功
        public String result;  // 结果状态: succ/fail
    }
    
    // 相册相关
    class AlbumResponse extends BaseResponse {
        public AlbumData data;
    }
    
    class AlbumData {
        public List<Album> albums;
    }
    
    class Album {
        public String id;          // 相册ID
        public String name;        // 相册名称
        public int count;          // 照片数量
        public String cover;       // 封面照片URL
        public long createTime;    // 创建时间
        public long updateTime;    // 更新时间
    }
    
    // 照片列表
    class PhotoListResponse extends BaseResponse {
        public PhotoListData data;
    }
    
    class PhotoListData {
        public List<Photo> photos;
        public int total;        // 总数量
        public boolean hasMore;    // 是否有更多
    }
    
    class Photo {
        public String id;          // 照片ID
        public String name;        // 文件名
        public String thumb;       // 缩略图URL
        public String url;         // 原图URL
        public long size;          // 文件大小
        public long createTime;    // 创建时间
        public long modifyTime;    // 修改时间
        public String type;        // 类型: image/video
        public int width;          // 宽度
        public int height;         // 高度
    }
    
    // 上传相关
    class UploadPathResponse extends BaseResponse {
        public UploadPathData data;
    }
    
    class UploadPathData {
        public String path;        // 临时上传路径
        public long expireTime;    // 过期时间
    }
    
    class SupportTypeResponse extends BaseResponse {
        public SupportTypeData data;
    }
    
    class SupportTypeData {
        public List<String> imageTypes;  // 支持的图片格式: ["jpg", "png", ...]
        public List<String> videoTypes;  // 支持的视频格式: ["mp4", "mov", ...]
        public long maxFileSize;         // 最大文件大小限制
    }
    
    // 存储管理
    class StorageResponse extends BaseResponse {
        public StorageData data;
    }
    
    class StorageData {
        public long totalSpace;     // 总空间 (字节)
        public long usedSpace;      // 已用空间
        public long freeSpace;      // 剩余空间
        public List<FolderInfo> folders;
    }
    
    class FolderInfo {
        public String id;
        public String name;
        public String path;
        public long size;
        public int fileCount;
    }
    
    class FolderViewResponse extends BaseResponse {
        public FolderViewData data;
    }
    
    class FolderViewData {
        public List<ManagedFolder> folders;
    }
    
    class ManagedFolder {
        public String id;
        public String name;
        public String path;
        public boolean isSystem;   // 是否系统文件夹
    }
    
    // 用户信息
    class UsersResponse extends BaseResponse {
        public UsersData data;
    }
    
    class UsersData {
        public List<UserInfo> users;
    }
    
    class UserInfoResponse extends BaseResponse {
        public UserInfo data;
    }
    
    class UserInfo {
        public String uid;          // 用户ID
        public String username;   // 用户名
        public String nickname;   // 昵称
        public String avatar;     // 头像URL
        public boolean isAdmin;   // 是否管理员
        public long createTime;   // 创建时间
    }
    
    // 系统信息
    class SysInfoResponse extends BaseResponse {
        public SysInfoData data;
    }
    
    class SysInfoData {
        public String version;      // 系统版本
        public String model;        // 设备型号
        public String serial;       // 序列号
        public String hostname;     // 主机名
        public long uptime;         // 运行时间
    }
    
    // 统计信息
    class PhotoStatsResponse {
        public int code;
        public String msg;
        public PhotoStatsData data;
    }
    
    class PhotoStatsData {
        public int id;
        public int photoCount;      // 总照片数
        public int videoCount;      // 总视频数
        public boolean isAdmin;
        public int nasUid;
    }
    
    // 应用版本
    class AppVersionResponse extends BaseResponse {
        public AppVersionData data;
    }
    
    class AppVersionData {
        public String version;      // 版本号
    }
    
    // 时间线
    class TimelineResponse {
        public int code;
        public String msg;
        public TimelineData data;
    }
    
    class TimelineData {
        public List<TimelineItem> list;
    }
    
    class TimelineItem {
        public int year;
        public int month;
        public int day;
        public int itemCount;
        public List<String> previewThumbs; // 预览缩略图URL列表（前4个）
    }
    
    // 时间范围请求
    class TimeRangeRequest {
        public long startTime;      // 开始时间戳
        public long endTime;        // 结束时间戳
        public int page;            // 页码
        public int pageSize;        // 每页数量
    }

    // ==================== 照片列表接口 (/p/api/v1/gallery/getList) ====================

    class GalleryListResponse {
        public int code;
        public String msg;
        public GalleryListData data;
    }

    class GalleryListData {
        public Integer count;
        public Boolean hasNext;
        public List<GalleryPhoto> list;
    }

    class GalleryPhoto {
        public int id;
        public int ownerId;
        public String dateTime;
        public String photoDateTime;
        public String fileType;
        public String category;  // "photo" or "video"
        public String fileName;
        public long fileSize;
        public String description;
        public int isCollect;
        public String model;
        public String fumber;
        public String exposureTime;
        public String isoSpeedRatings;
        public String focalLength;
        public String mp;
        public String filePath;
        public String showFilePath;
        public int height;
        public int width;
        public String make;
        public String exposureProgram;
        public String meteringMode;
        public String geo;
        public int isLive;
        public int rotation;
        public int isCanPreview;
        public GalleryPhotoAdditional additional;
        public String photoUUID;
    }

    class GalleryPhotoAdditional {
        public GalleryThumbnail thumbnail;
        public List<String> tags;
    }

    class GalleryThumbnail {
        public String mUrl;   // 中等尺寸
        public String sUrl;   // 小尺寸
        public String xsUrl;  // 超小尺寸
        public String xxsUrl; // 超超小尺寸
        public String videoUrl;
        public String originalUrl; // 原图
    }

}
