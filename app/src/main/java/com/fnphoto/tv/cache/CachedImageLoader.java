package com.fnphoto.tv.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;

public class CachedImageLoader {
    private static final String TAG = "CachedImageLoader";
    
    public interface ImageLoadCallback {
        void onBitmapLoaded(Bitmap bitmap);
        void onLoadFailed();
    }
    
    /**
     * 加载图片（带缓存）
     * @param context 上下文
     * @param url 图片URL
     * @param token 认证token
     * @param width 目标宽度
     * @param height 目标高度
     * @param callback 回调
     */
    public static void loadImage(Context context, String url, String token, 
                                  int width, int height, ImageLoadCallback callback) {
        ImageCacheManager cacheManager = ImageCacheManager.getInstance(context);
        
        // 1. 先检查内存/磁盘缓存
        Bitmap cachedBitmap = cacheManager.getCachedBitmap(url);
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            Log.d(TAG, "Using cached image for: " + url);
            callback.onBitmapLoaded(cachedBitmap);
            return;
        }
        
        // 2. 检查缓存文件（用于Glide直接从文件加载）
        File cacheFile = cacheManager.getCacheFile(url);
        if (cacheFile != null) {
            // 从缓存文件加载
            Log.d(TAG, "Loading from cache file: " + url);
            Glide.with(context)
                    .asBitmap()
                    .load(cacheFile)
                    .override(width, height)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            callback.onBitmapLoaded(resource);
                        }
                        
                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {}
                        
                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            // 缓存文件损坏，从网络加载
                            loadFromNetwork(context, url, token, width, height, callback);
                        }
                    });
            return;
        }
        
        // 3. 从网络加载
        loadFromNetwork(context, url, token, width, height, callback);
    }
    
    /**
     * 从网络加载并缓存
     */
    private static void loadFromNetwork(Context context, String url, String token,
                                        int width, int height, ImageLoadCallback callback) {
        Log.d(TAG, "Loading from network: " + url);
        
        ImageCacheManager cacheManager = ImageCacheManager.getInstance(context);
        
        GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("accesstoken", token)
                .build());
        
        Glide.with(context)
                .asBitmap()
                .load(glideUrl)
                .override(width, height)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // 保存到缓存
                        cacheManager.saveBitmapToCache(url, resource);
                        callback.onBitmapLoaded(resource);
                    }
                    
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                    
                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        Log.e(TAG, "Failed to load image: " + url);
                        callback.onLoadFailed();
                    }
                });
    }
    
    /**
     * 加载单张图片到ImageView（使用Glide的标准方式，但会先检查缓存）
     */
    public static void loadIntoImageView(android.widget.ImageView imageView, String url, String token) {
        Context context = imageView.getContext();
        ImageCacheManager cacheManager = ImageCacheManager.getInstance(context);
        
        // 检查缓存文件
        File cacheFile = cacheManager.getCacheFile(url);
        if (cacheFile != null) {
            // 从缓存加载
            Glide.with(context)
                    .load(cacheFile)
                    .centerCrop()
                    .into(imageView);
        } else {
            // 从网络加载
            GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                    .addHeader("accesstoken", token)
                    .build());
            
            Glide.with(context)
                    .load(glideUrl)
                    .centerCrop()
                    .into(new com.bumptech.glide.request.target.DrawableImageViewTarget(imageView) {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            super.onResourceReady(resource, transition);
                            // 下载完成后保存到缓存（Glide会自动处理）
                        }
                    });
        }
    }
    
    /**
     * 预加载图片到缓存
     */
    public static void preloadImage(Context context, String url, String token) {
        ImageCacheManager cacheManager = ImageCacheManager.getInstance(context);
        
        // 如果缓存不存在，从网络加载并缓存
        if (!cacheManager.isCacheValid(url)) {
            GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                    .addHeader("accesstoken", token)
                    .build());
            
            Glide.with(context)
                    .asBitmap()
                    .load(glideUrl)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            cacheManager.saveBitmapToCache(url, resource);
                        }
                        
                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {}
                    });
        }
    }
    
    /**
     * 清空所有图片缓存
     */
    public static void clearAllCache(Context context) {
        ImageCacheManager.getInstance(context).clearCache();
    }
    
    /**
     * 获取缓存大小（MB）
     */
    public static float getCacheSizeMB(Context context) {
        long bytes = ImageCacheManager.getInstance(context).getCacheSize();
        return bytes / (1024f * 1024f);
    }
}
