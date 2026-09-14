package com.example.model

enum class EqualizerPreset(val displayName: String) {
    NORMAL("Normal"),
    POP("Pop"),
    ROCK("Rock"),
    JAZZ("Jazz"),
    HIPHOP("Hip-Hop"),
    ELECTRONIC("Elektronik"),
    VOCAL_BOOST("Vokal Güçlendirici"),
    BASS_BOOST("Derin Bas"),
    ACOUSTIC("Akustik"),
    CLASSICAL("Klasik"),
    CUSTOM("Özel Ayar");

    val title: String get() = displayName
}

data class FrequencyBand(
    val index: Int,
    val frequencyLabel: String,
    val levelDb: Float // range -12.0f to +12.0f
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val activePreset: EqualizerPreset = EqualizerPreset.POP,
    val bands: List<FrequencyBand> = listOf(
        FrequencyBand(0, "60 Hz", 3.0f),
        FrequencyBand(1, "230 Hz", 2.0f),
        FrequencyBand(2, "910 Hz", 0.0f),
        FrequencyBand(3, "3.6 kHz", 2.5f),
        FrequencyBand(4, "14 kHz", 4.0f)
    ),
    val bassBoostPercent: Float = 0.55f, // 0.0 to 1.0
    val virtualizerPercent: Float = 0.40f, // 0.0 to 1.0
    val loudnessGainPercent: Float = 0.60f // 0.0 to 1.0
) {
    companion object {
        fun getPresetBands(preset: EqualizerPreset): List<FrequencyBand> {
            val levels = when (preset) {
                EqualizerPreset.NORMAL -> listOf(0f, 0f, 0f, 0f, 0f)
                EqualizerPreset.POP -> listOf(2.5f, 1.5f, 0f, 2.0f, 3.5f)
                EqualizerPreset.ROCK -> listOf(5.0f, 3.0f, -1.0f, 3.0f, 5.0f)
                EqualizerPreset.JAZZ -> listOf(3.0f, 2.0f, 1.0f, 2.0f, 3.0f)
                EqualizerPreset.HIPHOP -> listOf(6.0f, 4.5f, 1.0f, 2.0f, 3.5f)
                EqualizerPreset.ELECTRONIC -> listOf(5.5f, 3.5f, 0.5f, 3.5f, 4.5f)
                EqualizerPreset.VOCAL_BOOST -> listOf(-1.0f, 1.0f, 4.5f, 3.5f, 1.0f)
                EqualizerPreset.BASS_BOOST -> listOf(8.0f, 6.0f, 2.0f, 0.0f, 0.0f)
                EqualizerPreset.ACOUSTIC -> listOf(3.5f, 2.5f, 2.0f, 3.0f, 4.0f)
                EqualizerPreset.CLASSICAL -> listOf(4.0f, 2.5f, 0.0f, 2.5f, 3.5f)
                EqualizerPreset.CUSTOM -> listOf(3.0f, 2.0f, 0.0f, 2.5f, 4.0f)
            }
            val labels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
            return levels.mapIndexed { idx, lvl -> FrequencyBand(idx, labels[idx], lvl) }
        }
    }
}

data class EqualizerSettings(
    val isEnabled: Boolean = true,
    val preset: EqualizerPreset = EqualizerPreset.POP,
    val band60Hz: Float = 3.0f,
    val band230Hz: Float = 2.0f,
    val band910Hz: Float = 0.0f,
    val band3600Hz: Float = 2.5f,
    val band14000Hz: Float = 4.0f,
    val bassBoostPercent: Float = 55f,
    val virtualizerPercent: Float = 40f
)

fun EqualizerState.toSettings(): EqualizerSettings {
    val b0 = bands.getOrNull(0)?.levelDb ?: 0f
    val b1 = bands.getOrNull(1)?.levelDb ?: 0f
    val b2 = bands.getOrNull(2)?.levelDb ?: 0f
    val b3 = bands.getOrNull(3)?.levelDb ?: 0f
    val b4 = bands.getOrNull(4)?.levelDb ?: 0f
    return EqualizerSettings(
        isEnabled = isEnabled,
        preset = activePreset,
        band60Hz = b0,
        band230Hz = b1,
        band910Hz = b2,
        band3600Hz = b3,
        band14000Hz = b4,
        bassBoostPercent = bassBoostPercent * 100f,
        virtualizerPercent = virtualizerPercent * 100f
    )
}
