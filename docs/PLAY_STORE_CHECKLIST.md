# 🚀 Google Play Store — publication checklist

Tracking everything required to ship **Carne** on the Google Play Store.
Carne was originally built for sideloading (no store, no account), so this
covers the gap to a production Play listing.

Legend: ✅ done · 🟡 partially done / needs action · ⬜ not started ·
🔴 blocker.

---

## 1. Build & signing (in this repo)

| # | Item | Status | Notes |
|---|------|--------|-------|
| 1.1 | Release signing config reads an **upload key** from `keystore.properties` / CI env vars, falls back to debug key when absent | ✅ | `app/build.gradle.kts`. See `keystore.properties.example`. |
| 1.2 | Generate the real **upload keystore** and store it safely (1Password / secret manager) | ⬜ | `keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`. **Never commit it.** Losing it = no more updates (unless on Play App Signing). |
| 1.3 | Add CI secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | ⬜ | `base64 -w0 upload-keystore.jks` → `KEYSTORE_BASE64`. Repo → Settings → Secrets → Actions. |
| 1.4 | Build the **Android App Bundle (.aab)** — Play requires AAB, not APK | ✅ | `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`. CI uploads `carne-release-aab`. |
| 1.5 | `targetSdk` meets Play's current minimum for new apps | ✅ | `targetSdk = 36` (Android 16), `compileSdk = 36`. |
| 1.6 | **16 KB page size** compliance (required for new apps/updates on Android 15+) | 🟡 | Media3/ExoPlayer ship native `.so`. Verify with the bundle: extract `.aab`, check `.so` alignment, or use the Play Console pre-launch report. Bump Media3 + AGP if flagged. |
| 1.7 | `versionCode` strictly increases on every upload | ✅ | Derived from semver in `app/build.gradle.kts` (major*10000 + minor*100 + patch). v1.4.0 → `10400`. |
| 1.8 | Backup / data-extraction rules declared (Android 12+) | ✅ | `data_extraction_rules.xml` + `backup_rules.xml` referenced in the manifest. |
| 1.9 | R8/ProGuard release build verified (no crash from stripped classes) | 🟡 | Smoke-test the release AAB on a device before submitting. |

## 2. Google Play Console — account & process

| # | Item | Status | Notes |
|---|------|--------|-------|
| 2.1 | Developer account ($25 one-time) + **identity verification** | ⬜ | Personal accounts now require identity verification before publishing. |
| 2.2 | **Closed testing** before production | ⬜ | New personal accounts must run a closed test with **≥12 testers for ≥14 days** to unlock production. Plan this lead time. |
| 2.3 | Create the app, choose **Music & Audio** category, free | ⬜ | |
| 2.4 | Enroll in **Play App Signing** | ⬜ | Recommended — Google manages the app signing key; you keep the upload key. |
| 2.5 | Internal testing track for fast iteration | ⬜ | Optional but useful while filling forms. |

## 3. Policy & declarations (Play Console forms)

| # | Item | Status | Notes |
|---|------|--------|-------|
| 3.1 | **Privacy policy URL** (hosted, public) | 🔴 ⬜ | Mandatory for all apps. Draft below in §6. Host it (GitHub Pages works). |
| 3.2 | **Data safety** form | ⬜ | Carne stores everything on-device, no accounts, no analytics/ads. Declare: no data collected/shared. Mention network calls to iTunes search + RSS feeds (not "collection" per Play's definition, but be accurate). |
| 3.3 | **Foreground service** declaration | ⬜ | Justify `FOREGROUND_SERVICE_MEDIA_PLAYBACK` — background audio playback (core feature). |
| 3.4 | **Content rating** (IARC) questionnaire | ⬜ | Likely "Everyone"; podcast content is user-fetched, so review the music/streaming questions. |
| 3.5 | **Ads** declaration | ⬜ | No ads. |
| 3.6 | **App access** | ⬜ | No login required — declare full access, no credentials needed. |
| 3.7 | **Target audience & content** | ⬜ | Not directed at children. |
| 3.8 | Government / financial / health declarations | ✅ N/A | None apply. |
| 3.9 | Account/data **deletion** disclosure | ✅ N/A | No account; data is local and removable via uninstall + in-app backup/restore. Note in Data safety if asked. |

## 4. Store listing assets

| # | Item | Status | Notes |
|---|------|--------|-------|
| 4.1 | App name (≤30 chars) | 🟡 | "Carne" — see §5 branding risk before locking it in. |
| 4.2 | Short description (≤80) + full description (≤4000) | ⬜ | Reuse README feature list. EN + FR (app is localized). |
| 4.3 | **Hi-res icon 512×512 PNG** (32-bit, alpha) | ⬜ | App only ships adaptive/vector icons; export a 512² PNG for the listing. |
| 4.4 | **Feature graphic 1024×500 PNG/JPG** | ⬜ | |
| 4.5 | Phone screenshots (2–8, min 320px, 16:9 or 9:16) | 🟡 | 5 exist in `docs/screenshots/`. Verify resolution meets Play minimums. |
| 4.6 | (Optional) tablet / Android Auto / 7"+10" screenshots | ⬜ | App supports Android Auto — consider a car screenshot. |
| 4.7 | Contact email + (optional) website | 🟡 | Email available; decide on a public support contact. |

## 5. ⚠️ Branding / legal risk — read before submitting

Carne is named after, themed around, and **pre-subscribes** the third-party
podcast **"Silicon Carne"** by Carlos Diaz, with no affiliation (per the
README). On Google Play this can trip the **Impersonation** and
**Intellectual Property** policies (app name + auto-seeded show implying an
official relationship).

**Options (pick one before publishing):**
- ⬜ Obtain **written permission** from Carlos Diaz / Silicon Carne to use the
  name & seed the feed, and add a clear "unofficial / fan-made" disclaimer.
- ⬜ **Neutralize the branding**: rename the app to something generic, remove
  the auto-seeded Silicon Carne feed (or make it just one suggestion among
  many in the interest picker), and adjust store copy.

This is the single most likely reason for a **rejection or takedown**, so
resolve it deliberately.

## 6. Privacy policy (draft to host)

A minimal, accurate policy to publish at a public URL (e.g. GitHub Pages):

> **Carne — Privacy Policy.** Carne does not collect, store, or share any
> personal data. There are no accounts, no analytics, and no advertising. All
> your data — subscriptions, listening progress, downloads, and settings — is
> stored only on your device and is never transmitted to us. The app makes
> direct network requests to: (1) podcast RSS feeds you subscribe to, (2) the
> Apple iTunes Search API when you search for shows, and (3) episode media/
> artwork hosts, in order to stream and download content. These third parties
> receive standard request metadata (such as your IP address) as required to
> deliver the content; Carne sends them no additional personal information.
> Uninstalling the app removes all locally stored data. Contact: <email>.

---

## Suggested order of operations

1. Resolve the **branding decision** (§5) — it may change the app name/assets.
2. Generate the **upload keystore** (1.2) and add **CI secrets** (1.3).
3. **Host the privacy policy** (3.1 / §6).
4. Build the **signed AAB** via CI, smoke-test on a device (1.4, 1.6, 1.9).
5. Create the Play Console app, fill **all policy forms** (§3) and the
   **store listing** (§4).
6. Run **closed testing** (≥12 testers / 14 days) (2.2), watch the
   **pre-launch report**.
7. Submit for **production** review.
