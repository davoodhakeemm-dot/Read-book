# Reading Corner (Android)

Native Android rewrite of Reading Corner built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Navigation Compose**.

## Features

- **Splash & Welcome**: Warm editorial landing screen with onboarding and quick-explore entry points.
- **Language & Category Onboarding**: Multi-select personalization across 8 languages and 16 curated reading categories.
- **Catalog & Discovery**:
  - **Home** dashboard with Continue Reading, Popular, Recommended, New Arrivals, Top Rated, Categories, and Languages.
  - **Explore** tabs (Popular, Trending, New, Recommended, Top Rated).
  - **Search** with multi-filter drawer (Language, Category, Minimum Rating) and sorting (Popular, Newest, A–Z, Highest Rated).
  - **Category & Language Detail** shelves with featured highlights and cross-filtering.
- **Book Details & Distraction-Free Reader**:
  - Book metadata, progress tracking, favorites, bookmarks, and Android Share sheet integration.
  - Customizable reader with **Light / Sepia / Night** themes, Serif/Sans typography, adjustable font size (`14sp–28sp`), line spacing, page width, page bookmarking, and Android `TextToSpeech` read-aloud (`0.8x` / `1x` / `1.3x`).
- **Library, History, Profile & Settings**:
  - 4-tab My Library (Currently Reading, Favorites, Bookmarks, Completed), reading session history, profile stats, and DataStore-backed preferences.
- **Private Messaging Area (Device-Local)**:
  - Accessed by holding the Reading Corner logo for 7 seconds or typing `hi` in Search; triple-tap anywhere outside interactive controls to return to Home.
  - Local account creation/login with salted password digests, profile photo picker (`PickVisualMedia`), directory & private threads, emoji picker, photo/video attachments, and voice note recording/playback.
