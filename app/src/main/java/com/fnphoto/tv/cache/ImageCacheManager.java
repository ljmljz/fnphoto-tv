package com.fnphoto.tv.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageCacheManager {
    private static final String TAG = "ImageCacheManager";
    private static final String CACHE_DIR = "image_cache";
    private static final long DEFAULT_CACHE_EXPIRY = 10 * 24 * 60 * 60 * 1000; // 10 * 24小时
    private static final long MAX_CACHE_SIZE = 200 * 1024 * 1024; // 200MB
    
    private static ImageCacheManager instance;
    private File cacheDir;
    private ExecutorService executorService;
    
    public static synchronized ImageCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new ImageCacheManager(context);
        }
        return instance;
    }
    
    private ImageCacheManager(Context context) {
        cacheDir = new File(context.getCacheDir(), CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        executorService = Executors.newFixedThreadPool(4);
    }
    
    /**
     * 获取缓存的Bitmap
     * @param url 图片URL
     * @return 缓存的Bitmap，如果没有缓存或已过期则返回null
     */
    public Bitmap getCachedBitmap(String url) {
        String fileName = hashUrl(url);
        File cacheFile = new File(cacheDir, fileName);
        
        if (!cacheFile.exists()) {
            return null;
        }
        
        // 检查是否过期
        long age = System.currentTimeMillis() - cacheFile.lastModified();
        if (age > DEFAULT_CACHE_EXPIRY) {
            Log.d(TAG, "Cache expired for: " + url);
            cacheFile.delete();
            return null;
        }
        
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(cacheFile));
            Log.d(TAG, "Cache hit for: " + url);
            return bitmap;
        } catch (IOException e) {
            Log.e(TAG, "Error reading cache file", e);
            cacheFile.delete();
            return null;
        }
    }
    
    /**
     * 将Bitmap保存到缓存
     * @param url 图片URL
     * @param bitmap Bitmap对象
     */
    public void saveBitmapToCache(String url, Bitmap bitmap) {
        executorService.execute(() -> {
            try {
                // 检查缓存大小，如果超过限制则清理
                ensureCacheSize();
                
                String fileName = hashUrl(url);
                File cacheFile = new File(cacheDir, fileName);
                
                FileOutputStream fos = new FileOutputStream(cacheFile);
                // 压缩保存为PNG，质量100%
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                
                Log.d(TAG, "Saved to cache: " + url + " (" + cacheFile.length() / 1024 + "KB)");
            } catch (IOException e) {
                Log.e(TAG, "Error saving cache file", e);
            }
        });
    }
    
    /**
     * 检查缓存是否存在且有效
     * @param url 图片URL
     * @return true 如果缓存存在且未过期
     */
    public boolean isCacheValid(String url) {
        String fileName = hashUrl(url);
        File cacheFile = new File(cacheDir, fileName);
        
        if (!cacheFile.exists()) {
            return false;
        }
        
        long age = System.currentTimeMillis() - cacheFile.lastModified();
        return age <= DEFAULT_CACHE_EXPIRY;
    }
    
    /**
     * 清空所有缓存
     */
    public void clearCache() {
        executorService.execute(() -> {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            Log.d(TAG, "Cache cleared");
        });
    }
    
    /**
     * 获取缓存文件
     */
    public File getCacheFile(String url) {
        String fileName = hashUrl(url);
        File cacheFile = new File(cacheDir, fileName);
        
        if (cacheFile.exists()) {
            // 检查是否过期
            long age = System.currentTimeMillis() - cacheFile.lastModified();
            if (age <= DEFAULT_CACHE_EXPIRY) {
                return cacheFile;
            } else {
                cacheFile.delete();
            }
        }
        return null;
    }
    
    /**
     * 获取缓存文件路径（用于Glide加载）
     */
    public String getCacheFilePath(String url) {
        File cacheFile = getCacheFile(url);
        return cacheFile != null ? cacheFile.getAbsolutePath() : null;
    }
    
    /**
     * 确保缓存大小不超过限制
     */
    private void ensureCacheSize() {
        long totalSize = getCacheSize();
        if (totalSize > MAX_CACHE_SIZE) {
            // 删除最旧的文件
            File[] files = cacheDir.listFiles();
            if (files != null && files.length > 0) {
                // 按修改时间排序
                java.util.Arrays.sort(files, (f1, f2) -> 
                    Long.compare(f1.lastModified(), f2.lastModified()));
                
                // 删除最旧的50%文件
                int deleteCount = files.length / 2;
                for (int i = 0; i < deleteCount; i++) {
                    files[i].delete();
                }
                Log.d(TAG, "Cleaned " + deleteCount + " old cache files");
            }
        }
    }
    
    /**
     * 获取当前缓存大小
     */
    public long getCacheSize() {
        long size = 0;
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                size += file.length();
            }
        }
        return size;
    }
    
    /**
     * 使用MD5哈希URL作为文件名
     */
    private String hashUrl(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(url.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString() + ".png";
        } catch (NoSuchAlgorithmException e) {
            // 如果MD5不可用，使用URL的hashCode
            return String.valueOf(url.hashCode()) + ".png";
        }
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}
