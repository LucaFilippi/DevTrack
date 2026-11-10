package com.filippi.devtrack.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.filippi.devtrack.data.local.entity.GoalEntity
import com.filippi.devtrack.data.local.entity.ProjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class BackupCodecTest {
    private val exportedAt = Instant.parse("2026-11-10T12:00:00Z")

    @Test fun emptyDatabaseRoundTripsWithFormatVersion() {
        val document = BackupCodec.decode(BackupCodec.encode(emptyList(), emptyList(), exportedAt))

        assertEquals(BackupCodec.CURRENT_FORMAT_VERSION, document.formatVersion)
        assertEquals(exportedAt, document.exportedAt)
        assertTrue(document.projects.isEmpty())
        assertTrue(document.goals.isEmpty())
    }

    @Test fun multipleRecordsOptionalFieldsUnicodeAndCompletionRoundTrip() {
        val project = ProjectEntity(
            id = "project-1", name = "Café 🚀", description = "Descrição com acentos",
            status = "active", createdAt = 1_000, updatedAt = 2_000, deadlineAt = 3_000,
            manualPriority = null, classification = "Portfolio"
        )
        val goal = GoalEntity(
            id = "goal-1", projectId = project.id, name = "Lançar versão",
            description = "Validar JSON ✓", deadlineAt = 2_500, completed = true
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(listOf(project), listOf(goal), exportedAt))

        assertEquals(listOf(project), decoded.projects)
        assertEquals(listOf(goal), decoded.goals)
    }

    @Test fun malformedUnsupportedAndBrokenRelationshipsAreRejected() {
        assertFailsWith<BackupFormatException> { BackupCodec.decode("not json") }
        assertFailsWith<UnsupportedBackupVersionException> {
            BackupCodec.decode(BackupCodec.encode(emptyList(), emptyList()).replace("\"formatVersion\": 1", "\"formatVersion\": 2"))
        }
        val orphan = """
            {"formatVersion":1,"app":"DevTrack","exportedAt":"2026-11-10T12:00:00Z",
             "projects":[],"goals":[{"id":"goal","projectId":"missing","name":"Goal","description":"","deadlineAt":1,"completed":false}]}
        """.trimIndent()
        assertFailsWith<BackupFormatException> { BackupCodec.decode(orphan) }
    }

    @Test fun duplicateIdsAndInvalidDatesAreRejected() {
        val duplicate = BackupCodec.encode(
            listOf(ProjectEntity(id = "same", name = "One"), ProjectEntity(id = "same", name = "Two")),
            emptyList(), exportedAt
        )
        assertFailsWith<BackupFormatException> { BackupCodec.decode(duplicate) }

        val invalidDate = BackupCodec.encode(
            listOf(ProjectEntity(id = "p", name = "Project", createdAt = 0, updatedAt = 0)), emptyList(), exportedAt
        )
            .replace("\"createdAt\": 0", "\"createdAt\": -1")
        assertFailsWith<BackupFormatException> { BackupCodec.decode(invalidDate) }
    }

    private inline fun <reified T : Throwable> assertFailsWith(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected ${T::class.simpleName}")
        } catch (error: Throwable) {
            assertTrue("Expected ${T::class.simpleName}, got ${error::class.simpleName}", error is T)
        }
    }
}
