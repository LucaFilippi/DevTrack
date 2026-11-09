package com.filippi.devtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.filippi.devtrack.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY completed, deadlineAt, name, id")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT MAX(deadlineAt) FROM goals WHERE projectId = :projectId")
    suspend fun latestDeadline(projectId: String): Long?

    @Insert
    suspend fun insert(goal: GoalEntity)

    @Update
    suspend fun update(goal: GoalEntity): Int

    @Delete
    suspend fun delete(goal: GoalEntity): Int
}
