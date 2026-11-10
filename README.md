# DevTrack

DevTrack is an offline first Android app for organizing projects, goals, tasks, and personal productivity. It is a portfolio project developed in small milestones.

## Status

Version 0.1.3 shows goal completion for each project on the dashboard. Projects have optional classifications and can use automatic or manual priority.

Version 0.1.2 adds the DevTrack launcher icon and Settings for appearance and app language. Settings remembers light, dark, or system appearance and English, Brazilian Portuguese, or the device language.

Version 0.1.1 adds project goals. A project with a deadline can have any number of goals. Each goal needs a name, description, and a deadline before its project's deadline. Use the Goals tab to add, edit, complete, or delete them. Project management and the dashboard work offline, and the interface supports English and Brazilian Portuguese.

## Stack

Kotlin, Jetpack Compose, Material 3, Navigation Compose, Room/SQLite, Coroutines/Flow, Gradle Kotlin DSL, and JUnit.

## Build and run

Open this directory in Android Studio, install Android SDK 35 and JDK 17, sync Gradle, then run the `app` configuration on an Android 8.0 (API 26) or newer device. Run `:app:assembleDebug` for a debug APK and `:app:testDebugUnitTest` for local tests. Instrumented Room tests require an emulator or device.

## Roadmap

Tasks, milestones, expanded dashboard statistics, notes, and local reminders are planned. No account or internet connection is required.

See [architecture](docs/ARCHITECTURE.md) for the initial design.

## License

MIT; see [LICENSE](LICENSE).
