package com.example

import android.app.Application
import com.example.data.MusicRepository
import com.example.service.AudioPlayerController
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guard against accidentally reintroducing non-injected per-screen dependencies. */
class MainViewModelInjectionContractTest {
    @Test
    fun mainViewModelUsesHiltAndConstructorInjectedDependencies() {
        val parameters = arrayOf(
            Application::class.java,
            MusicRepository::class.java,
            AudioPlayerController::class.java
        )
        assertTrue(MainViewModel::class.java.declaredConstructors.any {
            it.parameterTypes.contentEquals(parameters)
        })
        // HiltViewModel's retention is not necessarily RUNTIME; rely on Hilt's
        // compile-time processing and guard the runtime injectable signature.
        assertFalse(MainViewModel::class.java.declaredConstructors.any {
            it.parameterTypes.contentEquals(arrayOf(Application::class.java))
        })
    }
}
