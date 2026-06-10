# Privacy Policy

**Effective Date:** June 10, 2026

BoxViewer is an open-source companion app for the openSenseMap platform. We value your privacy and are committed to maintaining a clean, tracking-free, and secure user experience. This Privacy Policy details how BoxViewer processes information.

---

## 1. No Collection or Transmission of Personal Data
BoxViewer does **not** collect, store, or transmit any personal information, usage analytics, telemetry, crash reporting, or diagnostic data to any external server. 
* All diagnostic logs and error reporting are kept **strictly local** on your device.
* No background analytics pings or telemetry SDKs (such as Firebase, Google Analytics, or Crashlytics) are integrated.

---

## 2. Network Connections
To provide its core functionality, BoxViewer only communicates with the official **openSenseMap API**:
* **API Host:** `https://api.opensensemap.org`
* **Purpose:** To fetch public senseBox configurations, metadata, and current sensor measurements.
* No other third-party servers, advertisement networks, or tracking systems are contacted.

---

## 3. Location Data
When you grant location permissions to BoxViewer:
* The app uses the standard, native Android `LocationManager` to determine your device's coordinates (via GPS, Network, or Passive location providers).
* **Local Processing:** This location is used solely on-device to calculate distances to nearby senseBoxes or to show your location on the discovery map.
* **No Sharing:** Your location coordinates are never sent to our servers, openSenseMap, or any other third parties.

---

## 4. Local SQLite Storage
The app stores your preferences, bookmarked senseBoxes, and cached sensor data locally on your device in a secure SQLite database. 
* This data is never synchronized with any cloud services.
* If you uninstall the app or clear its data in Android settings, all stored bookmarks, cached sensor values, and configurations are permanently deleted.

---

## 5. Contact Info / Support
Since BoxViewer is open source, you can audit the complete source code, report issues, or contribute directly at:
* **Code Repository:** [https://codeberg.org/nichu42/BoxViewer](https://codeberg.org/nichu42/BoxViewer)

If you have any questions or feedback regarding this policy, feel free to open an issue on the repository.
