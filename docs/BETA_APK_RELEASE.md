# Beta APK release workflow (CURSOR-062)

When shipping a new **AasPas BETA** APK to testers:

## Prerequisites

- Neon Object Storage `public_read` bucket configured on Beta (see `docs/BETA_DEPLOYMENT_RENDER.md`)
- `.env.beta` contains `AWS_*` / `MEDIA_PUBLIC_BASE_URL` for uploads
- Render Beta backend deployed from `main`

## Publish steps

1. **Bump Android version** in `android/app/build.gradle.kts`:
   - Increase `versionCode` by at least 1 (never reuse a lower code)
   - Update `versionName` as needed (e.g. `0.1.4`)

2. **Build & publish locally**
   ```powershell
   powershell -File scripts\publish_beta_release.ps1 -ReleaseNotes "Fix summary","Another change"
   ```
   Or manually:
   ```bat
   Build-AasPas-BETA-APK.bat
   powershell -File scripts\sync_beta_app_version.ps1 -ReleaseNotes "Your notes here"
   python scripts\upload_beta_apk.py
   ```

3. **Stable download URL** (HTTPS, public):
   ```
   {MEDIA_PUBLIC_BASE_URL}/beta/android/latest.apk
   ```
   Example: `https://<branch>.storage.c-2.us-east-2.aws.neon.tech/<bucket>/beta/android/latest.apk`

4. **Update backend metadata on Render**
   - Commit `deploy/beta/app_version.json` if changed
   - Manual deploy on Render (`autoDeploy: false`)

5. **Verify API**
   ```powershell
   python -c "import json,urllib.request as u; print(u.urlopen('https://aaspas-beta.onrender.com/api/v1/app/version',timeout=60).read().decode())"
   ```
   Confirm `latest_version_code`, `download_url` (HTTPS), and `release_notes`.

6. **Verify update detection**
   - Install an **older** Beta APK on a phone
   - Open app → dialog: "New AasPas Beta update available"
   - Tap **Update** → browser/download opens stable APK URL
   - Install over existing app (data preserved)

7. **Share with testers**
   - APK for USB/adb: `releases/current/latest.apk`
   - Or share the stable Neon HTTPS URL above

## Single source of truth

| Item | Source |
|------|--------|
| Installed APK version | `android/app/build.gradle.kts` → `BuildConfig` |
| API latest version | `deploy/beta/app_version.json` (synced by script) |
| APK bytes at stable URL | Neon S3 key `beta/android/latest.apk` |

Never publish an APK without syncing `app_version.json` and uploading to S3.

## In-app behavior

- Beta builds only (`APP_ENVIRONMENT=BETA`)
- Checks `GET /api/v1/app/version` once per app session (public API, no auth)
- Compares `latest_version_code` vs installed `VERSION_CODE`
- Optional update: **Later** dismisses until next cold start
- Mandatory update (`mandatory: true` in JSON): dialog cannot be dismissed with Later
