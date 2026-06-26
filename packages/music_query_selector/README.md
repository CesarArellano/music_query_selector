<div align=center>

# music_query_selector

[![Platforms](https://img.shields.io/badge/Platforms-Android%20%7C%20iOS%20%7C%20Web-9cf?&style=flat-square)]()
[![Languages](https://img.shields.io/badge/Languages-Dart%20%7C%20Kotlin%20%7C%20Swift-9cf?&style=flat-square)]()

[Flutter](https://flutter.dev/) plugin to query audio/song metadata [title, artist, album, etc.] from device storage.

**This is a fork of [on_audio_query](https://github.com/LucJosin/on_audio_query) by [Lucas Josino](https://github.com/LucJosin).**
The original package is published at [pub.dev/packages/on_audio_query](https://pub.dev/packages/on_audio_query)
and licensed under the [MIT License](https://github.com/LucJosin/on_audio_query/blob/main/on_audio_query/LICENSE).

This fork fixes several iOS-specific bugs present in `on_audio_query_ios` v1.1.0
that make the plugin non-functional on iOS devices.

</div>

---

## What's different from the original

| Fix | File | Description |
|-----|------|-------------|
| Permission race condition | `PermissionController.swift` | `requestPermission()` used to return `false` immediately because `MPMediaLibrary.requestAuthorization` fires its callback asynchronously. Fixed with `DispatchSemaphore` on a background thread. |
| Empty songs list | All 8 query files | `MPMediaItemPropertyIsCloudItem: false` predicate excluded Apple Music songs that are downloaded locally but still flagged as cloud-backed. Removed the predicate; `assetURL != nil` guard retained. |
| SPM support | `ios/Package.swift` | Added Swift Package Manager manifest — Flutter was warning this would become a build error. |
| Blocking `sleep(1)` | `PlaylistController.swift` | Replaced blocking sleep with proper async dispatch. |
| Broken playlist guard | `WithFiltersQuery.swift` | Fixed guard that appended empty dicts for non-matching playlists. |

---

## Topics:

* [Installation](#installation)
* [Platforms](#platforms)
* [Overview](#overview)
* [Examples](#examples)
* [License](#license)

---

## Platforms:

|  Methods  |   Android   |   iOS   |   Web   |
|-----------|:-----------:|:-------:|:-------:|
| `querySongs` | ✔️ | ✔️ | ✔️ |
| `queryAlbums` | ✔️ | ✔️ | ✔️ |
| `queryArtists` | ✔️ | ✔️ | ✔️ |
| `queryPlaylists` | ✔️ | ✔️ | ❌ |
| `queryGenres` | ✔️ | ✔️ | ✔️ |
| `queryAudiosFrom` | ✔️ | ✔️ | ✔️ |
| `queryWithFilters` | ✔️ | ✔️ | ✔️ |
| `queryArtwork` | ✔️ | ✔️ | ✔️ |
| `createPlaylist` | ✔️ | ✔️ | ❌ |
| `removePlaylist` | ✔️ | ❌ | ❌ |
| `addToPlaylist` | ✔️ | ✔️ | ❌ |
| `removeFromPlaylist` | ✔️ | ❌ | ❌ |
| `renamePlaylist` | ✔️ | ❌ | ❌ |
| `moveItemTo` | ✔️ | ❌ | ❌ |
| `permissionsRequest` | ✔️ | ✔️ | ❌ |
| `permissionsStatus` | ✔️ | ✔️ | ❌ |
| `queryDeviceInfo` | ✔️ | ✔️ | ✔️ |
| `scanMedia` | ✔️ | ❌ | ❌ |

✔️ Supported &nbsp; ❌ Not supported

---

## Installation:

Add to your `pubspec.yaml`:
```yaml
dependencies:
  music_query_selector:
    path: packages/music_query_selector
```

### Request Permissions:

#### Android (`AndroidManifest.xml`):
```xml
<!-- Android 12 or below -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>

<!-- Android 13 or greater -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO"/>
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO"/>
```

#### iOS (`Info.plist`):
```xml
<key>NSAppleMusicUsageDescription</key>
<string>$(PROJECT_NAME) requires access to the media library</string>

<key>UIBackgroundModes</key>
<array>
  <string>audio</string>
</array>
```

> **Note:** On iOS, this plugin queries the Apple Music / iTunes library via `MPMediaQuery`. Songs must be synced through Finder/iTunes or downloaded via Apple Music — arbitrary filesystem files are not accessible through this API.

---

## Overview:

### Artwork Widget

```dart
QueryArtworkWidget(
  id: audioId,
  type: ArtworkType.AUDIO,
);
```

---

## Examples:

#### Initialize

```dart
final MusicQuerySelector _audioQuery = MusicQuerySelector();
```

#### Query methods

```dart
someName() async {
  List<AudioModel> audios = await _audioQuery.querySongs();
  List<AlbumModel> albums = await _audioQuery.queryAlbums();
}
```

#### Artwork

```dart
someName() async {
  Uint8List? artwork = await _audioQuery.queryArtwork(
    audioId,
    ArtworkType.AUDIO,
  );
}
```

---

## License:

This fork is released under the same [MIT License](https://github.com/LucJosin/on_audio_query/blob/main/on_audio_query/LICENSE) as the original work.

Original author: **[Lucas Josino](https://github.com/LucJosin)** — [lucasjosino.com](https://www.lucasjosino.com/)
Fork maintainer: **[Cesar Arellano](https://github.com/CesarArellano)**

> [Back to top](#music_query_selector)
