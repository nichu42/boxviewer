# Project Credits & Third-Party Attributions

BoxViewer is built on open data platforms, open-source software, and creative community artwork. This document provides a complete inventory of credits and attributions for datasets, geocoding backends, libraries, and graphics used both in the application and across project documentation.

---

## 🌍 Data Services

* **openSenseMap API**
  * **Operator:** openSenseLab gGmbH (Münster, Germany)
  * **Description:** Open environmental sensor data platform providing telemetry measurements and station metadata.
  * **License:** GPL v2 / Open Database License (ODbL) — [opensensemap.org](https://opensensemap.org)

---

## 📍 Geocoding Backends

* **Photon Geocoder**
  * **Operator:** komoot GmbH (Berlin, Germany)
  * **Description:** OpenStreetMap-powered search and reverse-geocoding service.
  * **License:** Data © OpenStreetMap contributors ([CC-BY-SA](https://creativecommons.org/licenses/by-sa/2.0/)) — [photon.komoot.io](https://photon.komoot.io)

* **Nominatim Geocoder**
  * **Operator:** OpenStreetMap Foundation
  * **Description:** Open-source search and reverse-geocoding engine.
  * **License:** Open Database License (ODbL) — [nominatim.org](https://nominatim.org)

---

## 📦 Libraries & Frameworks

### Core UI & Framework
* **Jetpack Compose & AndroidX**
  * **Author:** Google LLC
  * **Description:** Declarative UI toolkit, architecture components, and lifecycle systems.
  * **License:** [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
* **AndroidX Navigation Compose, Core KTX, SplashScreen, Lifecycle & Activity Compose, AppCompat**
  * **Author:** Google LLC
  * **License:** [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
* **Compose Material Icons Extended**
  * **Author:** Google LLC
  * **License:** [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

### Networking & Serialization
* **Retrofit**
  * **Author:** Square, Inc.
  * **Description:** Type-safe HTTP client for Android used for openSenseMap REST API queries.
  * **License:** [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
* **OkHttp Engine & Interceptor**
  * **Author:** Square, Inc.
  * **Description:** HTTP client infrastructure and logging interceptor.
  * **License:** [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
* **Moshi Core & Codegen**
  * **Author:** Square, Inc.
  * **Description:** JSON library for Android and Kotlin.
  * **License:** [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

### Database & Concurrency
* **Kotlinx Coroutines & Flow**
  * **Author:** JetBrains s.r.o.
  * **Description:** Asynchronous programming and reactive state flows.
  * **License:** [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
* **Room Persistence Database**
  * **Author:** Google LLC
  * **Description:** SQLite abstraction layer for local caching of environmental sensor boxes.
  * **License:** [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

### Utilities & Testing
* **ZXing Core**
  * **Author:** ZXing Authors
  * **Description:** Barcode and QR code processing library used for senseBox deep link sharing.
  * **License:** [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
* **Robolectric**
  * **Author:** Robolectric Authors
  * **Description:** Android unit testing framework running framework code on the JVM.
  * **License:** [MIT License](https://opensource.org/licenses/MIT)
* **Roborazzi**
  * **Author:** Takahiro Menju and Contributors
  * **Description:** Screenshot testing toolkit for Compose visual regression tests.
  * **License:** [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

---

## 🎨 Graphics & Artwork

* **"Get it on GitHub" Badge (`metadata/badges/badge_github.png`)**
  * **Original Creator:** `@flocke` for [F-Droid Artwork](https://gitlab.com/fdroid/artwork/-/tree/master/badge/src) / [Kunzisoft/Github-badge](https://github.com/Kunzisoft/Github-badge)
  * **License:** [Creative Commons Attribution-ShareAlike 3.0 Unported (CC-BY-SA 3.0)](https://creativecommons.org/licenses/by-sa/3.0/)

* **"Get it on Obtainium" Badge (`metadata/badges/badge_obtainium.png`)**
  * **Original Creator:** ImranR98 ([Obtainium](https://github.com/ImranR98/Obtainium))
  * **License:** MIT License / Open Source

* **"Get it on Google Play" Badge (`metadata/badges/badge_google_play.png`)**
  * **Original Creator:** Google LLC
  * **License:** Google Brand Guidelines / Google Play Badge Usage
