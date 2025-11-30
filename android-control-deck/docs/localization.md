# Localization workflow

## Source of truth
- Add and update user-facing strings in `app/src/main/res/values/strings.xml`. Treat this file as the canonical source before syncing translations.
- Mirror every string key in locale folders (e.g., `app/src/main/res/values-fr/strings.xml`).

## Updating translations
1. Add or rename keys in `values/strings.xml`.
2. Copy the same keys to each locale file with translated values.
3. Run a quick build or lint check (e.g., `./gradlew :app:assembleDebug` or `./gradlew :app:lint`) to surface missing or malformed resources.
4. Manually spot-check screens with long labels and RTL layout to verify wrapping/ellipsis still read well.

## Pull request checklist
- [ ] All new strings exist in `values/strings.xml` and every supported locale file.
- [ ] Language picker options cover the newly added locales (if applicable).
- [ ] Screens with long or RTL text still render without clipping.
- [ ] A quick build/lint has been run to catch resource regressions.
