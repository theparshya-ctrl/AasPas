# AasPas Customer Android App

Kotlin + Jetpack Compose customer app for AasPas local commerce.

## Prerequisites

- Android Studio Ladybug or newer
- Android SDK 35
- JDK 17
- Running AasPas FastAPI backend (see project root `README.md`)

## Validation prerequisites

PostgreSQL must be running before backend validation:

```powershell
# Option A: Docker (recommended)
docker compose up -d postgres

# Option B: local PostgreSQL on localhost:5432 with database/user aaspas/aaspas
```

Without PostgreSQL, `alembic upgrade head`, seed, and live Home API checks cannot run.

The debug build uses a configurable base URL via `BuildConfig.API_BASE_URL`.

| Environment | Default URL | Notes |
|-------------|-------------|-------|
| Android Emulator | `http://10.0.2.2:8000/` | Maps emulator localhost to host machine. Used when `API_BASE_URL` is unset. |
| Physical device (DEBUG) | `http://<tailscale-ipv4>:8000/` | Set `API_BASE_URL` in `android/local.properties`. Phone and laptop do not need the same Wi-Fi. |
| Production (release) | `https://api.aaspas.example/` | Separate release `buildConfigField`; not the debug Tailscale URL. |

When PostgreSQL is not installed, start a SQLite dev API from the repo root:

```powershell
.\scripts\run_dev_phone.ps1
```

### Start backend

```powershell
cd H:\AasPas
.\.venv\Scripts\activate
alembic upgrade head
python scripts/seed_pilot.py
uvicorn aaspas.main:app --reload --app-dir src --host 0.0.0.0 --port 8000
```

Cleartext HTTP is enabled only for debug builds (`usesCleartextTraffic="true"`).

## Build & Test

```powershell
cd android
.\gradlew.bat assembleDebug
```

Debug APK:

`android/app/build/outputs/apk/debug/app-debug.apk`

Full check (build + unit tests + lint):

```powershell
cd android
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --no-daemon
```

### Check installed version on a phone

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb shell dumpsys package com.aaspas.customer | Select-String -Pattern "versionName|versionCode|pkg="
```

Or open **Profile** in the app and read **About / App Version**.

### Distinguish DEV vs production

| | DEBUG | RELEASE |
|--|-------|---------|
| Launcher name | **AasPas DEV** | **AasPas** |
| `applicationId` | `com.aaspas.customer` (same; updates overwrite the existing app) | `com.aaspas.customer` |
| About environment | `DEV` | `PRODUCTION` |
| DEV chip | `DEV • v0.1.0 (1)` | Hidden |
| `versionName` / `versionCode` | `0.1.0` / `1` | `0.1.0` / `1` |

Do not change `applicationId` for testing. That would install a second app. There is no in-app updater and no Play Store publishing yet.

## Architecture

```
presentation/   Compose UI, ViewModels, navigation, theme
domain/         Models and repository interfaces
data/           Retrofit DTOs, mappers, repository implementations
core/           Network, location, shared utilities
```

Home consumes `GET /api/v1/home` with optional `latitude` and `longitude` query params.

Location permission is requested on first Home load. If denied, Home still loads without coordinates.

## Configuration

### API base URL (physical phone)

Prefer **Tailscale**, not LAN Wi-Fi. On the laptop run `tailscale ip -4`, then in `android/local.properties` (gitignored):

```properties
API_BASE_URL=http://100.x.y.z:8000/
```

Emulator development can omit this key and keep the Gradle default `http://10.0.2.2:8000/`. Do not hard-code `10.0.2.2` for physical-device testing. Do not hard-code URLs inside Composables or repositories.

FastAPI stays on `0.0.0.0:8000` for local development. Do **not** port-forward 8000 on the router; Tailscale is the private path.

### Google Maps (primary provider) — CURSOR-013

**Application ID:** `com.aaspas.customer`

#### Google Cloud Console checklist

1. Create or open a [Google Cloud project](https://console.cloud.google.com/)
2. Enable **Maps SDK for Android** (APIs & Services → Library)
3. Enable **billing** on the project (Google requires it for Maps; free monthly credit applies)
4. Create an **API key** (APIs & Services → Credentials)
5. **Restrict the key:**
   - Application restrictions → **Android apps**
   - Add package name: `com.aaspas.customer`
   - Add your debug **SHA-1** (and SHA-256 if requested)

Print debug fingerprints:

```powershell
.\scripts\get_android_maps_sha.ps1
```

Example debug SHA-1 (this machine): `69:65:41:5F:79:26:04:DC:C1:9C:CA:82:54:06:84:D2:C6:DF:D1:55`

6. Under API restrictions, limit the key to **Maps SDK for Android** only.

#### Add key locally (never commit)

Copy `android/local.properties.example` to `android/local.properties` and set:

```properties
MAPS_API_KEY=your_google_maps_android_key
```

- Injected via `BuildConfig.MAPS_API_KEY` and `AndroidManifest.xml` meta-data `com.google.android.geo.API_KEY`
- Initialized at startup via `GoogleMapsInitializer` (only when key is non-empty)
- **Do not** log, print, or commit the key
- `android/local.properties` is gitignored

#### Provider selection

| Condition | Provider |
|-----------|----------|
| `MAPS_API_KEY` set + Google loads | **Google Maps** |
| Key missing | **OSM (osmdroid)** |
| Google init fails (timeout/invalid key) | **OSM fallback** |

Rebuild after adding the key:

```powershell
cd android
.\gradlew.bat installDebug
```

When `MAPS_API_KEY` is set, **View Map** uses Google Maps Compose (`maps-compose 6.2.1`).

### OpenStreetMap fallback

When `MAPS_API_KEY` is empty or Google Maps fails to initialize:

- The app uses **native OSM tiles** via **osmdroid** (`OpenStreetMapProviderView`).
- **List mode is NOT used as automatic fallback** — the user still sees a map.
- Required attribution is shown: **© OpenStreetMap contributors**
- OSM tile usage must comply with the [OpenStreetMap tile usage policy](https://operations.osmfoundation.org/policies/tiles/). Pilot/dev usage of the public tile endpoint is acceptable; production should use a dedicated tile provider or self-hosted tiles.

**Library evaluation (CURSOR-012):**

| Library | License | Status | Decision |
|---------|---------|--------|----------|
| Google Maps Compose | Google ToS | Primary | Used when key configured |
| osmdroid | Apache 2.0 | Stable | OSM fallback on device |

### Map provider architecture

```
MapRoute / MapViewModel (provider-independent business logic)
    └── MapDiscoveryView
            ├── GoogleMapProviderView   (when key configured)
            └── OpenStreetMapProviderView (fallback)
```

Selection logic: `MapProviderSelector` in `presentation/map/provider/`.

Business logic (markers, location, offers) stays in `MapViewModel` + `MapMarkerGrouper`.

## Map behavior (CURSOR-012)

| Behavior | Implementation |
|----------|----------------|
| Default view | **Map** (not List) |
| List mode | User must tap **List** explicitly |
| Markers | One per shop with visible offers |
| Map center | `SelectedLocationStore` (manual or GPS) |
| Marker tap | Bottom sheet with shop + offer details |
| Directions | Shop lat/lng via `DirectionsIntentBuilder` |
| Category filter | `categoryName` nav arg filters Home offers |
| No location | Prompt to open location selection |

### Known limitations

- **Search → Map**: Search API results do not include shop coordinates. Search cannot open the map with result markers without backend changes.
- **Category → Map**: Category Offers screen has a **View Map** button. Navigates via `Routes.map(categoryId, categoryName)` and filters markers to that category only (today + coming soon).
- **Real markers on device**: Require live backend with seeded shops that have coordinates.

## Development vs production

| Setting | Development | Production |
|---------|-------------|------------|
| `API_BASE_URL` | Tailscale `100.x` (phone) or `10.0.2.2` (emulator) | HTTPS API domain |
| App label | `AasPas DEV` | `AasPas` |
| About environment | `DEV` | `PRODUCTION` |
| `MAPS_API_KEY` | Optional (OSM fallback) | Required for best UX |
| Cleartext HTTP | Enabled (debug) | Disabled |
| OSM tiles | Public endpoint (pilot) | Dedicated tile provider recommended |
