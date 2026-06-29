# Changelog

All notable changes to the BoxViewer project will be documented in this file.

## [0.42] - 2026-06-29

### Added
- **Coherent Local Synchronization**: Added instant database-to-widget updates whenever measurements are updated in the app (e.g., during startup synchronization, dashboard refreshes, detail screen sync, or toggling favorites) or when unit and formatting preferences are changed (temperature, pressure, wind units, conditional formatting, and AQI systems). This ensures home screen widgets, the dashboard, and details pages are instantly kept in sync without triggering redundant network calls.
- **Forced Update Rate-Limiting**: Implemented a 15-second rate-limit throttle on manual forced updates per senseBox to prevent hammering the openSenseMap API. Displays a debounced Toast warning if spammed, and gracefully degrades to resolving from the local SQLite cache.
- **Legacy Launcher Icon Fallbacks**: Added legacy square (`ic_launcher.png`) and round (`ic_launcher_round.png`) launcher icons across all density buckets (`mdpi` to `xxxhdpi`) generated from the updated logo. This ensures clean, high-quality, and consistent app icon presentation on older Android versions and system-level components (such as Toast notification headers on Android 12+).

### Changed
- **Launcher Icon Border & Margin Refinement**: Cleaned up the app icon artwork to remove the outer blue frame and built a 60% artwork scale safety margin directly into the source logo WebP file. This ensures the branding crops cleanly inside circular, squircle, and rounded-rectangle launcher shapes, and remains fully unclipped inside system-rendered Toast headers.
- **Launcher & Splash Icon Scaling**: Adjusted the adaptive launcher foreground insets to `8dp` and the startup splash screen icon insets to `32dp` to optimize visibility and render both icons at highly readable, prominent sizes on all devices.

### Fixed
- **Widget Layout Jump**: Fixed a layout jump during widget updates when the header is disabled. The header container and the loading spinner are now hidden entirely if all header elements (box name, update time, and buttons) are disabled by the user or hidden due to widget size constraints.
- **Widget Physical Refresh Force Flag**: Fixed a bug where the `force` parameter was not forwarded to the database repository in `SenseBoxWidgetProvider`, which previously caused manual widget refreshes to be ignored if the database cache was fresh.

## [0.41] - 2026-06-28

### Added
- **Small and Large Home Screen Widgets**: Added two new widget sizes, `senseBox Info (Small 2x1)` and `senseBox Info (Large 4x3)`, in addition to the standard `Medium 3x2` widget.
- **Responsive & Size-Adaptive Widgets**:
  - Widgets dynamically adapt their layout to the size constraints. When a widget's height is too short (< 74dp), it automatically forces the Metric Highlight (GRID) view.
  - Header elements (box name, update time, and action buttons) show or hide dynamically based on the widget's current width constraints to prevent clipping.
  - A visual loading spinner replaces the refresh button while network requests are active.
  - Improved layout background transparency handling.
- **Resilient Alarm Scheduling & Boot Autostart**:
  - Reschedules all widget update alarms automatically upon system reboot (`BOOT_COMPLETED`) or app update (`MY_PACKAGE_REPLACED`).
  - Added `RECEIVE_BOOT_COMPLETED` permission declaration in `AndroidManifest.xml`.
- **QR & Barcode Scanning Integration**:
  - Added QR code scanner integration to the Discovery screen search bar. Users can now scan senseBox IDs or URLs containing box IDs using any external ZXing-compatible barcode scanner app.
- **API Error Toasts**: Displays an error toast when openSenseMap API calls fail, showing the HTTP status code (if applicable) or the network error message.
- **Geocoding & Data Service Attributions**: Added explicit attribution entries for openSenseMap, Photon, and Nominatim (with OpenStreetMap contributors licensing details) in both `AboutScreen` (under 'Data & Geocoding Services') and `README.md` ('Data, Geocoding & Attribution').

### Changed
- **Geocoding Fallback Prioritization**: Prioritized Photon (komoot GmbH, Germany) as the primary geocoding fallback, and Nominatim (OSM Foundation) as the secondary fallback.
- **User-Agent Headers**: Set appropriate dynamic headers identifying `BoxViewer/versionname` on all geocoding (via `SenseBoxViewModel`) and openSenseMap (via Retrofit's `OkHttpClient`) network queries, resolving the version name dynamically from `BuildConfig.VERSION_NAME`.
- **Third-Party Licenses UI**: Restructured the third-party licenses list in `AboutScreen` to be grouped into logical categories (Core UI, Networking, Concurrency, Utilities, Testing) and sorted alphabetically within each category for better organization.
- **Privacy Disclosures**: Updated `PRIVACY.md` to disclose IP address transmission to external services (openSenseMap, Photon, Nominatim) under the GDPR and linked the privacy policies of Photon, Nominatim, and openSenseMap.

### Fixed
- **Cleaned Up Compose State Delegations**: Replaced generic `mutableStateOf` references with type-specific `mutableIntStateOf` and `mutableFloatStateOf` delegates on the Discovery screen to prevent boxing.
- **Refactored NowCast & InstantCast Signature**: Simplified parameter signatures by removing unused `boxId` parameters.

## [0.40] - 2026-06-27

### Added
- **Air Quality Index (AQI) Engine**: Six international standards (US EPA, UK DAQI, EU EAQI, Canada AQHI, India, China). A virtual AQI sensor is synthesized for every box with PM2.5/PM10 readings, and a 12-hour NowCast weighted average is computed from openSenseMap sensor stats.
- **Dedicated AQI Info Screen**: New `AqiInfoScreen` explains each supported AQI standard, breakpoints, and health guidance.
- **Unit Conversion System**: Per-sensor unit switching for temperature (°C/°F/K), pressure (hPa/mbar/Pa/inHg/mmHg), and wind (m/s/km/h/mph/kn). Unit unification normalizes common openSenseMap unit strings before conversion. Includes a JUnit/Robolectric test suite (`ConverterTest.kt`).
- **Settings Screen Rewrite**: New app-level `SettingsScreen` reachable from the dashboard, grouping general app settings, diagnostics/bug reporting, and local-only API debug logging. Diagnostics and API logs can be copied to the clipboard or shared via the native share sheet.
- **Widget Conditional Formatting & AQI Display**: Widget config now supports `useConditionalFormatting` and `aqiDisplayMode`, with matching database migrations (`MIGRATION_6_7`, `MIGRATION_7_8`, schema version 8).

### Changed
- **Discovery "Last Updated" Filter**: Reworked to use minute-level steps (15, 30, 60, 180, 360, 720, 1440, All Time) instead of hours, matching the slider UI.
- **Third-Party Licenses**: Updated `AboutScreen` attribution list for the new dependency set.
- **Canonical Sensor Ordering**: Newly added senseBoxes now default to a consistent sensor order (Temperature → Humidity → PM10 → PM2.5 → AQI → Pressure → Wind → other) on both the dashboard and in widget configuration, rather than the raw API order.

### Fixed
- **Clipboard API**: Replaced the non-existent `LocalClipboard` / `toClipEntry()` calls in the new Settings screen with the standard `LocalClipboardManager` + `AnnotatedString` API.
- **AQI Missing from Default Selection**: The synthesized virtual AQI sensor is now included in the default pre-selected metrics when adding a senseBox to the dashboard or configuring a new widget, provided PM2.5/PM10 data is available.
- **Discovery List Geocoding on De-Googled Devices**: `resolveLocationsFor` now falls back to Nominatim when the native Android `Geocoder` returns no results.
- **Widget Wake-to-Refresh**: Force-triggered updates (`ACTION_SCREEN_ON` / `ACTION_USER_PRESENT`) now bypass the `isInteractive` short-circuit so they don't silently skip on devices where the interactive state lags behind the broadcast.
- **Foreground Polling Lifecycle**: Box detail and dashboard auto-refresh loops are now gated by `repeatOnLifecycle(STARTED)` so they stop when the UI is not in the foreground.
- **Sensor Value Color Thresholds**: `SensorTheme.getValueColor` now applies thresholds to converted canonical units, so values in Kelvin, °F, Pa, inHg, etc. are colored correctly.
- **"All Time" Discovery Filter**: The discovery slider can now select the "All Time" option.

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
