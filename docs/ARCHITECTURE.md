# Architecture

The single `app` Gradle module groups code by responsibility. Compose owns presentation and Navigation Compose selects the four top level destinations. The UI currently displays placeholders; later screens will use ViewModels exposing immutable StateFlow.

`ProjectRepository` is the boundary between product logic and persistence. `LocalProjectRepository` delegates to a Room DAO. Room suspend calls and Flow observations keep database work away from the main thread. Projects use UUID string identifiers and epoch millisecond timestamps. The first schema contains only projects. Future schema changes require explicit, tested migrations; destructive fallback is intentionally absent.

The database is defined but no screen creates or reads it yet. This keeps M0 focused on the foundation. M1 will wire it into project screens with a ViewModel and input validation. There is no network permission or synchronization layer.
