# Architecture & Technical Design: Tsuyu

## 1. System Topology & Component Boundaries
Tsuyu follows an event-driven, decoupled pipeline. The architecture unifies two different input methods into a single processing workflow.

```text
[Input Method A: API 30+]              [Input Method B: API 24+]
[FloatingBadgeManager]                 [MainActivity / Picker UI]
         │                                         │
[ScreenshotAccessibilityService]        (System Photo Picker)
         │                                         │
         └───► (Raw Screen Bitmap / URI) ◄─────────┘
                       │
                       ▼
               [CropActivity] ── (Draw selection) ──► [BitmapSlicer]
                                                            │
                                                            ▼
                                                    [OnnxOcrEngine] (manga-ocr)
                                                            │
                                                  (Clean String Result)
                                                            │
                                                            ▼
                                                [DispatchCoordinator]
                                               /                     \
                               (Default Flow) /                       \ (Optional / Secondary)
                                             ▼                         ▼
                                    [ClipboardHelper]          [BrowserLauncher]
```

---

## 2. Input Sourcing Specification

### 2.1. Method A: `ScreenshotAccessibilityService` (Modern Android)
*   **Requirement:** API 30+ (Android 11+).
*   **Lifecycle:** Runs persistently as an `AccessibilityService`. 
*   **Execution:** 
    1. `FloatingBadgeManager` intercepts a touch gesture.
    2. Sends a broadcast or callback to the accessibility service.
    3. The service executes `takeScreenshot()`.
    4. Upon `onSuccess(ScreenshotResult)`, the hardware buffer is converted into a standard `Bitmap`.
    5. The `Bitmap` is cached (or written to a temporary cache file to avoid Intent size limits) and `CropActivity` is launched.

### 2.2. Method B: Gallery Fallback (Legacy Android)
*   **Requirement:** API 24+.
*   **Execution:** 
    1. User taps a static shortcut or an in-app button.
    2. Launches `ActivityResultContracts.PickVisualMedia` (or legacy `ACTION_GET_CONTENT`).
    3. The OS returns an image `Uri`.
    4. The app passes the `Uri` directly to `CropActivity`.

---

## 3. Image Cropping (`CropActivity`)
*   **State:** Replaces the custom floating overlay from previous iterations. Because the image is pre-captured, a standard full-screen Android `Activity` is used to display the `Bitmap` or `Uri`.
*   **Interaction:** 
    *   Uses a reliable Open-Source cropping library (e.g., `CanHub/Android-Image-Cropper`) or a custom Canvas view.
    *   Extracts normalized `Rect` bounds and isolates the region of interest using `Bitmap.createBitmap()`.

---

## 4. Inference: `OnnxOcrEngine`
*   **Dependency:** `com.microsoft.onnxruntime:onnxruntime-android`.
*   **Model Source:** A PyTorch `manga-ocr` model exported to the ONNX format (e.g., `kha-white/manga-ocr-base` exported to ONNX). 
*   **Asset Bundling:** The model (`.onnx`) and the tokenizer vocabulary (`.json` or `.txt`) are stored in `src/main/assets/`.
*   **Execution:**
    1. The cropped `Bitmap` is resized and converted into a multi-dimensional float tensor (typically 224x224 RGB).
    2. The tensor is fed to the ONNX `OrtSession` running on `Dispatchers.Default`.
    3. The model outputs token IDs which are mapped back to Japanese characters via the tokenizer.
    4. Cleans output by removing structural tokens (like `[SEP]`, `[PAD]`) and stripping erroneous line breaks.

---

## 5. `DispatchCoordinator` (Decoupled Handoff Pipeline)
Coordinates downstream delivery based on user configuration (`SharedPreferences` or Jetpack `DataStore`):
*   **Primary Route (Clipboard):** Invokes `ClipboardManager` to push the normalized string into system `ClipData`.
*   **Secondary Route (Browser):** Encodes query string via `URLEncoder` and dispatches `Intent.ACTION_VIEW` targeting `org.mozilla.firefox` mapped to `https://jisho.org/search/<encoded_text>`.
