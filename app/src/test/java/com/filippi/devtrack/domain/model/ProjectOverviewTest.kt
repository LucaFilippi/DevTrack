package com.filippi.devtrack.domain.model

import com.filippi.devtrack.data.local.entity.GoalEntity
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectOverviewTest {
    private val today = LocalDate.of(2026, 1, 1)

    @Test fun automaticPriorityFollowsDeadlineAndManualChoiceWins() {
        assertEquals(ProjectPriority.High, priority(days = -1))
        assertEquals(ProjectPriority.High, priority(days = 7))
        assertEquals(ProjectPriority.Medium, priority(days = 8))
        assertEquals(ProjectPriority.Medium, priority(days = 30))
        assertEquals(ProjectPriority.Low, priority(days = 31))
        assertEquals(ProjectPriority.Low, ProjectPriority.effective(null, null, today))
        assertEquals(ProjectPriority.High, priority(days = 100, override = "high"))
        assertEquals(ProjectPriority.Low, priority(days = 1, override = "low"))
    }

    @Test fun goalProgressCountsOnlyGoalsLinkedToEachProject() {
        val goals = listOf(goal("first", true), goal("second", true), goal("first", false))
        val progress = goalProgressByProject(goals)
        assertEquals(GoalProgress(1, 2), progress["first"])
        assertEquals(0.5f, progress.getValue("first").fraction, 0f)
        assertEquals(1f, progress.getValue("second").fraction, 0f)
        assertEquals(0f, (progress["third"] ?: GoalProgress()).fraction, 0f)
    }

    private fun priority(days: Long, override: String? = null): ProjectPriority {
        val deadline = today.plusDays(days).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        return ProjectPriority.effective(override, deadline, today)
    }

    private fun goal(project: String, completed: Boolean) =
        GoalEntity(projectId = project, name = "Goal", description = "Details", deadlineAt = 0, completed = completed)
}
