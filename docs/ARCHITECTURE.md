# Architecture

The single `app` Gradle module groups code by responsibility. Compose owns presentation and Navigation Compose selects the four top level destinations. Dashboard and Projects share an activity ViewModel that exposes Room results as immutable StateFlow and owns project writes.

`ProjectRepository` is the boundary between product logic and persistence. `LocalProjectRepository` delegates to a Room DAO. Room suspend calls and Flow observations keep database work away from the main thread. Projects use UUID string identifiers and epoch millisecond timestamps. The first schema contains only projects. Future schema changes require explicit, tested migrations; destructive fallback is intentionally absent.

Project deadlines are calendar dates stored as UTC midnight, so moving between time zones preserves the selected date. Dashboard completion is the fraction of non-archived projects marked completed. There is no network permission or synchronization layer.
