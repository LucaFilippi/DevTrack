package com.filippi.devtrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "goals",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("projectId")]
)
data class GoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val name: String,
    val description: String,
    val deadlineAt: Long,
    val completed: Boolean = false
)
