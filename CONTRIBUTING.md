# Contributing

Thanks for improving CREST. This project supports a school e-library workflow, so changes should keep student and teacher tasks reliable, accessible, and easy to validate.

## Development Setup

1. Install Android Studio Ladybug 2024.2.1 or newer.
2. Use JDK 17.
3. Add `app/google-services.json`.
4. Add Appwrite values to `local.properties`.
5. Run `gradlew.bat assembleDebug` on Windows or `./gradlew assembleDebug` on macOS/Linux.

## Branches and Commits

- Use short feature branches such as `feature/research-filters` or `fix/pdf-loading-state`.
- Use conventional commit prefixes when practical: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `ci:`.
- Keep commits focused. Separate app behavior, documentation, and formatting changes when they are unrelated.

## Pull Request Checklist

- The change is scoped to one feature, fix, or maintenance task.
- UI changes include loading, empty, error, and permission states when relevant.
- ViewModel or repository behavior has unit coverage where practical.
- Compose UI changes are covered by instrumented tests for important flows.
- New configuration is documented in `README.md` or `docs/`.
- No local files, credentials, build logs, IDE state, or generated outputs are committed.

## Validation

Run the fastest relevant checks locally before opening a pull request:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

For UI or navigation changes:

```bash
./gradlew connectedDebugAndroidTest
```

For performance-sensitive changes:

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

## Code Style

- Follow the existing Kotlin and Compose style.
- Keep ViewModels free of direct UI dependencies.
- Prefer repository or use-case changes over putting business logic in composables.
- Keep Firebase and Appwrite access behind data-layer classes.
- Avoid committing generated output unless the generated file is a reviewed product artifact.
