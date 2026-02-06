package com.fnphoto.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

public class CardPresenter extends Presenter {
    private static final int CARD_WIDTH = 320;
    private static final int CARD_HEIGHT = 180;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        ImageCardView cardView = new ImageCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                super.setSelected(selected);
                updateCardBackgroundColor(this, selected);
            }
        };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        
        // 自定义 info field 样式
        customizeInfoField(cardView);
        
        updateCardBackgroundColor(cardView, false);
        
        return new ViewHolder(cardView);
    }

    private void customizeInfoField(ImageCardView cardView) {
        // 获取 info field 并设置半透明黑色背景
        View infoField = cardView.findViewById(androidx.leanback.R.id.info_field);
        if (infoField != null) {
            infoField.setBackgroundColor(Color.parseColor("#80000000")); // 半透明黑色
        }
        
        // 获取 title text view 并设置小字体
        TextView titleView = cardView.findViewById(androidx.leanback.R.id.title_text);
        if (titleView != null) {
            titleView.setTextSize(12); // 小字体
            titleView.setTextColor(Color.WHITE);
        }
        
        // 获取 content text view 并隐藏
        TextView contentView = cardView.findViewById(androidx.leanback.R.id.content_text);
        if (contentView != null) {
            contentView.setVisibility(View.GONE);
        }
    }

    private void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? 
                ContextCompat.getColor(view.getContext(), android.R.color.white) :
                ContextCompat.getColor(view.getContext(), android.R.color.darker_gray);
        view.setBackgroundColor(color);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        MediaItem mediaItem = (MediaItem) item;
        ImageCardView cardView = (ImageCardView) viewHolder.view;

        cardView.setTitleText(mediaItem.getTitle());
        // 不设置 content text（已隐藏）
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);

        // 加载图片
        String imageUrl = mediaItem.getThumbnailUrl() != null ? mediaItem.getThumbnailUrl() : mediaItem.getMediaUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // 获取 token 并生成 authx
            Context context = viewHolder.view.getContext();
            SharedPreferences prefs = context.getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
            String token = prefs.getString("api_token", "");

            // 构建带认证头的 GlideUrl
            GlideUrl glideUrl = new GlideUrl(imageUrl, new LazyHeaders.Builder()
                    .addHeader("accesstoken", token)
                    .build());

            Glide.with(context)
                    .load(glideUrl)
                    .centerCrop()
                    .into(cardView.getMainImageView());
        } else {
            // 默认图标
            Drawable drawable = ContextCompat.getDrawable(viewHolder.view.getContext(),
                    android.R.drawable.ic_menu_gallery);
            cardView.setMainImage(drawable);
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}
