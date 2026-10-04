# Requirements Specification: Android Screen-Capture Japanese OCR

## 1. Product Overview
A lightweight, background-running Android utility application designed to assist users in reading Japanese media (manga, visual novels, games). The app allows users to capture a specific portion of their screen, extract the Japanese text using Optical Character Recognition (OCR), and seamlessly hand off that text to Mozilla Firefox to trigger the Yomitan dictionary extension.

## 2. Technical Stack
*   **Language:** Kotlin
*   **Minimum SDK:** API 24 (Android 7.0 Nougat)
*   **Target SDK:** API 34/35 (Android 14+)
*   **Core OCR Engine:** Google ML Kit Text Recognition v2 (Japanese unbundled model)
*   **Concurrency:** Kotlin Coroutines
*   **Build System:** Gradle (Kotlin DSL)

## 3. Functional Requirements

### 3.1. System & Background Management
*   **Foreground Service:** The app must maintain an active background state using a Foreground Service to prevent the Android OS from killing the process while the user is inside other applications.
*   **Persistent Notification:** The service must display an ongoing notification containing a "Capture" action button to trigger the OCR workflow without requiring the user to open the host app.
*   **Lifecycle Management:** The service must gracefully handle system memory pressure and restart automatically if killed (`START_STICKY`).

### 3.2. Screen Capture (MediaProjection API)
*   **Permission Handshake:** The app must request user consent to record the screen via `MediaProjectionManager.createScreenCaptureIntent()` upon initial launch.
*   **Virtual Display:** The app must create a `VirtualDisplay` via the `MediaProjection` token to stream the device screen to an `ImageReader` surface.
*   **Single-Frame Capture:** The app must only capture a single bitmap frame when the user taps "Capture" to preserve battery and memory (no continuous video recording).

### 3.3. Overlay UI (WindowManager)
*   **System Alert Window:** The app must request `SYSTEM_ALERT_WINDOW` permission to draw over other applications.
*   **Transparent Canvas:** Upon triggering a capture, a transparent overlay must appear over the frozen screen frame.
*   **Interactive Cropping:** The user must be able to drag their finger to define a rectangular bounding box around the target text.
*   **Bitmap Slicing:** The system must calculate the coordinates of the user's bounding box and crop the captured screen bitmap accordingly.

### 3.4. Optical Character Recognition (OCR)
*   **Image Processing:** The cropped bitmap must be converted to an `InputImage` object and fed into the ML Kit Japanese Text Recognizer.
*   **String Extraction:** The engine must return a combined string of the recognized Japanese characters (Kanji, Hiragana, Katakana).
*   **Local HTML Dictionary Bridge:** Implement a local bundled HTML page for the Firefox Intent to allow Yomitan lookups entirely offline, completely removing the reliance on Jisho.org.