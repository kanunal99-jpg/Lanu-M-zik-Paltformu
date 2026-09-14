package com.example.service

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.example.model.EqualizerPreset
import com.example.model.EqualizerState
import kotlin.math.abs

/**
 * Best-effort device DSP. Android audio effects are device-dependent, so every
 * effect is optional and failures fall back safely without breaking playback.
 */
class AudioEffectsController(private val audioSessionId: Int) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var supported = false

    init {
        if (audioSessionId > 0) {
            runCatching { equalizer = Equalizer(0, audioSessionId) }
            runCatching { bassBoost = BassBoost(0, audioSessionId) }
            runCatching { virtualizer = Virtualizer(0, audioSessionId) }
            supported = equalizer != null || bassBoost != null || virtualizer != null
        }
    }

    fun isSupported(): Boolean = supported

    fun apply(state: EqualizerState) {
        if (!supported) return
        runCatching {
            equalizer?.enabled = state.isEnabled
            bassBoost?.enabled = state.isEnabled
            virtualizer?.enabled = state.isEnabled

            applyBands(state)
            bassBoost?.setStrength((state.bassBoostPercent.coerceIn(0f, 1f) * 1000).toInt().toShort())
            virtualizer?.setStrength((state.virtualizerPercent.coerceIn(0f, 1f) * 1000).toInt().toShort())
        }
    }

    private fun applyBands(state: EqualizerState) {
        val eq = equalizer ?: return
        val range = eq.bandLevelRange
        val min = range[0].toInt()
        val max = range[1].toInt()
        val bandCount = eq.numberOfBands.toInt()
        if (bandCount <= 0) return

        val targetFrequencies = longArrayOf(60_000L, 230_000L, 910_000L, 3_600_000L, 14_000_000L)
        val available = ArrayList<Pair<Short, Long>>(bandCount)
        for (i in 0 until bandCount) {
            val band = i.toShort()
            val frequency = runCatching { eq.getCenterFreq(band).toLong() }.getOrDefault(0L)
            available += band to frequency
        }

        state.bands.forEachIndexed { index, bandState ->
            val target = targetFrequencies.getOrNull(index) ?: return@forEachIndexed
            val closest = available.minByOrNull { abs(it.second - target) }?.first ?: return@forEachIndexed
            val level = (bandState.levelDb * 100f).toInt().coerceIn(min, max).toShort()
            runCatching { eq.setBandLevel(closest, level) }
        }
    }

    fun applyPreset(preset: EqualizerPreset, state: EqualizerState): EqualizerState {
        val bands = EqualizerState.getPresetBands(preset)
        val next = state.copy(activePreset = preset, bands = bands)
        apply(next)
        return next
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        supported = false
    }
}
