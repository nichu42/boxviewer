# Changelog

All notable changes to the BoxViewer project will be documented in this file.

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
