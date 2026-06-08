package com.fnphoto.tv;

import java.util.ArrayList;
import java.util.List;

public class MediaListHolder {
    private static List<MediaItem> sMediaList;

    public static void set(List<MediaItem> list) {
        sMediaList = list != null ? new ArrayList<>(list) : null;
    }

    public static List<MediaItem> get() {
        return sMediaList;
    }

    public static void clear() {
        sMediaList = null;
    }
}
