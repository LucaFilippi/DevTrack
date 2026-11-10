package com.filippi.devtrack.domain.model

import com.filippi.devtrack.data.local.entity.GoalEntity
import com.filippi.devtrack.data.local.entity.ProjectEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

enum class ProjectPriority(val key: String) {
    Low("low"), Medium("medium"), High("high");

    companion object {
        fun effective(manualPriority: String?, deadlineAt: Long?, today: LocalDate): ProjectPriority {
            entries.firstOrNull { it.key == manualPriority }?.let { return it }
            val deadline = deadlineAt?.let { Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).toLocalDate() }
                ?: return Low
            val days = ChronoUnit.DAYS.between(today, deadline)
            return when {
                days <= 7 -> High
                days <= 30 -> Medium
                else -> Low
            }
        }
    }
}

data class GoalProgress(val completed: Int = 0, val total: Int = 0) {
    val fraction: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

fun goalProgressByProject(goals: List<GoalEntity>): Map<String, GoalProgress> =
    goals.groupingBy { it.projectId }.fold(GoalProgress()) { progress, goal ->
        GoalProgress(progress.completed + if (goal.completed) 1 else 0, progress.total + 1)
    }

fun ProjectEntity.priorityOn(today: LocalDate): ProjectPriority =
    ProjectPriority.effective(manualPriority, deadlineAt, today)
