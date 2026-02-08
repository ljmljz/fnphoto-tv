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
 * 适配 ExoPlayer API 2.14.2
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
        // 创建 HttpDataSource.Factory 来生产带认证的 DataSource
        HttpDataSource.Factory httpDataSourceFactory = new HttpDataSource.Factory() {
            @Override
            public HttpDataSource createDataSource() {
                DefaultHttpDataSource httpDataSource = new DefaultHttpDataSource(userAgent);
                return new AuthenticatedHttpDataSource(context, httpDataSource);
            }

            @Override
            public HttpDataSource.Factory setDefaultRequestProperties(Map<String, String> defaultRequestProperties) {
                // 不需要设置默认请求头，认证头会在 AuthenticatedHttpDataSource.open() 中动态添加
                return this;
            }

            @Override
            public HttpDataSource.RequestProperties getDefaultRequestProperties() {
                return null;
            }
        };

        // 包装成 DefaultDataSource（支持本地和网络数据）
        return new DefaultDataSourceFactory(context, httpDataSourceFactory).createDataSource();
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
