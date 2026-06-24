# Changelog

All notable changes to the BoxViewer project will be documented in this file.

## [0.30] - 2026-06-24
### Added
- **SenseBox Sharing via QR & Deep Links**: Long-press a senseBox on the dashboard or detail screen to open the new Share sheet — generate a QR code (ZXing), share it as a PNG via the native Android share sheet (backed by a new `FileProvider`), or save it directly to the gallery on Android 10+. Recipients scan the QR or open the HTTPS link and land on a new `AddBoxConfirmScreen` previewing the box with explicit **Add to Dashboard** / **View Details** options.
- **Deep Link Entry Points**: Registered `boxviewer://box/{id}` (custom scheme) and an auto-verified HTTPS App Link `https://nichu42.codeberg.page/BoxViewer` so shared links open BoxViewer directly from any chat app or browser, with a graceful web fallback page (`web/index.html` + Open Graph metadata) hosted on Codeberg Pages (`web/.well-known/assetlinks.json` for App Links verification).
- **First-Time User Onboarding**: Comprehensive empty-state on the dashboard for users with no saved senseBoxes — friendly icon, "No senseBoxes added yet" heading, explanatory copy, and an "Add a senseBox" primary CTA, followed by a "What is openSenseMap?" infocard explaining the platform and the role of BoxViewer. On the detail screen, an "Add to Dashboard" call-to-action card appears for unfavorited stations, plus a "Home Screen Widgets" infobox explaining how to add a widget on the home screen. Reorder mode also gained an instructional infocard for drag/remove/save actions.
- **Splash Screen**: Adopted `androidx.core.splashscreen` with a themed `Theme.App.Starting`, dedicated `ic_splash_icon` drawable, and a `splash_background` color that respects dark mode (`values-night/colors.xml`). MainActivity set to `singleTop` launch mode to keep deep-link navigation stable.
- **Widget Localization & RTL Support**: Externalized all hard-coded widget placeholder strings (`Temperatur`, `Feuchte`, `Updated just now`, sensor category labels, `--` no-value markers, content descriptions) into `strings.xml`. Widget layout now uses `marginStart/End` instead of `marginLeft/Right` for proper right-to-left rendering on supported locales.

### Changed
- **Application Identity**: Renamed the entire application package from `com.example` to `de.nichu42.boxviewer` (namespace, `applicationId`, manifest, widget broadcast action prefix, all source files). Aligns the on-disk package with the published app identifier.
- **Signing Key**: Future Codeberg releases are signed with the same release key used for the Google Play closed-testing track. **Existing Codeberg users (0.10) will need to uninstall before installing 0.30** due to the certificate change.
- **Dependency Refresh**: compileSdk & targetSdk bumped to 37. Kotlin 2.4.0, Compose BOM 2026.06, Room 2.8.4, Navigation 2.9.8, Lifecycle 2.11.0, Activity-Compose 1.13.0, core-ktx 1.19.0.
- **Widget Timestamp Legibility**: Bumped the widget "last updated" text from 9sp to 11sp for improved readability at a glance.

### Removed
- **Unused Dependencies**: Dropped Firebase BOM, Google Play Services Location (`play-services-location`), Accompanist Permissions, and the AndroidX Camera libraries — none were linked at runtime, and their removal completes the de-Googled dependency profile mandated by the project charter (no GMS dependencies, no telemetry).
- **Stale Debug Signing Config**: Removed the now-unneeded `debugConfig` signing block from `build.gradle.kts`; debug builds now use Android's default debug keystore as intended.

### Fixed
- **Keystore Gitignore Hygiene**: Replaced the narrow `release-upload-key.jks` ignore pattern with `*.jks` / `*.b64` wildcards to prevent any signing key or base64 export from being committed, regardless of filename.

---

## [0.20] - 2026-05-29
### Added
- **Widget Customization Options**: Users can now configure the widget layout to show Full Details, Value & Unit (hiding category labels), or Value Only (hiding both labels and units).
- **Proportional Scaling**: Both text and sensor icons in the widget now scale up to 200% on Android 12+ (API 31+).
- **Widget Header Toggles**: Switch toggles added to show or hide the manual refresh button, settings configuration gear, senseBox name, and last updated timestamp in the widget header.
- **Home-Screen Reconfigurability**: Enabled Android 12+ widget long-press settings, allowing users to reconfigure placed widgets directly from the home screen.
- **Clean Widget Setup UX**: Removed numbered section headers and introduced horizontal dividers between option sections. Reordered all settings into a logical, natural user flow (Data Choice -> Styling -> UI elements -> Sync Frequency).
- **Graceful Downgrade Protection**: Manually inspects the SQLite database file on launch. If an app downgrade is detected (via sideloading), users are prompted to either clear the database (Start Over) or visit Codeberg to install the matching update.
- **Migration & Upgrade Notices**: Bumps database schema to version 6 and adds `MIGRATION_3_4`, `MIGRATION_4_5`, and `MIGRATION_5_6` paths. Implemented a feature-agnostic notification popup if a destructive schema reset ever occurs.

### Fixed
- **Drag-and-Drop Reordering**: Wrapped metric rows in Compose key blocks to preserve drag gesture states, allowing users to reorder metrics freely without cancellation.
- **Settings Card Alignments**: Constrained visualization format and metric display style choice cards to a uniform height of 80.dp to ensure clean layout alignment regardless of text wrapping.

---

## [0.10] - 2026-05-26
### Added
- **De-Googled Location Integration**: Removed Google Play Services (GMS) dependencies. Replaced `FusedLocationProviderClient` with native Android `LocationManager` (GPS, Network, Passive) for GrapheneOS/LineageOS compatibility.
- **Local Diagnostics & Diagnostics Card**: Integrated local uncaught exceptions crash capture and added a diagnostics card on the About screen with options to view and share logs.
- **OpenStreetMap Nominatim Fallback**: Dual-geocoding supporting Android native geocoder with OSM Nominatim fallback for location coordinates.
- **Smart SQLite Caching**: Implemented a 60-second database query cache cooldown in the repository to prevent redundant openSenseMap API hits.
- **Licenses Screen**: Added a custom open-source libraries license attribution screen.

### Fixed
- **Widget Update Scheduling**: Standardized repeating widget alarms and forced immediate updates when screen turns on or user is present.

---

## [0.10-prerelease] - 2026-05-24
- Initial release of BoxViewer Prerelease.
- Supports senseBox bookmarking, sensor values list representation, discovery map search, and list home screen widgets.
