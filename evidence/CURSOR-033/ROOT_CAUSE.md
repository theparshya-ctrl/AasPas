# CURSOR-033 Root Cause

## Save Photo button missing (confirmed via POCO screenshot + code audit)

**Symptom:** Manage Shop Photo shows helper text "Choose a photo, then tap Save photo to upload" but no Save button after gallery/camera selection.

**Cause:** CURSOR-032 only showed Save when `pendingPhotoBytes != null`. Gallery/camera callbacks called `ImageCompressor.compressImage()` **before** setting pending state. On POCO F1 / MIUI gallery, compression often returned `null` (decode failure for gallery URI/format) with **no error shown** — so `pendingPhotoBytes` stayed null and UI remained in pre-selection mode.

**Fix (CURSOR-033):**
1. Store selected `Uri` immediately in `pendingPhotoUri` → preview + Save button appear without waiting for compression.
2. Compress JPEG bytes only when user taps **Save photo** (with visible error if compression fails).
3. Improved `ImageCompressor` with `ImageDecoder` (API 28+) fallback for gallery formats.
4. Photo-only screen hides read-only profile fields so Save stays near the photo preview.
5. Save button placed **first** below preview in pending state.

## Customer photo blank

**Cause:** Upload never happened because Save was unreachable (above). No `photo_storage_key` in DB for affected shops (e.g. Prashant).

**Expected after fix:** Save → POST `/api/v1/shops/me/photo` → relative `/media/...` URL → customer Coil loads via `MediaUrlResolver`.

## Evidence captured

| File | Status |
|------|--------|
| `01_before_selection.png` | POCO screencap — broken state (no Save, blank preview) |
| `02_after_selection.png` | NOT REACHED — requires post-fix manual gallery pick on device |
| `03_save_state.png` | NOT REACHED |
| `04_uploading.png` | NOT REACHED |
| `05_upload_success.png` | NOT REACHED |
| `06_owner_profile.png` | NOT REACHED |
| `07_customer_shop_details.png` | NOT REACHED |
| `08_customer_after_replacement.png` | NOT REACHED |

Re-test on POCO with updated APK (installed CURSOR-033 build).
