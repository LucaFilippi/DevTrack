# DevTrack

<p align="center">
  <strong>A focused, offline-first Android workspace for projects, goals, and deadlines.</strong>
</p>

<p align="center">
  Built with Kotlin and Jetpack Compose, DevTrack keeps personal project planning simple, private, and available on the device you use every day.
</p>

<p align="center">
  <img src="docs/screenshots/dashboard.png" alt="DevTrack dashboard showing project progress and upcoming deadlines" width="220" />
  <img src="docs/screenshots/projects.png" alt="DevTrack projects screen with search and status filters" width="220" />
  <img src="docs/screenshots/goals.png" alt="DevTrack goals screen showing project goals and deadlines" width="220" />
</p>

## Overview

DevTrack is a local-first project and goal tracker for developers and makers. Create projects, break them into measurable goals, assign deadlines, and use the dashboard to see progress at a glance—without an account, server, or network connection.

The current release is **0.2.0**. See the [v0.2.0 release](https://github.com/LucaFilippi/DevTrack/releases/tag/v0.2.0) for release notes and artifacts.

## Highlights

- **Dashboard overview** — See active projects, completed projects, open work, upcoming deadlines, and goal completion progress.
- **Project and goal tracking** — Create, search, filter, edit, archive, and delete projects; link goals and mark them complete.
- **Priority and classification** — Use automatic or manual project priority and group work by classification.
- **Local JSON backups** — Export projects and goals through Android's document picker and restore them with validation and an explicit replace-data confirmation.
- **Deadline reminders** — Opt in to local notifications 7 days, 5 days, and 1 day before active project and incomplete goal deadlines.
- **Offline-first storage** — Projects and goals are stored locally with Room/SQLite; no account or internet connection is required.
- **Reliable settings** — Appearance, language, and deadline reminders are active and persisted; remaining productivity controls are labeled as planned.
- **Privacy by design** — Backup files stay under the user's control and are never uploaded by DevTrack.

### Backup and restore

Backups use a documented JSON format (`formatVersion: 1`) containing projects, goals, IDs, timestamps, statuses, priorities, classifications, deadlines, and completion states. Export and import use Android's Storage Access Framework, so no storage permission is required.

Restore is intentionally **replace-only**: the selected file is parsed and validated before a Room transaction clears and recreates projects and goals. Invalid or unsupported files leave existing data unchanged. App settings are not included in the backup.

### Deadline reminders

Turn on **Deadline reminders** in Settings → Productivity. On Android 13 and newer, allow DevTrack to post notifications when prompted. Reminders are scheduled locally for 9:00 a.m. device time and checked again when the app opens or data changes. Android may deliver background alarms later to save battery. Completed or archived projects and completed goals receive no reminders. Notifications use the selected calendar deadline, so travel between time zones does not shift the due date.

## Screenshots

<p align="center">
  <img src="docs/screenshots/dashboard.png" alt="Dashboard with project metrics and progress" width="250" />
  <img src="docs/screenshots/projects.png" alt="Projects list with search and filters" width="250" />
  <img src="docs/screenshots/goals.png" alt="Goals list with deadlines and completion status" width="250" />
</p>

<p align="center">
  <img src="docs/screenshots/new-project.png" alt="New project form" width="250" />
  <img src="docs/screenshots/new-goal.png" alt="New goal form" width="250" />
  <img src="docs/screenshots/settings.png" alt="DevTrack settings screen" width="250" />
</p>

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose and Material 3 |
| Navigation | Navigation Compose |
| Persistence | Room backed by SQLite |
| Async data | Kotlin Coroutines and Flow |
| Build | Gradle Kotlin DSL, Android Gradle Plugin |
| Testing | JUnit and AndroidX instrumentation tests |

## Requirements

- Android Studio with Android SDK 35
- JDK 17 or a compatible Android Studio JDK
- Android 8.0 (API 26) or newer for running the app
- An emulator or physical Android device for instrumented tests

## Build and run

1. Clone the repository and open it in Android Studio.
2. Allow Gradle to sync and install the required SDK components.
3. Select the `app` run configuration.
4. Run it on an Android 8.0+ emulator or device.

From a terminal:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

On Windows, use `gradlew.bat`. Instrumented tests require a connected device or running emulator:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Architecture

The single `app` module is organized by responsibility. Compose owns presentation, the ViewModel coordinates state and user actions, repositories define persistence boundaries, and Room owns local database integrity. Backup serialization and validation live in a dedicated data-layer service. Local deadline reminders use Android notifications and alarms. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for details.

## Roadmap

Tasks, milestones, expanded dashboard statistics, notes, week start, and default task priority are planned. Cloud synchronization and authentication are outside the 0.2.0 scope.

## License

DevTrack is released under the MIT License. See [LICENSE](LICENSE) for the full text.
