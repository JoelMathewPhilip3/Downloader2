# Joel Browser Downloader

A local Android browser with an integrated video/audio download action. The source is ready to host on GitHub and GitHub Actions builds the debug APK.

## Main flow

1. Browse to a public video page inside the app.
2. Start playback.
3. The bottom **Download** bar appears when the page exposes an HTML video or audio element.
4. Choose MP4 quality or MP3.
5. MP3 is always converted to **192 kbps** with available title, artist, metadata, and thumbnail embedded.

The app always retains the current page URL. It also observes obvious direct MP4/WebM/HLS/DASH requests and can use a detected media request as a fallback if normal page extraction fails.

## Included

- Android WebView browser
- Address/search bar, back, forward and refresh
- Playback detection through a minimal JavaScript bridge
- Current WebView cookies passed to yt-dlp when available
- MP4: 360p, 480p, 720p, 1080p, or best available
- MP3: fixed 192 kbps
- Thumbnail and metadata embedding
- One active background download at a time
- yt-dlp nightly update on first launch and every 24 hours
- Manual downloader-engine update button in the browser toolbar
- QuickJS/EJS options for newer YouTube extraction
- Downloads saved under `Downloads/JoelDownloader`
- GitHub Actions APK build

## Build on GitHub

1. Create an empty GitHub repository.
2. Upload everything in this project folder.
3. Open **Actions** → **Build APK** → **Run workflow**.
4. Download the `JoelVideoDownloader-debug` artifact.

## Important limitations

Playback detection does not guarantee that a download is possible. Direct media and public pages supported by the current extractor are the most reliable. DRM, paid access, protected streams, unsupported sites, expiring links, and some authenticated pages will fail. YouTube extraction changes regularly, so keep the downloader engine current.

WebView is not Brave. It does not include Brave Shields, sync, extensions, or all browser compatibility features. Only download content you own or are authorized to save.
