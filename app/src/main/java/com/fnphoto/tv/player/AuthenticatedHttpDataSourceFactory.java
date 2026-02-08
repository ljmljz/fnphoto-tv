package com.fnphoto.tv.player;

import android.content.Context;
import android.content.SharedPreferences;

import com.fnphoto.tv.api.FnAuthUtils;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.util.List;
import java.util.Map;

/**
 * 自定义 HttpDataSource 工厂，用于添加飞牛相册的认证头
 * 适配 ExoPlayer API 2.11.8 (支持 API 19)
 */
public class AuthenticatedHttpDataSourceFactory implements DataSource.Factory {

    private final Context context;
    private final String userAgent;

    public AuthenticatedHttpDataSourceFactory(Context context, String userAgent) {
        this.context = context;
        this.userAgent = userAgent;
    }

    @Override
    public DataSource createDataSource() {
        // 创建 HttpDataSource 工厂 - 必须实现所有 5 个方法
        HttpDataSource.Factory httpFactory = new HttpDataSource.Factory() {
            @Override
            public HttpDataSource createDataSource() {
                DefaultHttpDataSource dataSource = new DefaultHttpDataSource(userAgent);
                return new AuthenticatedHttpDataSource(context, dataSource);
            }
            
            @Override
            public HttpDataSource.RequestProperties getDefaultRequestProperties() {
                return null;
            }
            
            @Override
            public void setDefaultRequestProperty(String name, String value) {
                // Deprecated in 2.11.8, no-op
            }
            
            @Override
            public void clearDefaultRequestProperty(String name) {
                // Deprecated in 2.11.8, no-op
            }
            
            @Override
            public void clearAllDefaultRequestProperties() {
                // Deprecated in 2.11.8, no-op
            }
        };

        // 使用 DefaultDataSourceFactory 包装
        return new DefaultDataSourceFactory(context, httpFactory).createDataSource();
    }

    /**
     * 包装 DefaultHttpDataSource，在打开连接时添加认证头
     */
    private static class AuthenticatedHttpDataSource implements HttpDataSource {

        private final Context context;
        private final DefaultHttpDataSource delegate;

        AuthenticatedHttpDataSource(Context context, DefaultHttpDataSource delegate) {
            this.context = context;
            this.delegate = delegate;
        }

        @Override
        public long open(com.google.android.exoplayer2.upstream.DataSpec dataSpec)
                throws HttpDataSourceException {
            // 获取 URL 并生成 authx
            String url = dataSpec.uri.toString();
            String path = url.replaceFirst("^https?://[^/]+", "");

            SharedPreferences prefs = context.getSharedPreferences("fn_photo_prefs", Context.MODE_PRIVATE);
            String token = prefs.getString("api_token", "");
            String authx = FnAuthUtils.generateAuthX(path, "GET", null);

            // 设置认证头
            delegate.setRequestProperty("accesstoken", token);
            delegate.setRequestProperty("authx", authx);

            return delegate.open(dataSpec);
        }

        @Override
        public int read(byte[] buffer, int offset, int readLength) throws HttpDataSourceException {
            return delegate.read(buffer, offset, readLength);
        }

        @Override
        public void close() throws HttpDataSourceException {
            delegate.close();
        }

        @Override
        public void setRequestProperty(String name, String value) {
            delegate.setRequestProperty(name, value);
        }

        @Override
        public void clearRequestProperty(String name) {
            delegate.clearRequestProperty(name);
        }

        @Override
        public void clearAllRequestProperties() {
            delegate.clearAllRequestProperties();
        }

        @Override
        public Map<String, List<String>> getResponseHeaders() {
            return delegate.getResponseHeaders();
        }

        @Override
        public android.net.Uri getUri() {
            return delegate.getUri();
        }

        @Override
        public void addTransferListener(TransferListener transferListener) {
            delegate.addTransferListener(transferListener);
        }

        @Override
        public int getResponseCode() {
            return delegate.getResponseCode();
        }
    }
}
