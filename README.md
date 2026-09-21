# Android HealthCheck

A minimal Android app that shows live CPU and memory usage:

- **System CPU** — read from `/proc/stat` (may be unavailable on some Android 8+ devices due to SELinux restrictions).
- **App CPU** — this app's own CPU usage via `Process.getElapsedCpuTime()` (always available).
- **Memory** — device memory usage via `ActivityManager.MemoryInfo` and the app's JVM heap usage.

## Build

Every push to `main` triggers a GitHub Actions workflow (`.github/workflows/build.yml`) that builds a debug APK and uploads it as a workflow artifact. Find it under the **Actions** tab → the latest run → **Artifacts** → `app-debug-apk`.

To build locally, open the project in Android Studio, or run:

```
gradle assembleDebug
```

(requires a local Android SDK; no Gradle wrapper is checked in — CI installs Gradle itself).
