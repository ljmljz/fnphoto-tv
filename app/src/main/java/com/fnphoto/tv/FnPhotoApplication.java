package com.fnphoto.tv;

import android.app.Application;
import android.content.Context;
import androidx.multidex.MultiDex;

/**
 * Application 类，用于支持 API 19 (Android 4.4) 的 Multidex
 */
public class FnPhotoApplication extends Application {
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // 启用 Multidex 支持
        MultiDex.install(this);
    }
}
