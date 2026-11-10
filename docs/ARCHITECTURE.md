# Architecture

The single `app` Gradle module groups code by responsibility. Compose owns presentation and Navigation Compose selects the four top level destinations. Dashboard and Projects share an activity ViewModel that exposes Room results as immutable StateFlow and owns project writes.

`ProjectRepository` is the boundary between product logic and persistence. `LocalProjectRepository` delegates to a Room DAO. Room suspend calls and Flow observations keep database work away from the main thread. Projects use UUID string identifiers and epoch millisecond timestamps. The first schema contains only projects. Future schema changes require explicit, tested migrations; destructive fallback is intentionally absent.

Deadlines are calendar dates stored as UTC midnight, so moving between time zones preserves the selected date. Room migration 1 to 2 adds the goals table and preserves existing projects. Migration 2 to 3 adds optional project classification and manual priority. A foreign key allows multiple goals per project and deletes linked goals when their project is deleted. Goal writes and project deadline changes validate both sides of the deadline rule inside database transactions. Dashboard progress is the completed fraction of the goals linked to each project. There is no network permission or synchronization layer.
