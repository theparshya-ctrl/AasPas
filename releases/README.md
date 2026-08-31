# AasPas APK releases

Versioned debug and release APKs for physical device testing (POCO F1, Samsung S23, etc.).

## Folder layout

```
releases/
├── current/     ← exactly one versioned APK + latest.apk alias
└── archive/     ← all previous versioned APKs (never deleted by scripts)
```

Gradle build output under `android/app/build/outputs/apk/` is temporary only. **Do not** treat it as the canonical release copy.

## Version naming

Read from `android/app/build.gradle.kts`:

- `versionName` — e.g. `0.1.0`
- `versionCode` — e.g. `1`, `2`, `3` (must always increase for updates)

Filename format:

| Build | Pattern |
|-------|---------|
| Debug (DEV) | `AasPas-DEV-v{versionName}-build-{versionCode}.apk` |
| Beta | `AasPas-BETA-v{versionName}-build-{versionCode}.apk` |
| Release (PROD) | `AasPas-PROD-v{versionName}-build-{versionCode}.apk` |

Example: `AasPas-DEV-v0.1.0-build-1.apk`

## Build and publish

From the project root:

```bat
Build-AasPas-APK.bat
Build-AasPas-BETA-APK.bat
```

Flow:

1. `assembleDebug`
2. `testDebugUnitTest`
3. `lintDebug`
4. If all pass → `scripts/archive_and_publish_apk.ps1`

If any step fails, **nothing** is published to `releases/current/`.

Manual publish (after a successful build only):

```powershell
powershell -File scripts\archive_and_publish_apk.ps1 -BuildType Debug
```

## Install on devices

Install scripts use **`releases/current/latest.apk`** (same bytes as the versioned file). They use `adb install -r` and **do not** clear app data.

| Script | Target |
|--------|--------|
| `Install-AasPas-POCO.bat` | POCO F1 (beryllium) |
| `Install-AasPas-Samsung.bat` | Samsung phones |

If more than one matching device is connected, you will be asked to pick a serial number.

## Check installed versions

```bat
Check-AasPas-Devices.bat
```

Shows serial, installed version name, version code, and environment per connected device.

## Rules

- **Never** manually overwrite files in `archive/`.
- **Never** delete archive APKs from the release script.
- **Never** reuse a lower `versionCode` for an update.
- Bump `versionCode` (and usually `versionName`) in `build.gradle.kts` before each new release intended for devices.
- APK binaries under `releases/current/` and `releases/archive/` are **not** committed to git.

## Version progression example

| versionName | versionCode | File |
|-------------|-------------|------|
| 0.1.0 | 1 | AasPas-DEV-v0.1.0-build-1.apk |
| 0.1.0 | 2 | AasPas-DEV-v0.1.0-build-2.apk |
| 0.1.1 | 3 | AasPas-DEV-v0.1.1-build-3.apk |
