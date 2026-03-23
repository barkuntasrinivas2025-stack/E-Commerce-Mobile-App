# How to Open in Android Studio

## Step 1 — Prerequisites
Make sure you have installed:
- **Android Studio Hedgehog (2023.1.1)** or newer — download from developer.android.com/studio
- **JDK 17** — Android Studio bundles this, no separate install needed

## Step 2 — Open the Project
1. Open Android Studio
2. Click **"Open"** (NOT "Import Project")
3. Navigate to the `ConfidenceCommerce` folder
4. Click **OK**

Android Studio will detect `settings.gradle.kts` and recognise it as a Gradle project.

## Step 3 — Let Gradle Sync
The first sync downloads all dependencies (~500MB). This takes 3–10 minutes
depending on your internet speed. You will see a progress bar at the bottom.

**If you see "Gradle JDK not configured":**
- Go to File → Project Structure → SDK Location
- Set Gradle JDK to "Embedded JDK (17)"

## Step 4 — Fix the SDK Path
Android Studio will likely show a yellow banner:
> "SDK location not found"

Click the banner or go to:
- **File → Project Structure → SDK Location**
- Set "Android SDK location" to your local SDK path
  (usually `C:\Users\YOUR_NAME\AppData\Local\Android\Sdk` on Windows)

## Step 5 — Fix google-services.json
The Firebase `google-services.json` in `app/` is a placeholder.
Either:
- **Option A (Quick):** Remove these lines from `app/build.gradle.kts`:
  ```
  alias(libs.plugins.google.services)
  alias(libs.plugins.firebase.crashlytics)
  ```
  And remove Firebase dependencies — lets you build without Firebase.

- **Option B (Full):** Create a free Firebase project at console.firebase.google.com,
  add an Android app with package name `com.confidencecommerce`,
  download the real `google-services.json` and replace the placeholder.

## Step 6 — Run the App
- Connect an Android phone (USB debugging on) OR start an emulator
- Click the green **Run ▶** button
- Select your device

## Common Errors & Fixes

| Error | Fix |
|-------|-----|
| `SDK location not found` | File → Project Structure → set SDK path |
| `google-services.json` missing fields | Replace with real Firebase file or remove Firebase plugin |
| `Gradle sync failed: timeout` | Check internet connection, try again |
| `JDK version mismatch` | File → Project Structure → Gradle JDK → Embedded JDK 17 |
| `JAVA_HOME not set` | Android Studio → Settings → Build → Gradle → Gradle JDK → Embedded |
