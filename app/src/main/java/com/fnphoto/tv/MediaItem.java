package com.fnphoto.tv;

import java.io.Serializable;
import java.util.List;

public class MediaItem implements Serializable {
    private String id;
    private String title;
    private String type; // "photo", "video", "date", "folder", "album"
    private String thumbnailUrl;
    private String mediaUrl;
    private String dateStr;      // 日期字符串，用于时间线日期项
    private int photoCount;      // 照片数量，用于时间线日期项
    private List<String> previewThumbUrls; // 预览缩略图URL列表（用于时间线日期项）

    public MediaItem(String id, String title, String type, String thumbnailUrl, String mediaUrl) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.thumbnailUrl = thumbnailUrl;
        this.mediaUrl = mediaUrl;
    }

    // 用于时间线日期项的构造方法
    public MediaItem(String dateStr, String title, int photoCount) {
        this.id = dateStr;  // 使用dateStr作为id
        this.dateStr = dateStr;
        this.title = title;
        this.type = "date";
        this.photoCount = photoCount;
    }

    // 用于时间线日期项的构造方法（带预览缩略图）
    public MediaItem(String dateStr, String title, int photoCount, List<String> previewThumbUrls) {
        this.id = dateStr;  // 使用dateStr作为id
        this.dateStr = dateStr;
        this.title = title;
        this.type = "date";
        this.photoCount = photoCount;
        this.previewThumbUrls = previewThumbUrls;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getMediaUrl() { return mediaUrl; }
    public String getDateStr() { return dateStr; }
    public int getPhotoCount() { return photoCount; }
    public List<String> getPreviewThumbUrls() { return previewThumbUrls; }
    public void setPreviewThumbUrls(List<String> previewThumbUrls) { this.previewThumbUrls = previewThumbUrls; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MediaItem mediaItem = (MediaItem) obj;
        return id != null && id.equals(mediaItem.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
