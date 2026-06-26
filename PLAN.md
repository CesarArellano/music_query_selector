# Music Player App — Architecture Refactor Plan

---

## Phase 5 — Fork `on_audio_query_ios`

### Context

`on_audio_query_ios` v1.1.0 has three bugs that make the iOS app non-functional and one structural issue that generates a Flutter build warning. The package is effectively unmaintained for iOS. A local fork inside the project repo gives full control without waiting on upstream.

**Issues being fixed:**

| # | Severity | File | Problem |
|---|----------|------|---------|
| 1 | Critical | `PermissionController.swift` | `requestPermission()` returns `false` immediately due to a race condition — `MPMediaLibrary.requestAuthorization` fires its callback asynchronously but the function returns before it executes |
| 2 | Critical | `AudioQuery.swift` + 5 other query files | Cloud filter blocks Apple Music downloads (cloud-backed but locally cached) — empty list even when user has downloaded songs |
| 3 | High | missing | No `Package.swift` — Flutter warns this will become an error; CocoaPods-only setup |
| 4 | Medium | `PlaylistController.swift:28` | `sleep(1)` is a blocking call on whatever thread the method channel runs on |
| 5 | Low | `WithFiltersQuery.swift:145-148` | Playlist cloud filter checks first item but still appends all items; broken guard |

---

### 5.1 — Create the local fork

Copy the plugin into the project as a standalone Flutter plugin package:

```
packages/
└── on_audio_query_ios/          ← new local fork
    ├── pubspec.yaml
    └── ios/
        ├── Package.swift          ← NEW
        ├── on_audio_query_ios.podspec  (update version + remove SwiftyBeaver)
        └── Classes/
            └── ... (all existing Swift + ObjC files)
```

Place it at `packages/on_audio_query_ios/` relative to the project root. Then override the dependency in the app's `pubspec.yaml`:

```yaml
dependency_overrides:
  on_audio_query_ios:
    path: packages/on_audio_query_ios
```

---

### 5.2 — Fix `PermissionController.swift` (permission race condition)

**Root cause:** `MPMediaLibrary.requestAuthorization` calls its completion handler on an arbitrary background queue, but the function returns before the handler fires.

**Fix:** Dispatch the auth call from a background thread and block that thread with a `DispatchSemaphore` until the callback signals. This avoids deadlocking the main thread.

Also update `SwiftOnAudioQueryPlugin.swift` so `permissionsRequest` is handled on a background queue before calling back to Flutter on the main queue:

```swift
// SwiftOnAudioQueryPlugin.swift — in the method switch
case "permissionsRequest":
    DispatchQueue.global(qos: .userInitiated).async {
        let granted = PermissionController.requestPermission()
        DispatchQueue.main.async { result(granted) }
    }
    return
```

```swift
// PermissionController.swift
public static func requestPermission() -> Bool {
    let semaphore = DispatchSemaphore(value: 0)
    var granted = false
    MPMediaLibrary.requestAuthorization { status in
        granted = status == .authorized
        semaphore.signal()
    }
    semaphore.wait()   // safe — called from background thread
    return granted
}
```

---

### 5.3 — Fix cloud item filter (empty song list)

**Root cause:** `MPMediaItemPropertyIsCloudItem: false` excludes songs that are in Apple Music and downloaded locally but still flagged as cloud-backed items.

**Fix:** Remove the cloud predicate from all query files. Keep the per-item guard `song.assetURL != nil` (which already ensures the file is locally accessible). Files to update:

- `queries/AudioQuery.swift`
- `queries/AudioFromQuery.swift`
- `queries/AlbumQuery.swift`
- `queries/ArtistQuery.swift`
- `queries/GenreQuery.swift`
- `queries/PlaylistQuery.swift`
- `queries/ArtworkQuery.swift`
- `queries/WithFiltersQuery.swift`

Pattern to remove from each (8 occurrences total):
```swift
let cloudFilter = MPMediaPropertyPredicate(
    value: false,
    forProperty: MPMediaItemPropertyIsCloudItem
)
cursor.addFilterPredicate(cloudFilter)
```

Also fix the broken guard in `WithFiltersQuery.swift` — move the `!song.isCloudItem` check inside a proper `guard` that skips the item instead of still appending it.

---

### 5.4 — Fix `sleep(1)` in `PlaylistController.swift`

Replace the blocking `sleep(1)` with a proper async pattern. Dispatch the query to a background thread and signal completion via a `DispatchGroup`:

```swift
// Before
func addToPlaylist(...) {
    ...
    sleep(1)   // ← blocking
    result(hasBeenAdded)
}

// After — dispatch result back via DispatchGroup
DispatchGroup with notify on main queue → result(hasBeenAdded)
```

---

### 5.5 — Add `Package.swift` (Swift Package Manager support)

Create `packages/on_audio_query_ios/ios/Package.swift`. SwiftyBeaver supports SPM natively.

```swift
// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "on_audio_query_ios",
    platforms: [.iOS(.v12)],
    products: [
        .library(name: "on_audio_query_ios", targets: ["on_audio_query_ios"])
    ],
    dependencies: [
        .package(url: "https://github.com/SwiftyBeaver/SwiftyBeaver.git", from: "2.0.0")
    ],
    targets: [
        .target(
            name: "on_audio_query_ios",
            dependencies: [
                .product(name: "SwiftyBeaver", package: "SwiftyBeaver")
            ],
            path: "Classes"
        )
    ]
)
```

Update the podspec to set `s.version = '1.1.1'` and ensure `s.swift_version = '5.9'`.

---

### 5.6 — Wire up `dependency_overrides` in `pubspec.yaml`

```yaml
# In the app's pubspec.yaml
dependency_overrides:
  on_audio_query_ios:
    path: packages/on_audio_query_ios
```

Then run `flutter pub get` and `cd ios && pod install --repo-update` to pick up the local fork. Dart-side code (`OnAudioQueryRepository`, `LibraryCubit`) requires no changes.

---

### Verification

1. `flutter analyze` — zero issues
2. Build on physical iOS device (simulator has no music library)
3. First launch: permission dialog appears → tap Allow → songs load
4. Subsequent launches: songs load immediately without dialog
5. Confirm Apple Music–downloaded songs appear (previously filtered by cloud predicate)
6. Build warning `on_audio_query_ios does not support Swift Package Manager` should be gone

---