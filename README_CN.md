# fnPhoto TV

Android TV 端FN NAS (fnOS) 相册浏览应用，支持浏览照片和视频。

## 功能特点

- **FN Connect 支持**：通过 FN ID（云端）、局域网 IP 或域名登录，自动发现可达的 NAS
- **时间线视图**：按日期组织的时间线形式浏览照片
- **文件夹视图**：浏览受管理的文件夹
- **相册视图**：浏览用户创建的相册
- **媒体播放**：查看照片和播放视频，支持全屏显示
- **无缝导航**：使用电视遥控器左右键切换照片
- **全屏体验**：无系统 UI 干扰的沉浸式查看
- **TLS 1.2 支持**：针对 API 19 设备内置 Conscrypt + Mozilla CA 证书包，确保 HTTPS 安全连接

## 系统要求

- Android TV 或 API 级别 19+（Android 4.4）的 Android 设备
- 运行相册服务的飞牛 NAS（fnOS）
- 与 NAS 在同一局域网内

## 安装步骤

1. 克隆仓库：
```bash
git clone https://github.com/ljmljz/fnphoto-tv.git
cd fnPhoto
```

2. 使用 Android Studio 或 Gradle 构建项目：
```bash
./gradlew assembleDebug
```

3. 将 APK 安装到 Android TV：
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 认证方式

本应用使用 [fnnas-api](https://github.com/FNOSP/fnnas-api)（由 FNOSP 开发）的认证机制，流程如下：

1. 基于 WebSocket 登录获取访问令牌
2. 使用 API 密钥和密钥对请求签名
3. 基于时间的随机数生成，防止重放攻击

详见 [API.md](API.md) 的详细认证文档。

## 使用说明

1. **首次启动**：输入 FN ID、局域网 IP 或域名，然后输入用户名和密码
2. **时间线导航**：按年、月分组浏览照片
3. **侧边菜单**：按遥控器 MENU 键访问文件夹、相册和设置
4. **查看照片**：点击日期查看当天的所有照片
5. **媒体详情**：点击照片或视频进入全屏查看
6. **照片切换**：在详情页使用左右键切换照片
7. **退出应用**：双击返回键退出应用

## 应用架构

- **LoginActivity**：登录页面，作为启动 Activity
- **MainActivity**：主入口，带侧边抽屉导航
- **MainFragment**：基于 BrowseFragment 的相册网格展示
- **PhotoDetailActivity / MediaDetailActivity**：基于 ExoPlayer 的全屏照片/视频播放器
- **CardPresenter**：自定义 Presenter 用于照片缩略图展示
- **FnAuthUtils**：认证和请求签名工具类
- **FnHttpApi**：基于 Retrofit 的 NAS API 通信接口
- **FnConnectApi**：FN Connect 云端 API 客户端 — FN ID 查询、authx 签名、基于优先级并行延迟的 TCP 探测
- **TlsUtils**：Conscrypt SSL 提供者 + 内置 Mozilla CA 证书包，为 API 19 提供 TLS 1.2 支持
- **FnWebSocketClient / FnWebSocketManager**：基于 WebSocket 的登录流程
- **ApiInterceptor / Reauthenticator**：OkHttp 拦截器，自动注入认证头和刷新令牌

## API 接口

应用与飞牛 NAS 相册 API 通信，主要接口如下：

- `/p/api/v1/gallery/timeline` — 获取时间线数据
- `/p/api/v1/gallery/getList` — 获取指定日期范围的照片
- `/p/api/v1/photo/folder/view` — 获取受管理的文件夹
- `/api/v1/photos/albums` — 获取相册列表
- `/p/api/v1/stream/p/t/{id}` — 流式加载照片/视频

所有请求均通过 authx 头部机制进行认证。

## 配置说明

可在 `FnAuthUtils.java` 中修改以下常量：

```java
private static final String API_KEY = "YOUR_API_KEY";
private static final String API_SECRET = "YOUR_API_SECRET";
```

## 技术栈

- **语言**：Java
- **最低 SDK**：19（Android 4.4 KitKat）
- **目标 SDK**：29（Android 10）
- **TV UI 框架**：Android Leanback
- **图片加载**：Glide
- **视频播放**：ExoPlayer
- **网络请求**：OkHttp 3.12.x + Retrofit

## 致谢

- 认证实现基于 [fnnas-api](https://github.com/FNOSP/fnnas-api)（由 FNOSP 开发）
- 使用 Android Leanback 库构建电视优化界面
- 图片加载使用 Glide
- 视频播放使用 ExoPlayer

## 开源协议

MIT 协议 — 详见 LICENSE 文件

## 贡献指南

欢迎提交 Pull Request。重大变更请先提交 issue 讨论。

## 常见问题

- **连接失败**：确保电视和 NAS 在同一局域网，或 FN Connect 可访问
- **认证失败**：检查用户名和密码是否正确
- **照片无法加载**：确认电视能够访问照片流地址
- **API 19 TLS 错误**：已内置 Conscrypt；若证书有问题，从 https://curl.se/ca/cacert.pem 更新 `res/raw/cacert.pem`
