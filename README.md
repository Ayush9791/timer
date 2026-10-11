# Study Time Recorder for Android

A native Android stopwatch and study tracker built with Kotlin and Jetpack Compose. It uses local SharedPreferences storage and requires no account or internet connection at runtime.

## Features
- Black rounded timer display with light-grey seven-segment digits and red Start/Resume button.
- Separate Pause button that pauses without saving or resetting.
- Finish & Save opens a form to confirm the subject, edit the duration in minutes, and add optional notes.
- Session history, with subject, duration, timestamp and notes.
- Basic performance analytics: today's total, sessions completed today, last-seven-days total and subject breakdown.
- Optional CSV export is not currently included; session data is stored locally on the device.
- No network permission or cloud database.

## Build an APK without Android Studio
1. Open this repository on GitHub.
2. Select **Actions**.
3. Select **Build Android APK** and click **Run workflow**.
4. When the workflow succeeds, open its run and download the `TimeRecorder-debug-apk` artifact.
5. Extract the artifact and install `app-debug.apk` on your Android phone. Android may ask you to allow installation from that source.

## Local data
Session records live in this app's private SharedPreferences storage. They are not uploaded anywhere. Uninstalling the app or clearing its app data removes the saved records.

## Build configuration
- Minimum Android: 7.0 (API 24)
- Compile SDK: 35
- JVM: 17
