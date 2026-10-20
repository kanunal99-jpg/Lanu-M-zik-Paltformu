package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NavigationLocalizationTest {
    private fun localized(locale: Locale, id: Int): String {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config).getString(id)
    }

    @Test
    fun bottomNavigationHasRealTurkishAndEnglishResources() {
        val ids = listOf(R.string.nav_home, R.string.nav_search, R.string.nav_library,
            R.string.nav_account, R.string.nav_updates)
        val english = listOf("Home", "Explore", "My Library", "Account", "Check for Updates")
        val turkish = listOf("Ana Sayfa", "Keşfet", "Kitaplığım", "Hesabım", "Güncelleme Kontrol")
        ids.indices.forEach { i ->
            assertEquals(english[i], localized(Locale.ENGLISH, ids[i]))
            assertEquals(turkish[i], localized(Locale.forLanguageTag("tr-TR"), ids[i]))
        }
    }
}
