package com.filippi.devtrack.ui

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.filippi.devtrack.R
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

internal class AppPreferences(private val context: Context) {
    private val storage = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var theme: ThemeChoice
        get() = ThemeChoice.entries.firstOrNull { it.name == storage.getString("theme", null) }
            ?: ThemeChoice.Dark
        set(value) { storage.edit().putString("theme", value.name).apply() }

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
