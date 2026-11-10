package com.filippi.devtrack.ui

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.filippi.devtrack.R
import com.filippi.devtrack.domain.model.ProjectPriority
import java.util.Locale

internal enum class ThemeChoice(val label: Int) {
    System(R.string.follow_system),
    Light(R.string.theme_light),
    Dark(R.string.theme_dark)
}

internal enum class LanguageChoice(val tag: String, val label: Int) {
    System("", R.string.follow_system),
    English("en", R.string.language_english),
    Portuguese("pt-BR", R.string.language_portuguese)
}

internal enum class WeekStart(val label: Int) {
    Monday(R.string.week_monday),
    Sunday(R.string.week_sunday),
    Saturday(R.string.week_saturday)
}

internal class AppPreferences(private val context: Context) {
    private val storage = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var theme: ThemeChoice
        get() = ThemeChoice.entries.firstOrNull { it.name == storage.getString("theme", null) }
            ?: ThemeChoice.Dark
        set(value) { storage.edit().putString("theme", value.name).apply() }

    var notificationsEnabled: Boolean
        get() = storage.getBoolean("notifications_enabled", false)
        set(value) { storage.edit().putBoolean("notifications_enabled", value).apply() }

    var weekStart: WeekStart
        get() = WeekStart.entries.firstOrNull { it.name == storage.getString("week_start", null) }
            ?: WeekStart.Monday
        set(value) { storage.edit().putString("week_start", value.name).apply() }

    var defaultTaskPriority: ProjectPriority
        get() = ProjectPriority.entries.firstOrNull { it.key == storage.getString("default_task_priority", null) }
            ?: ProjectPriority.Medium
        set(value) { storage.edit().putString("default_task_priority", value.key).apply() }

    var language: LanguageChoice
        get() {
            val tag = if (Build.VERSION.SDK_INT >= 33) {
                context.getSystemService(LocaleManager::class.java).applicationLocales[0]?.toLanguageTag()
            } else {
                storage.getString("language", null)
            }
            return LanguageChoice.entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
                ?: LanguageChoice.System
        }
        set(value) {
            if (Build.VERSION.SDK_INT >= 33) {
                context.getSystemService(LocaleManager::class.java).applicationLocales =
                    LocaleList.forLanguageTags(value.tag)
            } else {
                storage.edit().putString("language", value.tag).apply()
            }
        }

    fun localizedContext(): Context {
        if (Build.VERSION.SDK_INT >= 33 || language == LanguageChoice.System) return context
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language.tag))
        return context.createConfigurationContext(configuration)
    }
}
