package com.filippi.devtrack

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.filippi.devtrack.ui.AppPreferences
import com.filippi.devtrack.ui.LanguageChoice
import com.filippi.devtrack.ui.ThemeChoice
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsTest {
    @Test fun appearanceAndLanguageSurviveActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var originalTheme: ThemeChoice
            lateinit var originalLanguage: LanguageChoice
            scenario.onActivity { activity ->
                val preferences = AppPreferences(activity)
                originalTheme = preferences.theme
                originalLanguage = preferences.language
            }
            try {
                scenario.onActivity { activity ->
                    AppPreferences(activity).apply {
                        theme = ThemeChoice.Light
                        language = LanguageChoice.Portuguese
                    }
                }
                scenario.recreate()
                scenario.onActivity { activity ->
                    val preferences = AppPreferences(activity)
                    assertEquals(ThemeChoice.Light, preferences.theme)
                    assertEquals(LanguageChoice.Portuguese, preferences.language)
                    assertEquals("Configurações", activity.getString(R.string.settings))
                }
            } finally {
                scenario.onActivity { activity ->
                    AppPreferences(activity).apply {
                        theme = originalTheme
                        language = originalLanguage
                    }
                }
            }
        }
    }
}
