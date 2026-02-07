package com.fnphoto.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.fnphoto.tv.cache.CachedImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CardPresenter extends Presenter {
    private static final String TAG = "CardPresenter";
    private static final int CARD_WIDTH = 320;
    private static final int CARD_HEIGHT = 180;
    private static final int DATE_CARD_WIDTH = 240;
    private static final int DATE_CARD_HEIGHT = 180;
    private static final int PADDING = 4;
    private static final int MAX_PREVIEW = 4;

    private String baseUrl;

    public CardPresenter(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ImageCardView cardView = new ImageCardView(parent.getContext());
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        
        customizeInfoField(cardView);
        
        return new ViewHolder(cardView);
    }

    private void customizeInfoField(ImageCardView cardView) {
        View infoField = cardView.findViewById(androidx.leanback.R.id.info_field);
        if (infoField != null) {
            infoField.setBackgroundColor(Color.parseColor("#80000000"));
        }
        
        TextView titleView = cardView.findViewById(androidx.leanback.R.id.title_text);
        if (titleView != null) {
            titleView.setTextSize(10);
            titleView.setTextColor(Color.WHITE);
        }
        
        TextView contentView = cardView.findViewById(androidx.leanback.R.id.content_text);
        if (contentView != null) {
            contentView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        MediaItem mediaItem = (MediaItem) item;
        ImageCardView cardView = (ImageCardView) viewHolder.view;

        cardView.setTitleText(mediaItem.getTitle());
        
        // 存储当前item的ID，用于检查视图是否已被重用
        cardView.setTag(R.id.media_item_id, mediaItem.getId());

        if ("date".equals(mediaItem.getType())) {
            cardView.setMainImageDimensions(DATE_CARD_WIDTH, DATE_CARD_HEIGHT);
            
            List<String> previewUrls = mediaItem.getPreviewThumbUrls();
            if (previewUrls != null && !previewUrls.isEmpty()) {
                loadPreviewImages(cardView, previewUrls, mediaItem.getId());
            } else {
                cardView.setMainImage(createPlaceholderDrawable(cardView.getContext()));
            }
        } else {
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
            loadSingleImage(cardView, mediaItem);
        }
    }

    private void loadPreviewImages(ImageCardView cardView, List<String> urls, String itemId) {
        Context context = cardView.getContext();
        SharedPreferences prefs = context.getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("api_token", "");
        
        int count = Math.min(urls.size(), MAX_PREVIEW);
        Bitmap[] bitmaps = new Bitmap[count];
        AtomicInteger loadedCount = new AtomicInteger(0);
        
        Log.d(TAG, "Loading " + count + " preview images with cache support");
        
        for (int i = 0; i < count; i++) {
            String url = urls.get(i);
            if (baseUrl != null && !url.startsWith("http")) {
                url = baseUrl + url;
            }
            
            final int index = i;
            
            // 使用带缓存的加载器
            CachedImageLoader.loadImage(context, url, token, 150, 150, 
                    new CachedImageLoader.ImageLoadCallback() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap) {
                            bitmaps[index] = bitmap;
                            int current = loadedCount.incrementAndGet();
                            Log.d(TAG, "Preview loaded " + current + "/" + count + " for " + itemId);
                            
                            if (current >= count) {
                                // 所有图片加载完成，检查视图是否可用
                                String currentId = (String) cardView.getTag(R.id.media_item_id);
                                if (itemId.equals(currentId)) {
                                    Bitmap composite = createCompositeBitmap(bitmaps, DATE_CARD_WIDTH, DATE_CARD_HEIGHT);
                                    cardView.setMainImage(new BitmapDrawable(context.getResources(), composite));
                                    Log.d(TAG, "Composite image set for " + itemId);
                                }
                            }
                        }
                        
                        @Override
                        public void onLoadFailed() {
                            int current = loadedCount.incrementAndGet();
                            Log.w(TAG, "Preview load failed " + current + "/" + count + " for " + itemId);
                            
                            if (current >= count) {
                                String currentId = (String) cardView.getTag(R.id.media_item_id);
                                if (itemId.equals(currentId)) {
                                    Bitmap composite = createCompositeBitmap(bitmaps, DATE_CARD_WIDTH, DATE_CARD_HEIGHT);
                                    cardView.setMainImage(new BitmapDrawable(context.getResources(), composite));
                                }
                            }
                        }
                    });
        }
    }

    private Bitmap createCompositeBitmap(Bitmap[] bitmaps, int width, int height) {
        Bitmap composite = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(composite);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        
        canvas.drawColor(Color.parseColor("#22000000"));
        
        // 计算有效bitmap数量
        List<Bitmap> validBitmaps = new ArrayList<>();
        for (Bitmap b : bitmaps) {
            if (b != null && !b.isRecycled()) {
                validBitmaps.add(b);
            }
        }
        
        if (validBitmaps.isEmpty()) {
            return composite;
        }

        int count = validBitmaps.size();

        if (count == 1) {
            drawBitmap(canvas, validBitmaps.get(0), 0, 0, width, height, paint);
        } else if (count == 2) {
            int halfWidth = (width - PADDING) / 2;
            // 左半部分
            drawBitmap(canvas, validBitmaps.get(0), 0, 0, halfWidth, height, paint);
            // 右半部分：从 halfWidth+PADDING 开始，到 width 结束
            drawBitmap(canvas, validBitmaps.get(1), halfWidth + PADDING, 0, width, height, paint);
        } else if (count == 3) {
            int halfWidth = (width - PADDING) / 2;
            int halfHeight = (height - PADDING) / 2;
            // 左边大图
            drawBitmap(canvas, validBitmaps.get(0), 0, 0, halfWidth, height, paint);
            // 右上：从 y=0 到 y=halfHeight
            drawBitmap(canvas, validBitmaps.get(1), halfWidth + PADDING, 0, width, halfHeight, paint);
            // 右下：从 y=halfHeight+PADDING 到 y=height
            drawBitmap(canvas, validBitmaps.get(2), halfWidth + PADDING, halfHeight + PADDING, width, height, paint);
        } else {
            int halfWidth = (width - PADDING) / 2;
            int halfHeight = (height - PADDING) / 2;
            // 左上
            drawBitmap(canvas, validBitmaps.get(0), 0, 0, halfWidth, halfHeight, paint);
            // 右上
            drawBitmap(canvas, validBitmaps.get(1), halfWidth + PADDING, 0, width, halfHeight, paint);
            // 左下：从 y=halfHeight+PADDING 到 y=height
            drawBitmap(canvas, validBitmaps.get(2), 0, halfHeight + PADDING, halfWidth, height, paint);
            // 右下
            drawBitmap(canvas, validBitmaps.get(3), halfWidth + PADDING, halfHeight + PADDING, width, height, paint);
        }
        
        return composite;
    }

    private void drawBitmap(Canvas canvas, Bitmap bitmap, float left, float top, float right, float bottom, Paint paint) {
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        float targetWidth = right - left;
        float targetHeight = bottom - top;
        
        float scale = Math.max(targetWidth / bitmapWidth, targetHeight / bitmapHeight);
        
        float srcLeft = Math.max(0, (bitmapWidth - targetWidth / scale) / 2);
        float srcTop = Math.max(0, (bitmapHeight - targetHeight / scale) / 2);
        float srcRight = Math.min(bitmapWidth, srcLeft + targetWidth / scale);
        float srcBottom = Math.min(bitmapHeight, srcTop + targetHeight / scale);
        
        Rect srcRect = new Rect((int) srcLeft, (int) srcTop, (int) srcRight, (int) srcBottom);
        Rect dstRect = new Rect((int) left, (int) top, (int) right, (int) bottom);
        
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint);
    }

    private Drawable createPlaceholderDrawable(Context context) {
        Bitmap bitmap = Bitmap.createBitmap(DATE_CARD_WIDTH, DATE_CARD_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.parseColor("#33000000"));

        // Don't draw the lines for seperate
//        Paint paint = new Paint();
//        paint.setColor(Color.parseColor("#55FFFFFF"));
//        paint.setStrokeWidth(2);
//
//        canvas.drawLine(DATE_CARD_WIDTH/2, 0, DATE_CARD_WIDTH/2, DATE_CARD_HEIGHT, paint);
//        canvas.drawLine(0, DATE_CARD_HEIGHT/2, DATE_CARD_WIDTH, DATE_CARD_HEIGHT/2, paint);
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private void loadSingleImage(ImageCardView cardView, MediaItem mediaItem) {
        Context context = cardView.getContext();
        SharedPreferences prefs = context.getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("api_token", "");
        
        String imageUrl = mediaItem.getThumbnailUrl() != null ?
                mediaItem.getThumbnailUrl() : mediaItem.getMediaUrl();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            // 使用带缓存的加载
            CachedImageLoader.loadIntoImageView(cardView.getMainImageView(), imageUrl, token);
        } else {
            Drawable drawable = ContextCompat.getDrawable(cardView.getContext(),
                    android.R.drawable.ic_menu_gallery);
            cardView.setMainImage(drawable);
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
        cardView.setTag(R.id.media_item_id, null);
    }
}
