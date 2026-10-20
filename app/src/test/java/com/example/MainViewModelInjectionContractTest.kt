package com.example

import android.app.Application
import com.example.data.MusicRepository
import com.example.service.AudioPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guard against accidentally reintroducing non-injected per-screen dependencies. */
class MainViewModelInjectionContractTest {
    @Test
    fun mainViewModelUsesHiltAndConstructorInjectedDependencies() {
        assertTrue(MainViewModel::class.java.isAnnotationPresent(HiltViewModel::class.java))
        val parameters = arrayOf(
            Application::class.java,
            MusicRepository::class.java,
            AudioPlayerController::class.java
        )
        assertTrue(MainViewModel::class.java.declaredConstructors.any {
            it.parameterTypes.contentEquals(parameters)
        })
    }
}
