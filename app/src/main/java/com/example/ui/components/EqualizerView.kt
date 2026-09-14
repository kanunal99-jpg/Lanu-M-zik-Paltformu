package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EqualizerPreset
import com.example.model.EqualizerState
import com.example.ui.theme.LanuCyan
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkBorder
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuDarkSurfaceElevated
import com.example.ui.theme.LanuEmerald
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuPurple
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.LanuTextPrimary
import com.example.ui.theme.LanuTextSecondary

@Composable
fun EqualizerView(
    equalizerState: EqualizerState,
    isPlaying: Boolean,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onBandLevelChanged: (Int, Float) -> Unit,
    onBassBoostChanged: (Float) -> Unit,
    onVirtualizerChanged: (Float) -> Unit,
    onToggleEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Real-time animated audio visualizer wave effect
    val infiniteTransition = rememberInfiniteTransition(label = "eq_visualizer")
    val waveAnim1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w1"
    )
    val waveAnim2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w2"
    )
    val waveAnim3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w3"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LanuDarkBg)
            .verticalScroll(scrollState)
            .padding(bottom = 100.dp)
            .testTag("equalizer_screen")
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LanuPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = LanuPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Gelişmiş Ekolayzır",
                        color = LanuTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Stüdyo Kalitesinde Ses Özelleştirme",
                        color = LanuTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Switch(
                checked = equalizerState.isEnabled,
                onCheckedChange = { onToggleEqualizer() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LanuGreen,
                    checkedTrackColor = LanuGreen.copy(alpha = 0.3f),
                    uncheckedThumbColor = LanuTextMuted,
                    uncheckedTrackColor = LanuDarkSurfaceElevated
                ),
                modifier = Modifier.testTag("equalizer_master_switch")
            )
        }

        // Live Dynamic Frequency Wave Visualizer Canvas
        Card(
            colors = CardDefaults.cardColors(containerColor = LanuDarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .border(1.dp, LanuDarkBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CANLI FREKANS SPEKTRUMU",
                        color = LanuTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isPlaying && equalizerState.isEnabled) "AKTİF ÇIKIŞ" else "HAZIRDA",
                        color = if (isPlaying && equalizerState.isEnabled) LanuGreen else LanuTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                ) {
                    val barCount = 28
                    val spacing = 6.dp.toPx()
                    val totalSpacing = spacing * (barCount - 1)
                    val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(2f)

                    val heights = listOf(
                        waveAnim1, waveAnim2, waveAnim3, waveAnim1 * 0.9f, waveAnim2 * 1.1f,
                        waveAnim3 * 0.8f, waveAnim1 * 1.2f, waveAnim2 * 0.7f, waveAnim3 * 1.05f
                    )

                    for (i in 0 until barCount) {
                        val factor = if (isPlaying && equalizerState.isEnabled) {
                            val h = heights[i % heights.size].coerceIn(0.1f, 1.0f)
                            // boost by bass on left bars
                            if (i < 8) h * (1f + equalizerState.bassBoostPercent * 0.6f) else h
                        } else 0.12f

                        val barHeight = (size.height * factor).coerceIn(4f, size.height)
                        val x = i * (barWidth + spacing)
                        val y = size.height - barHeight

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(LanuCyan, LanuGreen, LanuPurple)
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }
        }

        // Presets Selector (Horizontally Scrollable Chips)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Ses Ön Ayarları (Presets)",
            color = LanuTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            EqualizerPreset.values().forEach { preset ->
                val isSelected = equalizerState.activePreset == preset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) LanuGreen else LanuDarkSurfaceElevated
                        )
                        .border(
                            1.dp,
                            if (isSelected) LanuGreen else LanuDarkBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable(enabled = equalizerState.isEnabled) {
                            onPresetSelected(preset)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("preset_${preset.name.lowercase()}")
                ) {
                    Text(
                        text = preset.displayName,
                        color = if (isSelected) Color.Black else LanuTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // 5-Band Manual Frequency Sliders
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = LanuDarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .border(1.dp, LanuDarkBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "5-BANT MANUEL EKOLAYZIR",
                        color = LanuTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "-12 dB  /  +12 dB",
                        color = LanuTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                equalizerState.bands.forEach { band ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = band.frequencyLabel,
                                color = LanuTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${if (band.levelDb > 0) "+" else ""}${String.format("%.1f", band.levelDb)} dB",
                                color = if (band.levelDb != 0f) LanuGreen else LanuTextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = band.levelDb,
                            onValueChange = { onBandLevelChanged(band.index, it) },
                            valueRange = -12.0f..12.0f,
                            enabled = equalizerState.isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = LanuGreen,
                                activeTrackColor = LanuGreen,
                                inactiveTrackColor = LanuDarkSurfaceElevated
                            ),
                            modifier = Modifier.testTag("slider_band_${band.index}")
                        )
                    }
                }
            }
        }

        // Bass Boost & 3D Virtualizer Dials/Sliders
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Bass Boost Card
            Card(
                colors = CardDefaults.cardColors(containerColor = LanuDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, LanuDarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Derin Bas",
                            color = LanuTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "%${(equalizerState.bassBoostPercent * 100).toInt()}",
                            color = LanuPurple,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Bass Boost Gücü",
                        color = LanuTextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = equalizerState.bassBoostPercent,
                        onValueChange = { onBassBoostChanged(it) },
                        enabled = equalizerState.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = LanuPurple,
                            activeTrackColor = LanuPurple,
                            inactiveTrackColor = LanuDarkSurfaceElevated
                        ),
                        modifier = Modifier.testTag("slider_bass_boost")
                    )
                }
            }

            // 3D Virtualizer Card
            Card(
                colors = CardDefaults.cardColors(containerColor = LanuDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, LanuDarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "3D Çevreleyen",
                            color = LanuTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "%${(equalizerState.virtualizerPercent * 100).toInt()}",
                            color = LanuCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Sanal Surround",
                        color = LanuTextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = equalizerState.virtualizerPercent,
                        onValueChange = { onVirtualizerChanged(it) },
                        enabled = equalizerState.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = LanuCyan,
                            activeTrackColor = LanuCyan,
                            inactiveTrackColor = LanuDarkSurfaceElevated
                        ),
                        modifier = Modifier.testTag("slider_virtualizer")
                    )
                }
            }
        }
    }
}
