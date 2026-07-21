# Contributing to BoxViewer

Thank you for your interest in BoxViewer! This project is a privacy-first openSenseMap Android client, distributed as open-source under the **GNU GPL v3**. Contributions of all sizes are welcome — bug reports, translations, documentation fixes, and code changes.

BoxViewer is primarily maintained by a single developer in their spare time. That has a few practical consequences for contributors, all of which are spelled out below to keep expectations clear on both sides.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Project Philosophy (Read This First)](#project-philosophy-read-this-first)
- [How to Report a Bug](#how-to-report-a-bug)
- [How to Suggest an Enhancement](#how-to-suggest-an-enhancement)
- [Submitting a Pull Request](#submitting-a-pull-request)
- [Development Setup](#development-setup)
- [Code Style and Conventions](#code-style-and-conventions)
- [Translations / Localization](#translations--localization)
- [Security Vulnerabilities](#security-vulnerabilities)
- [License](#license)

---

## Code of Conduct

By participating, you agree to the Contributor Covenant Code of Conduct in [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md). Personal attacks, gatekeeping, or bad-faith arguments are not tolerated. The maintainer reserves the right to close issues or PRs that become unproductive.

## Project Philosophy (Read This First)

BoxViewer has a small set of non-negotiable design principles. Understanding them before opening an issue or PR will save everyone time:

1. **De-Googled compatibility is core, not optional.** The app must remain 100% free of Google Play Services (`com.google.android.gms.*`) dependencies. Use the native `android.location.LocationManager` and standard Android APIs, never Google-specific alternatives. Adding any GMS dependency will be rejected.
2. **The only network service contacted is openSenseMap.** No analytics, no telemetry, no third-party tracking SDKs. The only permitted exceptions are the native `Geocoder` (used for the address-search feature) and the OpenStreetMap-based fallbacks (Photon, then Nominatim) when the native geocoder returns no results. Do not add any other network endpoints.
3. **Battery and API friendliness are first-class concerns.** Foreground auto-refreshes are throttled to 60 s minimum intervals. Avoid introducing eager polling, listeners that fire on every state change, or background work that wakes the device.
4. **Local-only diagnostics.** The JSONL debug log is opt-in and stays on-device. Never add background log uploads, remote crash dumps, or analytics pings.
5. **GPL v3 only.** All contributions must be compatible with the GNU GPL v3. New third-party dependencies must use GPL-compatible licenses (Apache 2.0, MIT, BSD, etc. are fine; AGPL, SSPL, or proprietary are not).

## How to Report a Bug

1. **Search first.** Check [existing issues](https://github.com/nichu42/boxviewer/issues) to avoid duplicates.
2. **Use the Bug Report template.** Please fill in: BoxViewer app version (Settings → About), Android version, device model, ROM (stock/GrapheneOS/CalyxOS/LineageOS/etc.), reproduction steps, expected vs. actual behavior, and screenshots if applicable. Incomplete reports cannot be reproduced and may be closed.
3. **One bug per issue.** Multiple unrelated bugs in a single report slow down triage.

**What the maintainer cannot do:**
- Reproduce issues on every device or ROM. Bugs that only manifest on hardware the maintainer does not own are very hard to fix. **If you can capture a logcat excerpt or the opt-in `api_*.jsonl` debug log, please attach it.**
- Debug issues on closed-source forks or unofficial builds.

## How to Suggest an Enhancement

Open an issue using the Feature Request template. Please include:

- The **user-facing problem** you're trying to solve (not just "add feature X").
- Why the existing app cannot solve it.
- A short sketch of the proposed UX.

Enhancements that compromise the [Project Philosophy](#project-philosophy-read-this-first) (e.g., "add Firebase Analytics", "switch to Google Maps") will be closed without discussion. Enhancements that touch the openSenseMap API contract should ideally be coordinated with the openSenseMap team first — link any upstream conversation in the issue.

## Submitting a Pull Request

### When to Open a PR vs. Open an Issue First

**Small, low-risk PRs (typos, one-line fixes, simple translations) — open the PR directly.** No need to file an issue.

**Anything non-trivial — open an issue first and wait for the maintainer's reply.** This includes:
- New features or screens
- Changes to the openSenseMap API client or models
- Changes to the Room database schema
- Refactors spanning multiple files
- New third-party dependencies

This is not gatekeeping. It is the practical way to avoid the much more common scenario where a contributor spends hours on a PR that the maintainer would have redesigned before merging. A 5-minute issue check-in saves days of rework.

### PR Requirements

Once the issue is agreed upon (or for small fixes):

- **One logical change per PR.** A typo fix and a refactor should be two PRs. PRs that bundle unrelated changes are hard to review and slow down merging.
- **Write a clear description.** Use the PR template. Explain *what* changed and *why*. Reference the issue it closes (`Closes #123`).
- **Keep the diff focused.** Don't reformat unrelated code in the same PR — it makes the actual change impossible to review.
- **Run a clean build locally before pushing.** `./gradlew assembleDebug` must succeed.
- **Update `CHANGELOG.md`** under a new "Unreleased" section with `### Added` / `### Changed` / `### Fixed` headers as appropriate. Do this even for small fixes; the maintainer may rewrite wording.
- **Update `AboutScreen.kt`** if you add or change a third-party dependency — the third-party licenses screen is curated manually, not auto-generated).
- **Do not bump version numbers** (`versionCode`, `versionName` in `app/build.gradle.kts`). The maintainer does this at release time.
- **Do not introduce GMS dependencies.** This is non-negotiable. The CI does not enforce it, so the check is on you.

### Review and Merge Cadence

The maintainer reviews PRs in their spare time. Expect first-response times of **1–3 weeks**, not days. After approval, squash-merge is the default. If your PR is not reviewed within a month, a polite bump on the issue is fine.

Stale PRs (no activity for 60+ days) may be closed. Reopen or resubmit when ready.

## Development Setup

### Prerequisites

- **JDK 17** (the project's toolchain pin; newer JDKs may work but are untested)
- **Android Studio Ladybug (2024.2.1) or newer** with the Android SDK installed
- **Android SDK Platform 37** (`compileSdk = 37` in `app/build.gradle.kts`)
- **Gradle 9.x** (provided by the `gradlew` wrapper — do not use a system Gradle)

### Build

```bash
# Clone
git clone https://github.com/nichu42/boxviewer.git
cd boxviewer

# Set up local secrets (for signed release builds only; not needed for debug)
cp .env.example .env

# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew test
```

The debug variant uses a debug keystore and does not require any signing setup.

### Project Layout

```
app/
  src/main/java/de/nichu42/boxviewer/   # Kotlin source (single-module app)
  src/main/res/                          # Layouts, drawables, strings, themes
  src/main/res/values/                   # English strings (default)
  src/main/res/values-de/                # German translations
  src/test/                              # Local JVM unit tests (JUnit, Robolectric, Roborazzi)
gradle/libs.versions.toml                # Version catalog — all dependency pins live here
```

## Code Style and Conventions

BoxViewer does **not** have a formal linter or formatter configured in the build (no ktlint, no detekt, no `kotlin.code.style` property). The conventions below are therefore enforced by code review, not by CI:

- **Kotlin official style** for indentation, naming, and import ordering. Use Android Studio's default formatter (`Code → Reformat Code`) before committing.
- **Compose first.** New UI should be written in Jetpack Compose, not in legacy XML layouts. Existing XML layouts (mainly used by the AppWidget provider) are kept where Compose is not available.
- **MVVM with `ViewModel` + `StateFlow`.** No `LiveData` for new code. Coroutine builders should use `lifecycleScope` or `viewModelScope`, not `runBlocking`.
- **Repository pattern for data sources.** Network, database, and preferences access goes through repository classes. Do not call Retrofit or Room DAOs directly from ViewModels or composables.
- **KSP, not kapt.** New annotation processors must be configured for KSP.
- **No GMS imports.** Any `com.google.android.gms.*` import will block the PR.
- **Version catalog for new dependencies.** Add new entries to `gradle/libs.versions.toml`, not to `app/build.gradle.kts` directly.
- **Tests for bug fixes.** A PR that fixes a bug should ideally include a regression test. Visual changes should include a Roborazzi screenshot test.

If you want to add a real linter (ktlint is the most popular choice) as a separate PR, that is welcome — but it must not be bundled with a feature change.

## Translations / Localization

BoxViewer uses **POEditor** to collaboratively manage app translations. We welcome contributions for correcting existing strings or translating the app into new languages. 

### ✍️ How to Contribute Translations
1. **Join the Translation Project**: Visit our public join portal on POEditor:
   👉 **[Translate BoxViewer on POEditor](https://poeditor.com/join/project/3BO0G8m3BZ)**.
2. **Submit Translations**: You can suggest corrections or translate untranslated strings directly on the web interface.
3. **Adding a New Language**: You can suggest/request a new target language within the POEditor project portal. Once translations are complete, a developer will update the app's locale configuration and sync workflows to enable it.
4. **Moderation**: All submitted translations go through a moderation queue to ensure accuracy and prevent spam before they are merged.

*Alternatively, if you prefer editing XML resources directly, you can submit a Pull Request following the guidelines below:*

### 🛠️ Manual Translation Guidelines (via XML)
1. **Adding a new language:** copy `app/src/main/res/values/strings.xml` to `app/src/main/res/values-<lang>/strings.xml` (e.g., `values-fr/`, `values-es/`, `values-nl/`) and translate every string. The `<lang>` code follows Android's standard ISO 639-1 conventions. Do **not** add a new language by editing the existing `values-de/` directory.
2. **Improving an existing translation:** edit the file directly in the appropriate `values-<lang>/` directory.
3. **Do not translate keys, only values.** String keys (`R.string.dashboard_title`, etc.) are referenced from Kotlin code and must stay identical.
4. **Preserve placeholders.** If a string contains `%1$s`, `%1$d`, or similar, keep the placeholder structure intact in the translation. The order of placeholders is meaningful in some languages — translate around the placeholders rather than reordering them.
5. **Plural forms.** For strings with quantities, prefer Android's `<plurals>` element over hard-coded singular/plural variants. If you add a new plural string, reference the `getQuantityString` API.
6. **Run the build** with your new translation directory in place to make sure nothing references a missing string key: `./gradlew assembleDebug`.

If you are not a native speaker, mark the PR with `[i18n]` in the title and note your language proficiency in the description. Native-speaker review is the maintainer's responsibility.

## Security Vulnerabilities

**Do not report security issues through public GitHub issues.** See [`SECURITY.md`](./SECURITY.md) for the coordinated disclosure policy and PGP-encrypted contact email. Reports sent there are acknowledged within 5 business days.

## License

By contributing to BoxViewer, you agree that your contributions will be licensed under the **GNU General Public License v3.0** (the same license as the rest of the project). If you are incorporating code from another project, you are responsible for confirming it is GPL-compatible and crediting the original source in the PR description.
