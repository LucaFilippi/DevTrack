package com.filippi.devtrack.data.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.filippi.devtrack.data.local.database.AppDatabase
import com.filippi.devtrack.data.local.entity.GoalEntity
import com.filippi.devtrack.data.local.entity.ProjectEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.Instant

data class BackupDocument(
    val formatVersion: Int,
    val app: String,
    val exportedAt: Instant,
    val projects: List<ProjectEntity>,
    val goals: List<GoalEntity>
)

open class BackupFormatException(message: String) : IllegalArgumentException(message)
class UnsupportedBackupVersionException(message: String) : BackupFormatException(message)

object BackupCodec {
    const val CURRENT_FORMAT_VERSION = 1
    private const val APP_NAME = "DevTrack"

    fun encode(projects: List<ProjectEntity>, goals: List<GoalEntity>, exportedAt: Instant = Instant.now()): String {
        val root = JSONObject()
            .put("formatVersion", CURRENT_FORMAT_VERSION)
            .put("app", APP_NAME)
            .put("exportedAt", exportedAt.toString())
            .put("projects", JSONArray().also { array -> projects.forEach { array.put(it.toJson()) } })
            .put("goals", JSONArray().also { array -> goals.forEach { array.put(it.toJson()) } })
        return root.toString(2)
    }

    fun decode(json: String): BackupDocument {
        val root = try {
            JSONObject(json)
        } catch (error: Exception) {
            throw BackupFormatException("Malformed JSON: ${error.message ?: "invalid document"}")
        }
        val version = root.requiredInt("formatVersion")
        if (version != CURRENT_FORMAT_VERSION) {
            throw UnsupportedBackupVersionException("Unsupported backup format version: $version")
        }
        if (root.requiredString("app") != APP_NAME) {
            throw BackupFormatException("This file is not a DevTrack backup")
        }
        val exportedAt = try {
            Instant.parse(root.requiredString("exportedAt"))
        } catch (error: Exception) {
            throw BackupFormatException("Invalid export timestamp")
        }
        val projects = parseRecords(root.requiredArray("projects")) { it.toProject() }
        val goals = parseRecords(root.requiredArray("goals")) { it.toGoal() }
        validateRelationships(projects, goals)
        return BackupDocument(version, APP_NAME, exportedAt, projects, goals)
    }

    private fun <T> parseRecords(array: JSONArray, parser: (JSONObject) -> T): List<T> = try {
        buildList(array.length()) {
            for (index in 0 until array.length()) add(parser(array.getJSONObject(index)))
        }
    } catch (error: BackupFormatException) {
        throw error
    } catch (_: Exception) {
        throw BackupFormatException("Invalid backup record")
    }

    private fun validateRelationships(projects: List<ProjectEntity>, goals: List<GoalEntity>) {
        val projectIds = projects.map { it.id }
        if (projectIds.any { it.isBlank() } || projectIds.size != projectIds.toSet().size) {
            throw BackupFormatException("Projects contain duplicate or empty IDs")
        }
        val goalIds = goals.map { it.id }
        if (goalIds.any { it.isBlank() } || goalIds.size != goalIds.toSet().size) {
            throw BackupFormatException("Goals contain duplicate or empty IDs")
        }
        if (goals.any { it.projectId !in projectIds }) {
            throw BackupFormatException("A goal references a missing project")
        }
    }

    private fun ProjectEntity.toJson() = JSONObject()
        .put("id", id).put("name", name).put("description", description)
        .put("status", status).put("createdAt", createdAt).put("updatedAt", updatedAt)
        .putNullable("deadlineAt", deadlineAt).putNullable("manualPriority", manualPriority)
        .putNullable("classification", classification)

    private fun GoalEntity.toJson() = JSONObject()
        .put("id", id).put("projectId", projectId).put("name", name)
        .put("description", description).put("deadlineAt", deadlineAt).put("completed", completed)

    private fun JSONObject.toProject() = ProjectEntity(
        id = requiredString("id").also { requireNonBlank("id", it) },
        name = requiredString("name").also { requireNonBlank("name", it) },
        description = requiredString("description"),
        status = requiredString("status").also { requireNonBlank("status", it) },
        createdAt = requiredLong("createdAt").also { requireNonNegative("createdAt", it) },
        updatedAt = requiredLong("updatedAt").also { requireNonNegative("updatedAt", it) },
        deadlineAt = optionalLong("deadlineAt")?.also { requireNonNegative("deadlineAt", it) },
        manualPriority = optionalString("manualPriority"),
        classification = optionalString("classification")
    )

    private fun JSONObject.toGoal() = GoalEntity(
        id = requiredString("id").also { requireNonBlank("id", it) },
        projectId = requiredString("projectId").also { requireNonBlank("projectId", it) },
        name = requiredString("name").also { requireNonBlank("name", it) },
        description = requiredString("description"),
        deadlineAt = requiredLong("deadlineAt").also { requireNonNegative("deadlineAt", it) },
        completed = requiredBoolean("completed")
    )

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject = put(key, value ?: JSONObject.NULL)

    private fun JSONObject.requiredString(key: String): String = value(key, String::class.java)
    private fun JSONObject.optionalString(key: String): String? = if (!has(key) || isNull(key)) null else requiredString(key)
    private fun JSONObject.requiredInt(key: String): Int = value(key, Number::class.java).toInt()
    private fun JSONObject.requiredLong(key: String): Long = value(key, Number::class.java).toLong()
    private fun JSONObject.optionalLong(key: String): Long? = if (!has(key) || isNull(key)) null else requiredLong(key)
    private fun JSONObject.requiredBoolean(key: String): Boolean = value(key, Boolean::class.java)
    private fun JSONObject.requiredArray(key: String): JSONArray = value(key, JSONArray::class.java)

    private fun <T : Any> JSONObject.value(key: String, type: Class<T>): T {
        if (!has(key) || isNull(key)) throw BackupFormatException("Missing required field: $key")
        val value = try { get(key) } catch (_: Exception) { throw BackupFormatException("Invalid field: $key") }
        if (!type.isInstance(value)) throw BackupFormatException("Invalid field: $key")
        return type.cast(value)
    }

    private fun requireNonBlank(key: String, value: String) {
        if (value.isBlank()) throw BackupFormatException("Field must not be blank: $key")
    }

    private fun requireNonNegative(key: String, value: Long) {
        if (value < 0) throw BackupFormatException("Invalid timestamp: $key")
    }
}

class BackupService(
    private val resolver: ContentResolver,
    private val database: AppDatabase
) {
    suspend fun export(uri: Uri) {
        val (projects, goals) = database.withTransaction {
            database.projectDao().getAll() to database.goalDao().getAll()
        }
        val json = BackupCodec.encode(projects, goals)
        resolver.openOutputStream(uri)?.use { output ->
            output.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
        } ?: throw IOException("Unable to open backup destination")
    }

    suspend fun readAndValidate(uri: Uri): BackupDocument {
        val json = resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw IOException("Unable to open backup file")
        return BackupCodec.decode(json)
    }

    suspend fun restore(document: BackupDocument) = database.withTransaction {
        database.goalDao().deleteAll()
        database.projectDao().deleteAll()
        document.projects.forEach { database.projectDao().insert(it) }
        document.goals.forEach { database.goalDao().insert(it) }
    }
}
