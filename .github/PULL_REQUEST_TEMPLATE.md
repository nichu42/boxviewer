## Summary

<!-- One or two sentences describing what this PR does. -->

Closes #<!-- issue number, or "N/A" if this is a small standalone fix. -->

## Type of Change

<!-- Check the appropriate box. -->

- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Breaking change (fix or feature that changes existing behavior)
- [ ] Refactor (no behavior change)
- [ ] Documentation only
- [ ] Translation / localization
- [ ] Build / CI / dependency update

## Checklist

<!-- Review the items below. Leave unchecked items with a brief explanation. -->

- [ ] I have read [`CONTRIBUTING.md`](./CONTRIBUTING.md) and the project philosophy section.
- [ ] I have read [`SECURITY.md`](./SECURITY.md) — no security implications, OR any security impact is described below.
- [ ] This PR does **not** add any `com.google.android.gms.*` (Google Play Services) dependency.
- [ ] This PR does **not** add any non-openSenseMap network endpoint.
- [ ] I have run `./gradlew assembleDebug` locally and it succeeds.
- [ ] I have run `./gradlew test` locally and it passes (or explained any failures below).
- [ ] I have updated [`CHANGELOG.md`](./CHANGELOG.md) under an `Unreleased` section.
- [ ] If I added or changed a third-party dependency, I have updated the licenses entry in `AboutScreen.kt`.
- [ ] This PR contains **one logical change**. Unrelated fixes are in separate PRs.
- [ ] I have not bumped `versionCode` or `versionName`.

## Testing

<!-- How did you test this change? On what device, Android version, and ROM (stock / GrapheneOS / CalyxOS / LineageOS)? -->

## Screenshots / Recordings

<!-- Required for UI changes. Use Roborazzi screenshots for visual diffs when possible. -->

## Additional Notes

<!-- Anything reviewers should know: trade-offs, follow-up work, known limitations. -->
