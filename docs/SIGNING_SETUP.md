# Release signing & CI secrets — Ondes

How to sign the app for the Google Play Store. You do this **once**.

## Background — two keys

With **Play App Signing** (recommended, and effectively required for new apps),
there are two keys:

- **App signing key** — held by Google. It signs the APKs delivered to users.
  You never see it.
- **Upload key** — held by you. You sign your `.aab` with it; Google verifies
  the upload, strips your signature, and re-signs with the app signing key.

This repo's build is wired for the **upload key**. If you ever lose it, Google
can reset it (because they hold the real signing key) — but still keep it safe.

## 1. Generate the upload keystore (local, once)

From the repo root:

```bash
./scripts/setup-upload-keystore.sh
```

It runs `keytool`, asks for a keystore password and a key password (use strong
ones, store them in a password manager), writes `upload-keystore.jks`, and then
prints:

1. the contents for a local `keystore.properties`, and
2. the `gh` commands + the `KEYSTORE_BASE64` blob for CI.

> `upload-keystore.jks` and `keystore.properties` are already git-ignored.
> **Never commit them.** Back up the `.jks` and both passwords somewhere safe.

Equivalent manual command, if you prefer:

```bash
keytool -genkeypair -v -keystore upload-keystore.jks \
  -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

## 2. Local release builds

Create `keystore.properties` at the repo root (the script prints this for you):

```properties
storeFile=/absolute/path/to/upload-keystore.jks
storePassword=••••••
keyAlias=upload
keyPassword=••••••
```

Then:

```bash
./gradlew bundleRelease   # → app/build/outputs/bundle/release/app-release.aab
./gradlew assembleRelease # → app/build/outputs/apk/release/app-release.apk
```

`app/build.gradle.kts` reads `keystore.properties` (or the CI env vars below)
and signs with the upload key. With no key configured it falls back to the
debug key so plain APK builds still work — but that AAB is **not** store-ready.

## 3. CI secrets (GitHub Actions)

Set four repository secrets (Settings → Secrets and variables → Actions), or via
the `gh` CLI from the repo:

```bash
gh secret set KEYSTORE_BASE64   -b"$(base64 -w0 upload-keystore.jks)"
gh secret set KEYSTORE_PASSWORD -b'<keystore password>'
gh secret set KEY_ALIAS         -b'upload'
gh secret set KEY_PASSWORD      -b'<key password>'
```

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | the `.jks` file, base64-encoded (single line) |
| `KEYSTORE_PASSWORD` | the keystore (store) password |
| `KEY_ALIAS` | `upload` (or your chosen alias) |
| `KEY_PASSWORD` | the key password |

### How the workflow uses them

`.github/workflows/build.yml` → step **"Build release AAB"**:

- if `KEYSTORE_BASE64` is set, it decodes it to `upload-keystore.jks`, exports
  `KEYSTORE_FILE` + the password/alias env vars, and runs `bundleRelease`
  (signed with the upload key);
- if it is **not** set, the AAB is debug-signed and only good for inspection.

The signed `.aab` is uploaded as the **`ondes-release-aab`** workflow artifact.
Download it from the run and upload it to the Play Console (or wire up automated
publishing later).

## 4. Verify

After setting the secrets, trigger CI (push or "Run workflow"). Then check the
AAB's signer:

```bash
unzip -p ondes-release-aab.zip 'BUNDLE-METADATA/*' >/dev/null  # (just to extract)
# or, on a built APK:
$ANDROID_HOME/build-tools/<ver>/apksigner verify --print-certs app-release.apk
```

The certificate should match your upload key (not the Android debug cert,
`CN=Android Debug`).

## Security checklist

- [ ] `upload-keystore.jks` and `keystore.properties` are **not** committed.
- [ ] Keystore file + both passwords backed up in a password manager.
- [ ] CI secrets set; no secret value pasted into code, logs, or commits.
- [ ] Enrolled in **Play App Signing** when creating the app in the Console.
