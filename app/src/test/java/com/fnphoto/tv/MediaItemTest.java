package com.fnphoto.tv;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class MediaItemTest {

    @Test
    public void constructor_photoType() {
        MediaItem item = new MediaItem("123", "photo.jpg", "photo", "thumb.jpg", "orig.jpg");
        assertEquals("123", item.getId());
        assertEquals("photo.jpg", item.getTitle());
        assertEquals("photo", item.getType());
        assertEquals("thumb.jpg", item.getThumbnailUrl());
        assertEquals("orig.jpg", item.getMediaUrl());
    }

    @Test
    public void constructor_dateType() {
        MediaItem item = new MediaItem("2026-01-30", "30日 (5张)", 5);
        assertEquals("2026-01-30", item.getId());
        assertEquals("30日 (5张)", item.getTitle());
        assertEquals("date", item.getType());
        assertEquals(5, item.getPhotoCount());
        assertEquals("2026-01-30", item.getDateStr());
    }

    @Test
    public void constructor_dateTypeWithPreviews() {
        List<String> previews = Arrays.asList("url1", "url2", "url3");
        MediaItem item = new MediaItem("2026-01-30", "30日 (3张)", 3, previews);
        assertEquals("2026-01-30", item.getId());
        assertEquals("date", item.getType());
        assertEquals(3, item.getPhotoCount());
        assertEquals(previews, item.getPreviewThumbUrls());
    }

    @Test
    public void setPreviewThumbUrls() {
        MediaItem item = new MediaItem("2026-01-30", "30日 (5张)", 5);
        assertNull(item.getPreviewThumbUrls());

        List<String> previews = new ArrayList<>();
        previews.add("thumb1");
        item.setPreviewThumbUrls(previews);
        assertEquals(previews, item.getPreviewThumbUrls());
    }

    @Test
    public void setDateStr() {
        MediaItem item = new MediaItem("old-date", "test", 1);
        assertEquals("old-date", item.getDateStr());
        item.setDateStr("new-date");
        assertEquals("new-date", item.getDateStr());
    }

    @Test
    public void thumbnailUrlCanBeNull() {
        MediaItem item = new MediaItem("1", "test", "photo", null, "orig.jpg");
        assertNull(item.getThumbnailUrl());
        assertEquals("orig.jpg", item.getMediaUrl());
    }

    @Test
    public void mediaUrlCanBeNull() {
        MediaItem item = new MediaItem("1", "test", "photo", "thumb.jpg", null);
        assertEquals("thumb.jpg", item.getThumbnailUrl());
        assertNull(item.getMediaUrl());
    }

    @Test
    public void equals_sameId_returnsTrue() {
        MediaItem a = new MediaItem("123", "photoA.jpg", "photo", null, null);
        MediaItem b = new MediaItem("123", "photoB.jpg", "photo", null, null);
        assertEquals(a, b);
    }

    @Test
    public void equals_differentId_returnsFalse() {
        MediaItem a = new MediaItem("123", "photoA.jpg", "photo", null, null);
        MediaItem b = new MediaItem("456", "photoB.jpg", "photo", null, null);
        assertNotEquals(a, b);
    }

    @Test
    public void equals_sameObject_returnsTrue() {
        MediaItem a = new MediaItem("123", "test", "photo", null, null);
        assertEquals(a, a);
    }

    @Test
    public void equals_null_returnsFalse() {
        MediaItem a = new MediaItem("123", "test", "photo", null, null);
        assertNotEquals(null, a);
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        MediaItem a = new MediaItem("123", "test", "photo", null, null);
        assertNotEquals(a, "string");
    }

    @Test
    public void hashCode_sameForEqualItems() {
        MediaItem a = new MediaItem("123", "photoA.jpg", "photo", null, null);
        MediaItem b = new MediaItem("123", "photoB.jpg", "photo", null, null);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void hashCode_differsForDifferentItems() {
        MediaItem a = new MediaItem("123", "test", "photo", null, null);
        MediaItem b = new MediaItem("456", "test", "photo", null, null);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void getMediaUrl_returnsNullForDateType() {
        MediaItem item = new MediaItem("2026-01-30", "30日 (5张)", 5);
        assertNull(item.getMediaUrl());
    }

    @Test
    public void getThumbnailUrl_returnsNullForDateType() {
        MediaItem item = new MediaItem("2026-01-30", "30日 (5张)", 5);
        assertNull(item.getThumbnailUrl());
    }
}
