# 📻 Ondes — a modern Android podcast player

A clean, ad-free, tracker-free podcast player for Android. Discover shows via
search, paste any RSS feed, or import your OPML — then listen anywhere.

## Screenshots

<p align="center">
  <img src="docs/screenshots/01-home.png" width="19%" alt="Home — latest episodes" />
  <img src="docs/screenshots/02-player.png" width="19%" alt="Now playing — full-screen player" />
  <img src="docs/screenshots/03-library.png" width="19%" alt="Subscriptions library" />
  <img src="docs/screenshots/04-search.png" width="19%" alt="Discover — browse by theme" />
  <img src="docs/screenshots/05-settings.png" width="19%" alt="Settings" />
</p>

## Features

- 🎧 **Background playback** with lock-screen & notification controls (Media3 / ExoPlayer + MediaSession)
- 🚗 **Android Auto** — browse Continue listening / Subscriptions / Downloads and play hands-free in the car
- ⏯️ Play / pause, **configurable skip intervals**, scrub
- ▶️ **Auto-play the next episode** — continuous playback through your list
- ⏩ **Variable speed** (0.8×–3×), remembered as your default, with **per-podcast speed** overrides
- 🤫 **Skip silence** & **volume boost** for clearer speech over road/train noise
- 🔖 **Chapters** — tap to jump (Podcasting 2.0 `podcast:chapters`)
- 📝 **Rich show notes** — formatted HTML with tappable links & `mm:ss` timestamps that seek
- 😴 **Sleep timer** — fixed durations or **stop at end of episode**
- 💾 **Offline downloads** (WorkManager — survives app being closed), optional **Wi-Fi-only** and **auto-delete when finished**
- 🔄 **Background refresh + new-episode notifications** for your subscriptions, with optional **per-podcast auto-download**
- 🔖 **Resume where you left off** — playback positions saved per episode
- ✅ Auto mark-as-played, "Continue listening" on the home screen
- 🔍 **Discover** podcasts (iTunes search) or paste any RSS feed URL, with a first-run **interest picker**
- 📚 Subscriptions library with pull-to-refresh, plus **filter episodes** within a show
- ♿ **Accessibility** — merged TalkBack list items with custom actions for every episode operation
- 🧾 **Up-Next queue** — a persistent, reorderable play queue ("Play next" / "Add to queue")
- 💼 **Own your data** — OPML import/export and a full local backup/restore (subscriptions, progress & settings), all on-device
- 🌍 **Localized** — English & French (`fr`)
- ⚙️ **Settings** for playback, downloads, updates, your data and appearance
- 🎨 **Material You** dynamic theming, light/dark/system theme, edge-to-edge
- 🚫 No ads, no analytics, no login

## Tech stack

| Concern        | Choice |
|----------------|--------|
| Language       | Kotlin 2.0 |
| UI             | Jetpack Compose + Material 3 |
| Playback       | AndroidX **Media3** (ExoPlayer + Session) |
| Persistence    | **Room** |
| DI             | **Hilt** |
| Async          | Coroutines + Flow |
| Background work| WorkManager |
| Networking     | OkHttp + platform XmlPullParser (RSS) |
| Images         | Coil |

`minSdk 26` (Android 8.0) · `targetSdk 35` · single-activity, MVVM.

## Get the APK on your phone

**Easiest — from CI:** every push builds an installable APK.
1. Open the repo's **Actions** tab → latest **Build APK** run.
2. Download the **`ondes-release-apk`** artifact and unzip it.
3. Copy `app-release.apk` to your phone, tap it, allow *install from unknown
   sources*, install.

   (When building from `main`, the same APKs are also attached to the rolling
   **`latest`** GitHub Release for a one-tap download on the phone.)

**Build it yourself:**
```bash
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```
Requires JDK 17 and the Android SDK (platform 35).

> **Signing:** the **APK** distributed here (CI artifact / GitHub Release) is
> **debug-signed** so it installs without extra setup — it is for sideloading
> only, not Play. The **Play Store AAB** is built and **upload-signed** with the
> real upload key (supplied via `keystore.properties` locally or CI env vars; see
> `keystore.properties.example`). Configure your own upload key the same way for
> store distribution.

## Project layout

```
app/src/main/java/com/ondes/podcast/
├─ data/        Room (local) · RSS + iTunes (remote) · repository · settings (DataStore) · opml + backup (data ownership)
├─ playback/    Media3 service, controller bridge, sleep timer
├─ download/    WorkManager episode downloader
├─ sync/        Periodic feed refresh worker + new-episode notifications
├─ ui/          Compose screens, theme, navigation, components
├─ di/          Hilt modules
├─ OndesApp     Application — WorkManager + notification channel setup
└─ MainActivity
```

## Notes

- Ondes is an independent player. It streams the publicly published RSS feeds
  you subscribe to and is not affiliated with any podcast or publisher.
- The internal package/applicationId remains `com.ondes.podcast` for
  historical reasons; the user-facing name is **Ondes**.
