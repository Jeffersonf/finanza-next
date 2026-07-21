package com.finanza.next

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finanza.next.ui.theme.AppExperience
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemePreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun themePreferenceCanSwitchWithoutChangingTheUserSetting() {
        val preferences = context.getSharedPreferences(
            "finanza_theme_preference_test",
            Context.MODE_PRIVATE,
        )
        preferences.edit().clear().commit()
        try {
            preferences.edit().putString("theme_mode", "dark").commit()
            assertEquals("dark", preferences.getString("theme_mode", ""))
            preferences.edit().putString("theme_mode", "light").commit()
            assertEquals("light", preferences.getString("theme_mode", ""))
        } finally {
            preferences.edit().clear().commit()
        }
    }

    @Test
    fun visualExperienceCanSwitchWithoutChangingThemeMode() {
        val preferences = context.getSharedPreferences(
            "finanza_theme_preference_test",
            Context.MODE_PRIVATE,
        )
        preferences.edit().clear().commit()
        try {
            preferences.edit()
                .putString("theme_mode", "dark")
                .putString("visual_experience", AppExperience.FINANZA.id)
                .commit()

            assertEquals("dark", preferences.getString("theme_mode", ""))
            assertEquals(AppExperience.FINANZA.id, preferences.getString("visual_experience", ""))

            preferences.edit().putString("visual_experience", AppExperience.NEXT.id).commit()

            assertEquals("dark", preferences.getString("theme_mode", ""))
            assertEquals(AppExperience.NEXT.id, preferences.getString("visual_experience", ""))
        } finally {
            preferences.edit().clear().commit()
        }
    }
}
