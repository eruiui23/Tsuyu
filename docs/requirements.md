# Requirements Specification: Tsuyu (Japanese Screen-Capture OCR)

## 1. Product Overview
Tsuyu is a lightweight background Android utility designed to assist users reading Japanese media (manga, visual novels, light novels, and games). The app enables users to freeze the screen, crop a target Japanese text region, extract the characters using on-device Optical Character Recognition (OCR), and immediately copy the clean string to the system clipboard for arbitrary note-taking or vocabulary collection. 

Optionally, users can enable a quick-lookup bridge to automatically hand off the recognized text to Mozilla Firefox to trigger the Yomitan dictionary extension via search queries.

## 2. Technical Stack
*   **Language:** Kotlin
*   **Minimum SDK:** API 24 (Android 7.0 Nougat)
*   **Target SDK:** API 34/35 (Android 14/15)
*   **Core OCR Engine:** Google ML Kit Text Recognition v2 (Japanese unbundled model)
*   **Concurrency:** Kotlin Coroutines (`Dispatchers.Default` for pixel math, `Dispatchers.Main` for UI/Clipboard feedback)
*   **Build System:** Gradle (Kotlin DSL)

## 3. Functional Requirements

### 3.1. System & Background Management
*   **Foreground Service:** Maintains an active background session using `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` to keep the process alive while other target apps are displayed.
*   **Persistent Notification:** Displays ongoing status and provides direct action buttons:
    *   `Capture`: Triggers the screen grab and overlay selection.
    *   `Stop`: Cleanly shuts down the service and tears down active projections.
*   **Lifecycle Stability:** Recovers gracefully from low-memory conditions (`START_STICKY`) and handles Android 14+ projection session terminations properly via callbacks.

### 3.2. Screen Capture (MediaProjection)
*   **Permission Handshake:** Prompts user consent via `MediaProjectionManager.createScreenCaptureIntent()` at initial setup.
*   **Single-Frame Capture:** Uses a short-lived `VirtualDisplay` and `ImageReader` to acquire exactly one frame buffer on demand, immediately tearing down the display to preserve battery and compute resources.

### 3.3. Overlay & Cropping Interface
*   **System Alert Overlay:** Displays a transparent full-screen touch interceptor over third-party applications via `WindowManager` (`TYPE_APPLICATION_OVERLAY`).
*   **Interactive Cropping Canvas:** Captures user drag gestures (`ACTION_DOWN`, `ACTION_MOVE`, `ACTION_UP`) to draw a dynamic crop rectangle with real-time visual feedback.
*   **Coordinate Extraction:** Normalizes selection coordinates to device screen dimensions and extracts the designated sub-bitmap.

### 3.4. Optical Character Recognition (OCR)
*   **On-Device Processing:** Feeds cropped bitmap buffers into ML Kit Japanese Text Recognizer without remote network requests.
*   **Text Normalization:** Strips accidental whitespace, line breaks, or reading artifact noise typical in vertical manga text runs.

### 3.5. Text Handling & Dispatch Pipeline (Updated)
*   **Primary Action (Default Flow):**
    *   Automatically copies extracted Japanese text to the Android `ClipboardManager`.
    *   Displays non-blocking visual feedback (e.g., Toast or quick overlay badge) confirming copy success so the user can paste into personal notes, Anki, spreadsheets, or flashcard lists.
*   **Secondary Action (Optional / Configurable Bridge):**
    *   Provides a configurable setting or interactive option ("Open in Yomitan / Firefox").
    *   When active, formats the extracted text into a search URL (`https://jisho.org/search/<encoded_text>`) and dispatches an `ACTION_VIEW` Intent explicitly targeted at `org.mozilla.firefox`.
    *   Falls back to the system default browser if Firefox is not installed.

## 4. Non-Functional Requirements
*   **Backward Compatibility:** Full operational support across API 24 through API 34+ via runtime permission branching (`POST_NOTIFICATIONS`, `SYSTEM_ALERT_WINDOW`).
*   **Latency Target:** Cropping-to-clipboard latency must remain under 800ms on modern ARM64 devices.
*   **Memory Hygiene:** Bitmaps and `ImageReader` instances must be recycled immediately after frame extraction to prevent OutOfMemory (OOM) errors.

## 5. Future Scope
*   Swap ML Kit engine with quantized `kha-white/manga-ocr` running on ONNX Runtime Android for improved vertical text and furigana filtering.
*   Local offline HTML asset bridge for zero-network Yomitan lookups in Firefox.
