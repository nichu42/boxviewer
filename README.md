# BoxViewer

<p align="center">
  <img src="app/src/main/res/drawable/boxviewer_white_bg.png" alt="BoxViewer Logo" width="180" />
</p>

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-orange.svg)](https://kotlinlang.org)

**BoxViewer** is a modern, fully-featured, open-source Android client for [openSenseMap](https://opensensemap.org), a global open-data platform for environmental sensor networks and DIY weather stations (known as *senseBoxes*). 

Crafted entirely with **Kotlin** and **Jetpack Compose** following Material Design 3 styling, BoxViewer enables environmentalists, hobbyists, and researchers to locate nearby sensor stations, monitor micro-climate telemetry streams in real time, and configure sleek home-screen widgets for instant, glanceable observation.

---

## 🎨 Visual Preview & Design

BoxViewer's user experience incorporates Material Design 3 guidelines:
- **Tailored Modern Dark Surface**: Utilizes a highly polished dynamic contrast palette designed for premium aesthetics and prolonged, eye-safe viewing sessions.
- **Micro-interactions & Edge-to-Edge Core**: Full keyboard and touch event compliance (e.g., keyboard preview intercepts prevent unwanted state changes), combined with fluid page-entering transitions.
- **Clean Sizing**: Adaptive spacing adhering to the standard 8dp layout grid with clear status tracking.

---

## 🚀 Key Features

### 🌟 Interactive Dashboard (My senseBoxes)
- **Local Collections**: Favorite and save specific environmental stations to local database collections.
- **Telemetry Indicators**: High-density glanceable view of the latest active measurements (Temperature, Humidity, PM2.5, PM10, UV, Barometric Pressure, etc.) from saved stations.

### 🔍 Discovery Engine
- **City / Location Search**: Powered by responsive local geocoder services and reverse address autocompletes.
- **Name & ID Search**: Direct query access to individual senseBoxes using their strict hardware identifier or colloquial name.
- **GPS Discovery**: On-demand location queries matching active GPS coordinates against public datasets using highly accurate location clients.

### 📊 Deep Telemetry Analysis
- Detailed telemetry stream visualization including units, last updated timestamps, coordinates, station type, and exposure type (indoor vs. outdoor).
- Instant data refresh workflows ensuring current environmental parameters.

### 🔔 Configurable Home Screen Widgets
- Leverage Android's `AppWidgetProvider` architecture to configure widgets tracking specific senseBox metrics.
- Schedule custom-interval refresh cycles handled via `AlarmManager` routines for optimal battery conservation.

---

## 🛠️ Technical Architecture & Tech Stack

BoxViewer adheres to the strict guidelines of modern Android architecture (MVVM / Clean Architecture style) enabling clean decoupling between database layers, networking models, and the UI.

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetcompose) (declarative UI with type-safe compose navigation).
- **Architecture**: `ViewModel` + `StateFlow` + structured asynchronous coroutine builders (`lifecycleScope`, `collectAsStateWithLifecycle`).
- **Local Persistence Layer**: [Room Database](https://developer.android.com/training/data-storage/room) using Kotlin Symbol Processing (KSP) compilation to persist configurations, widgets, and offline caches.
- **Networking Server-Client**: [Retrofit 2](https://square.github.io/retrofit/) coupled with [OkHttp 4](https://square.github.io/okhttp/) and [Moshi](https://github.com/square/moshi) to carry out efficient JSON processing of the openSenseMap API.
- **Asynchrony Core**: Kotlin [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html).
- **Core Security Integration**: Secrets Gradle Plugin configured with safe `.env` runtime configurations to decouple keys and configurations from version-control processes.
- **Local Testing Ecosystem**: Fully integrated [Robolectric](https://robolectric.org/) testing suites running on high-speed headless JVM surfaces combined with [Roborazzi](https://github.com/takahirom/roborazzi) for visual screenshot regression testing.

---

## ⚙️ Development & Build Setup

### Prerequisites
- JDK 11 or higher
- Android Studio Ladybug or later
- Gradle 8.x +

### Quick Compilation & Build

To compile a debug variant of BoxViewer with Gradle:
```bash
gradle assembleDebug
```

To run standard unit tests and Robolectric instrumentation checks:
```bash
gradle test
```

To perform snapshot checks or visual regression validation using **Roborazzi**:
```bash
gradle verifyRoborazziDebug
```

To update current reference snapshots after making purposeful layout revisions:
```bash
gradle recordRoborazziDebug
```

---

## 🏠 App Homepage & Developer Support

BoxViewer is open-source and developed with love. Check out the project repository to contribute, report issues, or view the source code:

* **Project Homepage (Codeberg)**: [https://codeberg.org/nichu42/BoxViewer](https://codeberg.org/nichu42/BoxViewer)

### Support the Developer
If you are happy with the app and would like to support the ongoing development, please consider donating:
* **Support on Ko-fi**: [https://ko-fi.com/nichu42](https://ko-fi.com/nichu42)
* **Donate via Liberapay**: [https://liberapay.com/nichu42](https://liberapay.com/nichu42)

---

## 📜 Copyright & License Info

**Copyright (C) 2026 nichu42 and contributors**

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License v3.0** as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

---

## 🌍 Data & Attribution (openSenseMap)

This app utilizes the open API provided by **openSenseMap**, an open-source platform dedicated to collecting and exploring environmental sensor data from around the globe.

### What is openSenseMap?
Originally emerged from a research project at the University of Münster (Germany), openSenseMap has grown into one of the largest citizen-operated sensor networks in the world. It provides a free platform for schools, universities, scientists, and citizen enthusiasts to publish real-time environmental measurements—such as air quality, temperature, and humidity—and share them as Open Data.

### Who operates it?
The platform is operated and maintained by **openSenseLab gGmbH**, a non-profit organization based in Münster, Germany, dedicated to promoting digital sovereignty, education, and public participation in scientific environmental monitoring.

### Support Open Data!
openSenseMap is completely free to use and relies heavily on community contributions and donations to keep its servers running and its data accessible to all. If you love the environmental insights provided in this app, please consider supporting their project:
* **Explore**: [opensensemap.org](https://opensensemap.org)
* **Build**: [sensebox.de](https://sensebox.de)
* **Donate**: [Donate via Betterplace](https://www.betterplace.org/en/projects/89947-opensensemap-org-the-free-map-for-environmental-data)

---

## ⚠️ Affiliation Disclaimer

*The BoxViewer app is an independent project and is not affiliated with, endorsed by, or connected to openSenseMap (openSenseLab gGmbH) or senseBox (Reedu GmbH & Co. KG) in any way.*
