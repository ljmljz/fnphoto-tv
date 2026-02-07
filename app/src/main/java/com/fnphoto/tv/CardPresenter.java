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
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.List;

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
        Log.d(TAG, "onBindViewHolder - item id: " + mediaItem.getId() + ", type: " + mediaItem.getType());
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
        Log.d(TAG, "loadPreviewImages called with " + urls.size() + " urls, itemId: " + itemId);
        Context context = cardView.getContext();
        SharedPreferences prefs = context.getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("api_token", "");
        
        int count = Math.min(urls.size(), MAX_PREVIEW);
        Log.d(TAG, "Loading " + count + " preview images");
        PreviewLoader loader = new PreviewLoader(cardView, itemId, count, DATE_CARD_WIDTH, DATE_CARD_HEIGHT);
        
        for (int i = 0; i < count; i++) {
            String url = urls.get(i);
            if (baseUrl != null && !url.startsWith("http")) {
                url = baseUrl + url;
            }
            
            final int index = i;
            
            GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                    .addHeader("accesstoken", token)
                    .build());
            
            Glide.with(context)
                    .asBitmap()
                    .load(glideUrl)
                    .override(150, 150)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            loader.onBitmapLoaded(index, resource);
                        }
                        
                        @Override
                        public void onLoadFailed(Drawable errorDrawable) {
                            loader.onBitmapFailed(index);
                        }
                    });
        }
    }

    // 内部类：管理单个卡片的预览图加载
    private static class PreviewLoader {
        private ImageCardView cardView;
        private String expectedId;
        private int totalCount;
        private int loadedCount = 0;
        private Bitmap[] bitmaps;
        private int width;
        private int height;

        PreviewLoader(ImageCardView cardView, String expectedId, int count, int width, int height) {
            this.cardView = cardView;
            this.expectedId = expectedId;
            this.totalCount = count;
            this.width = width;
            this.height = height;
            this.bitmaps = new Bitmap[count];
        }

        void onBitmapLoaded(int index, Bitmap bitmap) {
            if (index >= 0 && index < bitmaps.length) {
                bitmaps[index] = bitmap;
            }
            checkComplete();
        }

        void onBitmapFailed(int index) {
            checkComplete();
        }

        private void checkComplete() {
            loadedCount++;
            Log.d(TAG, "PreviewLoader checkComplete: " + loadedCount + "/" + totalCount);
            if (loadedCount >= totalCount) {
                // 检查视图是否还是原来的那个
                String currentId = (String) cardView.getTag(R.id.media_item_id);
                Log.d(TAG, "PreviewLoader - expectedId: " + expectedId + ", currentId: " + currentId);
                
                // 显示图片的条件：
                // 1. currentId 和 expectedId 匹配
                // 2. 两者都为 null（视图刚创建）
                boolean shouldShowImage = false;
                if (currentId != null && currentId.equals(expectedId)) {
                    shouldShowImage = true;
                } else if (currentId == null && expectedId == null) {
                    // 两者都为null，视图可能是新的
                    shouldShowImage = true;
                } else if (currentId == null && expectedId != null) {
                    // currentId为null但expectedId不为null，可能是Tag还没设置
                    // 这种情况也可以尝试显示
                    shouldShowImage = true;
                }
                
                if (shouldShowImage) {
                    Bitmap composite = createCompositeBitmap(bitmaps, width, height);
                    cardView.setMainImage(new BitmapDrawable(cardView.getResources(), composite));
                    Log.d(TAG, "PreviewLoader - composite image set successfully");
                } else {
                    Log.w(TAG, "PreviewLoader - view reused (different id), skipping image set. Expected: " + expectedId + ", Current: " + currentId);
                }
            }
        }

        private Bitmap createCompositeBitmap(Bitmap[] bitmaps, int width, int height) {
            Bitmap composite = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(composite);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setFilterBitmap(true);
            
            canvas.drawColor(Color.parseColor("#22000000"));
            
            // 计算有效bitmap数量
            int validCount = 0;
            for (Bitmap b : bitmaps) {
                if (b != null && !b.isRecycled()) validCount++;
            }
            
            if (validCount == 0) return composite;
            
            // 按顺序绘制
            int idx = 0;
            if (validCount == 1) {
                for (int i = 0; i < bitmaps.length; i++) {
                    if (bitmaps[i] != null && !bitmaps[i].isRecycled()) {
                        drawBitmap(canvas, bitmaps[i], 0, 0, width, height, paint);
                        break;
                    }
                }
            } else if (validCount == 2) {
                int halfWidth = (width - PADDING) / 2;
                int drawn = 0;
                for (int i = 0; i < bitmaps.length && drawn < 2; i++) {
                    if (bitmaps[i] != null && !bitmaps[i].isRecycled()) {
                        if (drawn == 0) {
                            drawBitmap(canvas, bitmaps[i], 0, 0, halfWidth, height, paint);
                        } else {
                            drawBitmap(canvas, bitmaps[i], halfWidth + PADDING, 0, width - halfWidth - PADDING, height, paint);
                        }
                        drawn++;
                    }
                }
            } else if (validCount == 3) {
                int halfWidth = (width - PADDING) / 2;
                int halfHeight = (height - PADDING) / 2;
                int drawn = 0;
                for (int i = 0; i < bitmaps.length && drawn < 3; i++) {
                    if (bitmaps[i] != null && !bitmaps[i].isRecycled()) {
                        if (drawn == 0) {
                            drawBitmap(canvas, bitmaps[i], 0, 0, halfWidth, height, paint);
                        } else if (drawn == 1) {
                            drawBitmap(canvas, bitmaps[i], halfWidth + PADDING, 0, width - halfWidth - PADDING, halfHeight, paint);
                        } else {
                            drawBitmap(canvas, bitmaps[i], halfWidth + PADDING, halfHeight + PADDING, width - halfWidth - PADDING, height - halfHeight - PADDING, paint);
                        }
                        drawn++;
                    }
                }
            } else {
                int halfWidth = (width - PADDING) / 2;
                int halfHeight = (height - PADDING) / 2;
                int drawn = 0;
                for (int i = 0; i < bitmaps.length && drawn < 4; i++) {
                    if (bitmaps[i] != null && !bitmaps[i].isRecycled()) {
                        if (drawn == 0) {
                            drawBitmap(canvas, bitmaps[i], 0, 0, halfWidth, halfHeight, paint);
                        } else if (drawn == 1) {
                            drawBitmap(canvas, bitmaps[i], halfWidth + PADDING, 0, width - halfWidth - PADDING, halfHeight, paint);
                        } else if (drawn == 2) {
                            drawBitmap(canvas, bitmaps[i], 0, halfHeight + PADDING, halfWidth, height - halfHeight - PADDING, paint);
                        } else {
                            drawBitmap(canvas, bitmaps[i], halfWidth + PADDING, halfHeight + PADDING, width - halfWidth - PADDING, height - halfHeight - PADDING, paint);
                        }
                        drawn++;
                    }
                }
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
    }

    private Drawable createPlaceholderDrawable(Context context) {
        Bitmap bitmap = Bitmap.createBitmap(DATE_CARD_WIDTH, DATE_CARD_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.parseColor("#33000000"));
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private void loadSingleImage(ImageCardView cardView, MediaItem mediaItem) {
        String imageUrl = mediaItem.getThumbnailUrl() != null ?
                mediaItem.getThumbnailUrl() : mediaItem.getMediaUrl();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Context context = cardView.getContext();
            SharedPreferences prefs = context.getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
            String token = prefs.getString("api_token", "");

            GlideUrl glideUrl = new GlideUrl(imageUrl, new LazyHeaders.Builder()
                    .addHeader("accesstoken", token)
                    .build());

            Glide.with(context)
                    .load(glideUrl)
                    .centerCrop()
                    .into(cardView.getMainImageView());
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
