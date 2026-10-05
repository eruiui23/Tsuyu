# Implementation Tasks & Checklist: Tsuyu

## Milestone 1: Project Setup & Input Sourcing
- [ ] Task 1.1: Configure project dependencies in `build.gradle.kts` (`onnxruntime-android`, `kotlinx-coroutines-android`, `androidx.activity-ktx`).
- [ ] Task 1.2: Implement Method B (Universal / API 24+) Gallery Picker in `MainActivity.kt` using `ActivityResultContracts.PickVisualMedia` to obtain an image `Uri`.
- [ ] Task 1.3: Declare and implement Method A (API 30+) `ScreenshotAccessibilityService.kt` with `android:accessibilityFeedbackType="feedbackGeneric"` and `android:canTakeScreenshot="true"` in accessibility service XML.
- [ ] Task 1.4: Implement `FloatingBadgeManager.kt` using `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` (active on API 30+ when accessibility service is enabled).
- [ ] Task 1.5: Wire badge click to invoke `AccessibilityService.takeScreenshot()`, save the result to a temporary cache file, and prepare image handoff.

## Milestone 2: Cropping Interface (`CropActivity`)
- [ ] Task 2.1: Create `CropActivity.kt` and declare it in `AndroidManifest.xml`.
- [ ] Task 2.2: Implement image loading from `Uri` or temporary cache file into the cropping canvas.
- [ ] Task 2.3: Build or integrate cropping view allowing touch drag bounding box selection.
- [ ] Task 2.4: Implement sub-bitmap slicing with boundary checks and return the cropped `Bitmap`.

## Milestone 3: ONNX Inference Engine (`manga-ocr`)
- [ ] Task 3.1: Export/quantize `manga-ocr` into `.onnx` format and bundle model + tokenizer files into `src/main/assets/`.
- [ ] Task 3.2: Implement tokenizer vocabulary parser in Kotlin to map token IDs back to Japanese characters.
- [ ] Task 3.3: Implement image tensor preprocessing in `OnnxOcrEngine.kt` (resizing, RGB normalization, CHW float tensor conversion).
- [ ] Task 3.4: Implement `OrtSession` inference call on `Dispatchers.Default` and run autoregressive / greedy token decoding.
- [ ] Task 3.5: Add text post-processing to strip special tokens (`[PAD]`, `[SEP]`, etc.) and trim extra whitespace.

## Milestone 4: Dispatch Pipeline & Preferences
- [ ] Task 4.1: Implement `ClipboardHelper.kt` to push text to `ClipboardManager` and display a transient confirmation `Toast`.
- [ ] Task 4.2: Implement `BrowserLauncher.kt` to launch Firefox with `https://jisho.org/search/<encoded_text>` (with fallback to default browser).
- [ ] Task 4.3: Implement `DispatchCoordinator.kt` reading user preference (`pref_use_firefox`) from `SharedPreferences`.
- [ ] Task 4.4: Add preference switch in `MainActivity.kt` and `activity_main.xml` to toggle the Firefox handoff.

## Milestone 5: Testing & Hardening
- [ ] Task 5.1: Verify Gallery picker workflow on legacy Android devices (API 24–29, including Sony SOV35).
- [ ] Task 5.2: Verify Accessibility screenshot trigger and floating badge on modern Android devices (API 30+).
- [ ] Task 5.3: Benchmark on-device ONNX inference latency and verify bitmap recycling to avoid memory leaks.
