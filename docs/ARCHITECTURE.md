# Architecture

DevTrack is a single `app` Gradle module organized by responsibility. Compose owns presentation and Navigation Compose selects the four top-level destinations. `ProjectsViewModel` exposes Room results as immutable `StateFlow`, coordinates project and goal writes, and owns backup operation state for the Settings screen.

## Data and persistence

`ProjectRepository` is the boundary between product logic and persistence. `LocalProjectRepository` delegates project operations to a Room DAO, while `GoalRepository` coordinates goal operations and deadline validation with database transactions. Room suspend calls and Flow observations keep database work away from the main thread.

Projects and goals use UUID string identifiers and epoch-millisecond timestamps. Deadlines are calendar dates stored as UTC midnight, so moving between time zones preserves the selected date. A foreign key allows multiple goals per project and deletes linked goals when its project is deleted.

The database is currently schema version 3:

- Migration 1 → 2 adds the `goals` table and preserves existing projects.
- Migration 2 → 3 adds optional project classification and manual priority.

Destructive migration fallback is intentionally absent. Room schema export is enabled through the KAPT `room.schemaLocation` argument, and generated schemas are committed under `app/schemas/`. `MigrationTest` verifies the supported 1 → 3 upgrade path; DAO and repository tests cover foreign keys, cascade deletion, and deadline integrity.

## Backup and restore

`data.backup.BackupService` keeps file I/O and persistence concerns out of Compose. It uses Android's Storage Access Framework `ContentResolver` to write and read user-selected JSON documents without storage permissions.

`BackupCodec` defines format version 1. A document contains the app name, an ISO-8601 creation timestamp, all project fields, and all goal fields. Decoding validates the format version, required fields, timestamp values, unique IDs, and project/goal relationships before a restore can begin.

Restore is explicitly replace-only in v0.1.4. The selected document is fully parsed and validated first. Only then does `BackupService.restore` enter a Room transaction, delete goals and projects, and insert the validated records. Any database failure rolls back the transaction, and invalid files never reach the delete step. Settings are deliberately excluded from backups.

## UI and settings

Compose screens provide Dashboard, Projects, Goals, and Settings with Material 3 and the existing bottom navigation. Appearance, language, and the notification opt-in are persisted through `AppPreferences`. `DeadlineReminders` schedules a daily local alarm, checks deadlines against the device's calendar date, and records delivered reminders to avoid duplicates. A receiver resumes the schedule after reboot or clock changes. Week start and default task priority remain planned. Backup actions are available in Settings and surface validation, success, and failure states without exposing raw exceptions.

The app has no network permission or synchronization layer. Exported JSON remains under the user's control and is never transmitted by DevTrack.
