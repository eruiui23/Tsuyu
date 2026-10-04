# Architecture & Technical Design: Tsuyu

## 1. System Topology & Component Boundaries
Tsuyu follows an event-driven, decoupled pipeline where capture, UI interaction, text extraction, and text dispatching operate as isolated responsibilities.

```text
[Target App (Manga/Game)]
         ▲
         │ (Visual context)
[ScreenCaptureService] ──► [ScreenCaptureEngine] ──► Single Bitmap
         │                                                │
         ▼                                                ▼
[OverlayManager / CropView] ── (Rect Coordinates) ──► [BitmapSlicer]
                                                          │
                                                          ▼
                                                  [JapaneseOcrEngine]
                                                          │
                                                (Clean String Result)
                                                          │
                                                          ▼
                                              [DispatchCoordinator]
                                             /                     \
                             (Default Flow) /                       \ (Optional / Secondary)
                                           ▼                         ▼
                                  [ClipboardHelper]          [BrowserLauncher]
                               (System ClipData + Toast)   (Firefox / Yomitan Intent)
```

---

## 2. Core Components Specification

### 2.1. `ScreenCaptureService` (Process Anchor)
* **Lifecycle:** Runs as an ongoing Foreground Service with `foregroundServiceType="mediaProjection"`.
* **State Management:** Holds the active `MediaProjection` token passed from `MainActivity`. Handles the Android 14+ `MediaProjection.Callback` to react cleanly to external session terminations.
* **Notification Interface:** Shows a persistent low-priority notification with two primary actions:
  * `ACTION_TRIGGER_CAPTURE`: Initiates the screen capture and overlay sequence.
  * `ACTION_STOP_SERVICE`: Cleans up active projections, tears down notification, and calls `stopSelf()`.

### 2.2. `ScreenCaptureEngine` (Single-Shot Frame Acquisition)
* **Lifecycle:** Instantiated strictly on-demand; torn down immediately after capturing one valid frame.
* **Display Management:**
  1. Computes active display dimensions (`width`, `height`, `densityDpi`) using `WindowManager`.
  2. Allocates an `ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)`.
  3. Registers a `VirtualDisplay` via `mediaProjection.createVirtualDisplay(...)`.
  4. Collects the first ready plane buffer via `imageReader.acquireLatestImage()`.
  5. Implements row-stride normalization (see Section 3).
  6. Explicitly calls `virtualDisplay.release()` and `imageReader.close()`.

### 2.3. `OverlayManager` & `CropOverlayView` (UI Selection)
* **Window Configuration:** Attached via `WindowManager.addView()` using `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
* **Flags:** Configured with `FLAG_LAYOUT_IN_SCREEN` and `FLAG_FULLSCREEN`. Starts without `FLAG_NOT_TOUCHABLE` to intercept drag gestures.
* **Touch Handling:**
  * `ACTION_DOWN`: Captures origin point `(x0, y0)`.
  * `ACTION_MOVE`: Invalidates canvas to render dynamic semi-transparent bounding box with boundary stroke.
  * `ACTION_UP`: Validates selection minimum dimensions (> 10px). Emits normalized Android `Rect(left, top, right, bottom)` back to coordinator, then immediately calls `WindowManager.removeView()`.

### 2.4. `JapaneseOcrEngine` (Inference)
* **Execution:** Encapsulated in a suspending function running on `Dispatchers.Default`.
* **Input:** Cropped `Bitmap` supplied by `BitmapSlicer`.
* **Engine:** Wraps Google ML Kit `JapaneseTextRecognizer` via `TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())`.
* **Post-Processing (Normalization):**
  * Strips redundant newline characters (`\n`) introduced by vertical text line breaks.
  * Trims leading/trailing whitespace.
  * Preserves full-width Japanese punctuation (`「」`, `…`, `。`, `、`).

### 2.5. `DispatchCoordinator` (Decoupled Handoff Pipeline)
Coordinates downstream delivery based on user configuration:
* **Primary Route (Clipboard - Default):**
  * Invokes `ClipboardHelper` to push the normalized string into `ClipData.newPlainText("Japanese Text", text)`.
  * Displays user feedback (e.g., standard `Toast` or transient HUD: *"Copied: {text}"*) on `Dispatchers.Main`.
  * Keeps the user directly inside their reading application without switching contexts.
* **Secondary Route (Browser Lookup - Configurable / Secondary Trigger):**
  * Encapsulated in `BrowserLauncher`.
  * Encodes query string via `URLEncoder.encode(text, "UTF-8")`.
  * Dispatches `Intent.ACTION_VIEW` targeting `org.mozilla.firefox` pointing to `https://jisho.org/search/<encoded_text>`.
  * Fallback: Catches `ActivityNotFoundException` and dispatches generic browser intent.

---

## 3. Pixel Buffer Stride & Padding Normalization
Direct plane buffers extracted from `ImageReader` frequently contain padding bytes at the tail of each scanline:

`rowPadding = rowStride - (pixelStride * width)`

```kotlin
val plane = image.planes[0]
val buffer = plane.buffer
val pixelStride = plane.pixelStride
val rowStride = plane.rowStride
val rowPadding = rowStride - pixelStride * width

val bitmap = Bitmap.createBitmap(
    width + rowPadding / pixelStride,
    height,
    Bitmap.Config.ARGB_8888
)
bitmap.copyPixelsFromBuffer(buffer)

// Extract the unpadded screen frame
val cleanBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
if (cleanBitmap != bitmap) {
    bitmap.recycle()
}
```
*Failure to crop out `rowPadding` leads to skewed row alignment, corrupting character glyphs before OCR processing.*

---

## 4. Configuration Storage
* User preferences (e.g., toggle for automatic Firefox handoff vs. clipboard-only mode) will be persisted via `SharedPreferences` (or Jetpack `DataStore`), defaulting to:
  * `AUTO_OPEN_FIREFOX = false` (Primary clipboard flow enabled by default).
