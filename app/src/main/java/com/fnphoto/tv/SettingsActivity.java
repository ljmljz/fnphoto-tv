package com.fnphoto.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.fnphoto.tv.cache.CachedImageLoader;

/**
 * 设置页：Immich 风格 —— 深色底、分组卡片、低饱和大标题。
 */
public class SettingsActivity extends FragmentActivity {

    private static final int INK = 0xFF0B0C0F;
    private static final int TEXT_PRIMARY = 0xFFF2F4F8;
    private static final int TEXT_SECONDARY = 0xFFA8AFBF;
    private static final int TEXT_TERTIARY = 0xFF6E7686;
    private static final int DANGER = 0xFFEF4444;

    private LinearLayout contentLayout;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        prefs = getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);

        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(INK);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);

        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(dp(72), dp(40), dp(72), dp(56));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("设置");
        tvTitle.setTextColor(TEXT_PRIMARY);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvTitle.setLetterSpacing(0.01f);
        contentLayout.addView(tvTitle);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText("连接状态、本地缓存与版本信息");
        tvSubtitle.setTextColor(TEXT_SECONDARY);
        tvSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(8);
        tvSubtitle.setLayoutParams(subLp);
        contentLayout.addView(tvSubtitle);

        addSectionHeader("连接信息");
        LinearLayout connCard = createCard();
        addInfoRow(connCard, "NAS 地址", prefs.getString("nas_url", "未设置"));
        addInfoRow(connCard, "登录状态",
                prefs.getString("api_token", "").isEmpty() ? "未登录" : "已登录");
        contentLayout.addView(connCard);

        addSectionHeader("缓存管理");
        LinearLayout cacheCard = createCard();
        addCacheRow(cacheCard);
        contentLayout.addView(cacheCard);

        LinearLayout actionCard = createCard();
        TextView action = new TextView(this);
        action.setText("清空图片缓存");
        action.setTextColor(DANGER);
        action.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        action.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        action.setGravity(Gravity.CENTER_VERTICAL);
        action.setPadding(dp(16), 0, dp(16), 0);
        action.setBackgroundResource(R.drawable.bg_menu_item);
        action.setFocusable(true);
        action.setFocusableInTouchMode(true);
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        actionLp.topMargin = dp(4);
        actionLp.bottomMargin = dp(4);
        action.setLayoutParams(actionLp);
        action.setOnClickListener(v -> {
            CachedImageLoader.clearAllCache(this);
            Toast.makeText(this, "缓存已清空", Toast.LENGTH_SHORT).show();
            refreshCacheSize();
        });
        actionCard.addView(action);
        contentLayout.addView(actionCard);

        addSectionHeader("关于");
        LinearLayout aboutCard = createCard();
        addInfoRow(aboutCard, "应用名称", "fnPhoto TV");
        addInfoRow(aboutCard, "版本", "1.0.0");
        addInfoRow(aboutCard, "开发", "基于飞牛 NAS 相册系统");
        contentLayout.addView(aboutCard);

        scrollView.addView(contentLayout);
        root.addView(scrollView);
        setContentView(root);
    }

    private void addSectionHeader(String title) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextColor(TEXT_TERTIARY);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        header.setLetterSpacing(0.14f);
        header.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(30);
        lp.bottomMargin = dp(10);
        lp.leftMargin = dp(6);
        header.setLayoutParams(lp);
        contentLayout.addView(header);
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(20), dp(6), dp(20), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        card.setLayoutParams(lp);
        return card;
    }

    private void addInfoRow(LinearLayout card, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(54));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(TEXT_SECONDARY);
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(dp(260),
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextColor(TEXT_PRIMARY);
        tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvValue.setSingleLine(true);
        tvValue.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        tvValue.setGravity(Gravity.END);
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(tvLabel);
        row.addView(tvValue);

        if (card.getChildCount() > 0) {
            View divider = new View(this);
            divider.setBackgroundColor(0x1FFFFFFF);
            card.addView(divider, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        }
        card.addView(row);
    }

    private TextView tvCacheSize;

    private void addCacheRow(LinearLayout card) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(54));

        TextView tvLabel = new TextView(this);
        tvLabel.setText("缓存大小");
        tvLabel.setTextColor(TEXT_SECONDARY);
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(dp(260),
                ViewGroup.LayoutParams.WRAP_CONTENT));

        tvCacheSize = new TextView(this);
        tvCacheSize.setTextColor(TEXT_PRIMARY);
        tvCacheSize.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvCacheSize.setGravity(Gravity.END);
        tvCacheSize.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(tvLabel);
        row.addView(tvCacheSize);
        card.addView(row);

        refreshCacheSize();
    }

    private void refreshCacheSize() {
        if (tvCacheSize != null) {
            float sizeMB = CachedImageLoader.getCacheSizeMB(this);
            tvCacheSize.setText(sizeMB > 0.1f ? String.format("%.1f MB", sizeMB) : "无缓存");
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
