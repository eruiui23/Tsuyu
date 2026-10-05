# Requirements Specification: Tsuyu (Japanese Screen-Capture OCR)

## 1. Product Overview
Tsuyu is a lightweight background Android utility designed to assist users reading Japanese media (manga, visual novels, light novels, and games). The app provides a fast, offline pipeline to extract Japanese text from screenshots, crop the desired region, and immediately dispatch the text to the clipboard or a browser-based dictionary (Yomitan via Firefox). 

To support a wide range of devices without heavy background battery drain, Tsuyu relies on a dual-input architecture: a native Accessibility Service screenshot trigger for modern Android, and a lightweight gallery picker fallback for legacy devices.

## 2. Technical Stack
*   **Language:** Kotlin
*   **Minimum SDK:** API 24 (Android 7.0 Nougat)
*   **Target SDK:** API 34/35 (Android 14/15)
*   **Core OCR Engine:** ONNX Runtime Android executing a quantized ONNX export of `kha-white/manga-ocr-base` (e.g., `onnx-community/manga-ocr-base-ONNX`).
*   **Concurrency:** Kotlin Coroutines (`Dispatchers.Default` for inference, `Dispatchers.Main` for UI)
*   **Build System:** Gradle (Kotlin DSL)

## 3. Functional Requirements

### 3.1. Dual-Input Capture System
The app will dynamically choose its input method based on the Android OS version to bypass strict background execution limits:
*   **Method A: Accessibility Floating Badge (Android 11+ / API 30+):**
    *   Utilizes the native `AccessibilityService.takeScreenshot()` API, which was introduced in API level 30.
    *   Displays a persistent floating badge (`SYSTEM_ALERT_WINDOW`).
    *   Tapping the badge instantly grabs the screen buffer without video streaming overhead or Android 14+ projection warnings.
*   **Method B: Gallery Picker (Universal / API 24+):**
    *   Provides a fallback for legacy devices (e.g., Android 7.0).
    *   Uses Android's native `ActivityResultContracts.PickVisualMedia` or generic `ACTION_GET_CONTENT` Intent.
    *   Allows the user to take a standard hardware screenshot and immediately pick it from their gallery to process.

### 3.2. Cropping Interface
*   Once a raw `Bitmap` is acquired (via either input method), it is passed to a full-screen `Activity`.
*   The Activity presents the image and allows the user to draw a bounding box over the target Japanese text.
*   Validates selection minimum dimensions and extracts the sub-bitmap for processing.

### 3.3. Offline Optical Character Recognition (ONNX)
*   **Inference:** Feeds the cropped bitmap into the ONNX Runtime executing the manga-OCR model directly on the device CPU.
*   **Robustness:** Capable of reading vertical text, horizontal text, and text overlaid on complex image backgrounds without an internet connection.
*   **Text Normalization:** Cleans up newline artifacts and preserves full-width Japanese punctuation.

### 3.4. Text Handling & Dispatch Pipeline
*   **Primary Action (Default):**
    *   Copies extracted text to the Android `ClipboardManager` automatically.
    *   Displays a transient `Toast` confirming the copied text.
*   **Secondary Action (Optional / Firefox Handoff):**
    *   If enabled in settings, formats the extracted text into a search URL (`https://jisho.org/search/<encoded_text>`).
    *   Dispatches an `Intent.ACTION_VIEW` explicitly targeted at `org.mozilla.firefox`.

## 4. Non-Functional Requirements
*   **Offline Privacy:** Text recognition must happen 100% locally on-device.
*   **Binary Size:** The ONNX model must be quantized to minimize APK bloat while retaining high accuracy.
*   **Latency:** The crop-to-clipboard pipeline must process in under 1 second on modern hardware.
