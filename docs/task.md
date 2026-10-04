# Implementation Tasks & Checklist

## Milestone 1: Foundation & Setup
- [x] Create project with Minimum SDK 24, Kotlin DSL.
- [x] Configure dependencies (`mlkit-text-recognition-japanese`, `kotlinx-coroutines-android`).
- [x] Declare permissions & metadata in `AndroidManifest.xml`.
- [x] Implement permission chain in `MainActivity.kt`.
- [x] Implement base `ScreenCaptureService.kt` with persistent notification controls.

## Milestone 2: Frame Capturing (Single-Shot)
- [ ] Task 2.1: Create `ScreenCaptureEngine.kt` to encapsulate `VirtualDisplay` and `ImageReader`.
- [ ] Task 2.2: Implement plane buffer to `Bitmap` conversion with row stride/padding handling.
- [ ] Task 2.3: Wire capture trigger from `ScreenCaptureService` to return a raw full-screen `Bitmap`.

## Milestone 3: Transparent Crop Overlay
- [ ] Task 3.1: Create custom view `CropOverlayView.kt` with touch handling for drawing a rectangle.
- [ ] Task 3.2: Create `OverlayManager.kt` using `WindowManager` to attach/detach the view with `FLAG_NOT_FOCUSABLE`.
- [ ] Task 3.3: Implement callback to pass crop coordinates back to the service and remove the overlay.

## Milestone 4: OCR & Handoff Pipeline
- [ ] Task 4.1: Implement `BitmapSlicer.kt` to crop the region on background thread (`Dispatchers.Default`).
- [ ] Task 4.2: Implement `JapaneseOcrEngine.kt` using ML Kit `TextRecognition.getClient(...)`.
- [ ] Task 4.3: Implement `ClipboardHelper.kt` to push recognized string to `ClipData`.
- [ ] Task 4.4: Implement `BrowserLauncher.kt` to fire the Firefox Intent with the Jisho URL.

## Milestone 5: Hardening & Testing
- [ ] Test on Android 7.0–8.1 (verify no crashes on legacy notification/overlay handling).
- [ ] Test on Android 14+ (verify `MediaProjection` lifecycle callback and service stability).
- [ ] Validate edge cases (zero-width crop box, cancelled overlay, screen rotation).
