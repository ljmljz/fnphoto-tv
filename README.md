# fnPhoto TV

An Android TV application for browsing photos and videos from FN NAS (fnOS) photo gallery.

## Features

- **FN Connect Support**: Login via FN ID (cloud), local IP, or domain — auto-discovers reachable NAS
- **Timeline View**: Browse photos organized by date in a timeline format
- **Folder View**: Navigate through managed folders
- **Album View**: Browse user-created albums
- **Media Playback**: View photos and play videos with full-screen support
- **Seamless Navigation**: Use TV remote to navigate between photos with left/right keys
- **Fullscreen Experience**: Distraction-free viewing without system UI
- **TLS 1.2 on API 19**: Bundled Conscrypt + Mozilla CA bundle for secure HTTPS on older devices

## Requirements

- Android TV or Android device with API level 19+ (Android 4.4)
- Feiniu NAS (fnOS) running photo gallery service
- Network access to your NAS (same LAN)

## Installation

1. Clone the repository:
```bash
git clone https://github.com/ljmljz/fnphoto-tv.git
cd fnPhoto
```

2. Build the project using Android Studio or Gradle:
```bash
./gradlew assembleDebug
```

3. Install the APK on your Android TV:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Authentication

This application uses the authentication mechanism from [fnnas-api](https://github.com/FNOSP/fnnas-api) by FNOSP. The authentication flow involves:

1. WebSocket-based login to obtain access token
2. Request signing using API key and secret
3. Time-based nonce generation for replay protection

See [API.md](API.md) for detailed authentication documentation.

## Usage

1. **First Launch**: Enter FN ID, local IP, or domain, then your username and password
2. **Timeline Navigation**: Browse photos by date, grouped by year and month
3. **Side Menu**: Press MENU key to access folders, albums, and settings
4. **Photo Viewing**: Click a date to view all photos from that day
5. **Media Details**: Click a photo/video to view in full screen
6. **Navigation**: Use LEFT/RIGHT keys to switch between photos in detail view
7. **Exit**: Double-press BACK key to exit the app

## Architecture

- **LoginActivity**: Login screen as the launcher activity
- **MainActivity**: Main entry point with side drawer navigation
- **MainFragment**: BrowseFragment for displaying photo grids
- **PhotoDetailActivity / MediaDetailActivity**: Full-screen photo/video viewer with ExoPlayer
- **CardPresenter**: Custom presenter for photo thumbnails
- **FnAuthUtils**: Authentication and request signing utilities
- **FnHttpApi**: Retrofit interface for NAS API communication
- **FnConnectApi**: FN Connect cloud API client — FN ID lookup, authx signing, TCP probing with priority-based parallel delays
- **TlsUtils**: Conscrypt SSL provider + bundled Mozilla CA bundle for TLS 1.2 on API 19
- **FnWebSocketClient / FnWebSocketManager**: WebSocket-based login flow
- **ApiInterceptor / Reauthenticator**: OkHttp interceptor for automatic auth header injection and token refresh

## API Integration

The app communicates with Feiniu NAS photo gallery API endpoints:

- `/p/api/v1/gallery/timeline` — Get timeline data
- `/p/api/v1/gallery/getList` — Get photos by date range
- `/p/api/v1/photo/folder/view` — Get managed folders
- `/api/v1/photos/albums` — Get albums
- `/p/api/v1/stream/p/t/{id}` — Stream photos/videos

All requests are authenticated using the authx header mechanism.

## Configuration

The following constants can be modified in `FnAuthUtils.java`:

```java
private static final String API_KEY = "YOUR_API_KEY";
private static final String API_SECRET = "YOUR_API_SECRET";
```

## Tech Stack

- **Language**: Java
- **Min SDK**: 19 (Android 4.4 KitKat)
- **Target SDK**: 29 (Android 10)
- **TV UI**: Android Leanback library
- **Image Loading**: Glide
- **Video Playback**: ExoPlayer
- **Networking**: OkHttp 3.12.x + Retrofit

## Acknowledgments

- Authentication implementation based on [fnnas-api](https://github.com/FNOSP/fnnas-api) by FNOSP
- Built with Android Leanback library for TV-optimized UI
- Image loading powered by Glide
- Video playback using ExoPlayer

## License

MIT License — See LICENSE file for details

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
