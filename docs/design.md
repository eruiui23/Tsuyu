# Architecture & Technical Design: MangaOCR

## 1. System Topology & Component Boundaries
The application follows an asynchronous, event-driven pattern decoupled into four primary components:

1. **`MainActivity` (Presentation / Permission Gateway):**
    - Coordinates the runtime permission chain (`POST_NOTIFICATIONS`, `SYSTEM_ALERT_WINDOW`).
    - Obtains the `MediaProjection` token via `ActivityResultLauncher` and forwards it to the service.
    - Terminates immediately after token handoff to release foreground resources.

2. **`ScreenCaptureService` (Process Anchor & Orchestrator):**
    - Holds the `MediaProjection` instance in a low-priority Foreground Service (`mediaProjection` type).
    - Coordinates single-frame capture via `ImageReader` on-demand when the user triggers `ACTION_TRIGGER_CAPTURE`.
    - Spawns and manages the `OverlayManager`.

3. **`OverlayManager` & `CropOverlayView` (UI / Coordinate Translation):**
    - Injected into the Android `WindowManager` with `TYPE_APPLICATION_OVERLAY`.
    - Intercepts touch gestures (`MotionEvent.ACTION_DOWN`, `ACTION_MOVE`, `ACTION_UP`) to draw a dynamic selection rectangle.
    - Emits absolute screen coordinates `(left, top, right, bottom)` and dismisses the overlay.

4. **`OcrProcessor` (Domain Execution):**
    - Performs bitmap cropping using Kotlin Coroutines on `Dispatchers.Default`.
    - Wraps the Google ML Kit `JapaneseTextRecognizer` (or future ONNX models) in a suspend function.
    - Pushes raw text to `ClipboardManager` and issues the Firefox query Intent.

---

## 2. Memory & Frame Lifecycle Management
* **Single-Frame Pipeline:** A persistent `VirtualDisplay` consumes battery and CPU. When "Capture" is triggered:
    1. Measure display metrics (width, height, density DPI).
    2. Create a temporary `ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)`.
    3. Spin up `mediaProjection.createVirtualDisplay(...)`.
    4. Acquire the first valid image frame via `acquireLatestImage()`.
    5. Convert the plane buffer to an in-memory `Bitmap` (handling row padding / stride).
    6. Immediately call `virtualDisplay.release()` and `imageReader.close()`.

---

## 3. Row Stride & Padding Pitfall
`ImageReader` raw byte buffers frequently include padding bytes at the end of each pixel row (`rowStride != width * pixelStride`).
The bitmap conversion MUST calculate: