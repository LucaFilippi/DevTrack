# Changelog

## [0.2.0] — Deadline Reminders

### Added

- Optional local notifications for active projects and incomplete goals 7 days, 5 days, and 1 day before their deadlines.
- Android 13 notification permission request and a reminder toggle in Settings.
- Daily alarm scheduling with recovery after reboot, time changes, and time zone changes.

### Changed

- Updated the app version and documentation for 0.2.0.

## [0.1.4] — Backup & Reliability Update

### Added

- Local JSON export and import for projects and goals through Android's Storage Access Framework.
- Replace-only restore confirmation with strict format, field, ID, timestamp, and relationship validation.
- Room schema export under `app/schemas/` and migration coverage for the supported upgrade path.
- Backup, restore, and unavailable-productivity-setting messages in English and Brazilian Portuguese.

### Changed

- Settings now exposes working appearance and language controls plus Backup and Restore actions.
- Notifications, week start, and default task priority are clearly marked as planned instead of appearing active.
- README and architecture documentation now describe the v0.1.4 behavior and limitations.

### Fixed

- Invalid or unsupported backup files no longer reach the database replacement step.
- Restore failures are contained and reported without exposing raw exceptions or silently deleting existing data.

### Testing

- `:app:testDebugUnitTest` passes.
- Instrumented test sources compile, including backup codec, restore, and Room migration tests.
- `:app:assembleDebug` passes.
- Full instrumented execution requires a connected Android emulator or device.
