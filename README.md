# Smart Gallery
**Vue 'O Eden**
  
This is a media gallery application for Android designed to efficiently manage your photos and videos using Computer Vision model **MobileNetV3** for object detection with cloud-based LLM **Google Gemini**.

---

## Key Features

* **Intuitive Gallery:** Browse your media seamlessly with standard album views and a clean interface built with Jetpack Compose.
* **AI-Powered Search:** Find photos and videos using natural language queries through an integrated chatbot assistant powered by Google Gemini.
* **Automatic Object Detection:** Utilizes an on-device TensorFlow Lite model (MobileNetV3) to detect objects in your images and automatically organizes them into smart "AI Albums".
* **Location Awareness:** Fetches and displays location information for your media using EXIF data and reverse geocoding via OpenStreetMap Nominatim.
* **Comprehensive Media Management:**
    * **Trash:** Temporarily store deleted media before permanent removal.
    * **Favorites:** Mark your preferred photos and videos for quick access.
    * **Collections:** Create custom virtual albums to organize media without moving files.
    * **Duplicate Finder:** Identify and manage duplicate media based on file hash.
    * **Large Media:** Easily find media files that consume significant storage space.
    * **Search History:** Keep track of your past search queries.
* **Media Details:** View detailed information about your photos and videos, including filename, size, date, resolution, and location.
* **Integrated Video Player:** Play videos directly within the app using ExoPlayer.
* **Theme Support:** Switch between light and dark modes.
* **Background Processing:** Uses WorkManager for efficient background tasks like media scanning, object detection, and location fetching.

---

## Technologies Used

* **Core Stack:**
    * [Kotlin](https://kotlinlang.org/): Primary programming language.
    * [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html): For all asynchronous operations.
    * [MVVM Architecture](https://developer.android.com/topic/architecture): Model-View-ViewModel pattern for a clean separation of concerns.
* **Jetpack:**
    * [Jetpack Compose](https://developer.android.com/jetpack/compose): The entire UI is built with this modern declarative UI toolkit.
    * [Navigation Compose](https://developer.android.com/jetpack/compose/navigation): For all in-app navigation.
    * [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel): To store and manage UI-related data.
    * [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager): For reliable, deferrable background tasks (AI scanning, media indexing).
    * [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview): To load large media lists efficiently in grids.
* **Data & Storage:**
    * [Room Persistence Library](https://developer.android.com/jetpack/androidx/releases/room): Local database for caching media info, AI tags, collections, and search history.
    * [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore): For storing simple key-value preferences (like the app theme).
* **AI & Machine Learning:**
    * [TensorFlow Lite (On-Device)](https://www.tensorflow.org/lite): Runs a MobileNetV3 model directly on the device for fast and private image labeling.
    * [Google Gemini API](https://ai.google.dev/): (Cloud-based) Used for the natural language search and conversational AI assistant.
* **Media & UI:**
    * [Coil](https://coil-kt.github.io/coil/): An image loading library for Kotlin Coroutines.
    * [Media3 ExoPlayer](https://developer.android.com/guide/topics/media/media3/exoplayer): For robust and high-performance video playback.
* **Networking:**
    * [HttpURLConnection](https://developer.android.com/reference/java/net/HttpURLConnection): (Core Android) Used for making direct REST API calls to Gemini and Nominatim.
    * [OpenStreetMap Nominatim](https://nominatim.openstreetmap.org/): For reverse geocoding coordinates into addresses.

---

## Prerequisites

Before you begin, ensure you have the following installed:

* **Android Studio**: (Latest stable version recommended)
* **Android SDK**: Target SDK 35, Minimum SDK 29
* **Android Device/Emulator**: Running API level 29 or higher.
* **Google Gemini API Key**: Required for the natural language search feature.

---

## Installation and Setup

1.  **Clone the repository**:
    ```bash
    git clone [https://github.com/sslythrrr/smart-gallery.git](https://github.com/sslythrrr/smart-gallery.git)
    cd smart-gallery
    ```

2.  **Open in Android Studio**:
    * Open Android Studio.
    * Select "Open" or "Open an Existing Project".
    * Navigate to the cloned `smart-gallery` directory and select it.

3.  **Gradle Sync**:
    * Wait for Android Studio to sync the project with Gradle files. This will download all necessary dependencies.

4.  **Add Gemini API Key**:
    * Create or open the `gradle.properties` file in the root project directory (the same level as `settings.gradle.kts`).
    * Add the following line, replacing `"YOUR_API_KEY"` with your actual Gemini API key:
        ```properties
        GEMINI_API_KEY="YOUR_API_KEY"
        ```
    * *Note: Ensure this file is included in your `.gitignore` if you plan to share your code publicly.*

5.  **Build and Run**:
    * Select your target device (emulator or physical device).
    * Click the "Run 'app'" button (the green play icon) in Android Studio.

6.  **Permissions**:
    * The app will request necessary permissions (Storage/Media access, potentially Location) upon first launch. Grant these permissions for the app to function correctly.

7.  **Initial Scan**:
    * On the first run, the app will perform an initial scan of your device's media. This may take some time depending on the number of photos and videos. Background processing for object detection and location fetching will start afterwards.

---

## Example Usage

* **Browse:** Navigate through the "Gallery" tab to view your standard device albums and automatically generated "AI Albums".
* **Search:** Go to the "Search" tab and type natural language queries like "show me photos of cats in Bandung from last year" or "pictures from my beach vacation". The AI assistant will interpret your request and display relevant media or ask for clarification.
* **Manage:** Use the "Manage" tab to access tools like Trash, Favorites, Collections, Duplicate Finder, and Large Media view.
* **View Details:** Tap on any media item in the gallery or search results to open the detail view. Swipe left/right to navigate between items. Use the bottom bar icons to share, delete, favorite, or view information.

---

## Contributing

Contributions are welcome! If you'd like to contribute, please follow these steps:

1.  **Fork** the repository on GitHub.
2.  **Clone** your forked repository to your local machine.
3.  Create a new **branch** for your feature or bug fix (`git checkout -b feature/your-feature-name`).
4.  Make your changes and **commit** them (`git commit -m 'Add some feature'`).
5.  **Push** your changes to your fork on GitHub (`git push origin feature/your-feature-name`).
6.  Open a **Pull Request** from your fork to the original repository's `main` branch.

Please ensure your code follows the project's coding style and includes relevant documentation or tests if applicable.

---

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.
