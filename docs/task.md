# Implementation Tasks & Checklist: Tsuyu

## Milestone 1: Foundation & Setup (Currently Debugging)
- [x] Create project with Minimum SDK 24, Kotlin DSL.
- [x] Configure dependencies (`mlkit-text-recognition-japanese`, `kotlinx-coroutines-android`).
- [x] Declare permissions & metadata in `AndroidManifest.xml`.
- [x] Implement permission chain in `MainActivity.kt`.
- [ ] **[WIP]** Implement and stabilize `ScreenCaptureService.kt` (Ensure notification appears and no `SecurityException` crashes occur on Android 14+).

## Milestone 2: Frame Capturing (Single-Shot)
- [x] Task 2.1: Create `ScreenCaptureEngine.kt` to encapsulate `VirtualDisplay` and `ImageReader`.
- [x] Task 2.2: Implement plane buffer to `Bitmap` conversion with row stride/padding math.
- [x] Task 2.3: Wire capture trigger from `ScreenCaptureService` to return a raw full-screen `Bitmap`.
- [x] Task 2.4: Ensure immediate release of `VirtualDisplay` and `ImageReader` after one frame.

## Milestone 3: Transparent Crop Overlay
- [ ] Task 3.1: Create custom view `CropOverlayView.kt` with touch handling (`ACTION_DOWN`, `ACTION_MOVE`, `ACTION_UP`) for drawing a rectangle.
- [ ] Task 3.2: Create `OverlayManager.kt` using `WindowManager` to attach/detach the view with `FLAG_NOT_FOCUSABLE`.
- [ ] Task 3.3: Implement callback to pass crop coordinates `(left, top, right, bottom)` back to the service and remove the overlay.

## Milestone 4: OCR Extraction Pipeline
- [ ] Task 4.1: Implement `BitmapSlicer.kt` to crop the region on background thread (`Dispatchers.Default`).
- [ ] Task 4.2: Implement `JapaneseOcrEngine.kt` using ML Kit `TextRecognition.getClient(...)`.
- [ ] Task 4.3: Add text normalization (strip newlines, trim whitespace).

## Milestone 5: Decoupled Dispatch & Handoff
- [ ] Task 5.1: Implement `ClipboardHelper.kt` to push the normalized string to `ClipData` and show a `Toast`.
- [ ] Task 5.2: Implement `BrowserLauncher.kt` to handle the Firefox/Yomitan Intent routing (`https://jisho.org/search/...`).
- [ ] Task 5.3: Create `DispatchCoordinator.kt` to manage the flow based on user preference.
- [ ] Task 5.4: Implement a simple `SharedPreferences` toggle (e.g., in `MainActivity`) to switch between "Clipboard Only" and "Clipboard + Firefox".

## Milestone 6: Hardening & Testing
- [ ] Test on Android 7.0–8.1 (verify legacy notification and overlay handling).
- [ ] Test on Android 14+ (verify `MediaProjection` lifecycle callback and background stability).
- [ ] Validate edge cases (zero-width crop box, user cancelling overlay, screen rotation during capture).
