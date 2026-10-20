/*
 * Vrc Luancher - 100% Jetpack Compose Pre-Settings & Hardware Setup Dialog
 * Google Apps Design & Material 3 Styling (No XML layouts)
 */

package com.movtery.zalithlauncher.ui.control.setup

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.setup.AutoSetupManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.control.joystick.McpeSquareJoystickCompose
import java.util.Locale

// Google Apps Design Color Palette
private val GoogleBlue = Color(0xFF8AB4F8)
private val GoogleBlueDark = Color(0xFF174EA6)
private val GoogleGreen = Color(0xFF81C995)
private val GoogleYellow = Color(0xFFFDD663)
private val GoogleRed = Color(0xFFF28B82)
private val GoogleSurfaceDark = Color(0xFF1F1F1F)
private val GoogleCardBg = Color(0xFF28292A)
private val GoogleOutline = Color(0xFF3C4043)

/**
 * 100% Jetpack Compose Hardware Pre-Settings & OpenGL Setup Dialog.
 * Implements modern Google Apps Design with Material 3 components.
 */
@Composable
fun SetupPreSettingsDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val totalRam = remember { AutoSetupManager.getTotalPhysicalRamMb(context) }
    val gpuVendor = remember { AutoSetupManager.detectGpuSocVendor() }
    val hasVulkan = remember { AutoSetupManager.isVulkanSupported(context) }

    var allocatedRam by remember {
        mutableFloatStateOf(
            (AllSettings.ramAllocation.getValue().takeIf { it > 0 }
                ?: AutoSetupManager.computeOptimalRamMb(totalRam)).toFloat()
        )
    }

    Renderers.init()
    val availableRenderers = remember { Renderers.getRenderers() }
    var selectedRendererId by remember { mutableStateOf(AllSettings.renderer.getValue()) }
    var isRendererDropdownOpen by remember { mutableStateOf(false) }

    var selectedPreset by remember { mutableStateOf(AutoSetupManager.PerformancePreset.NATIVE_SPEED) }
    var enableMesaGlThread by remember { mutableStateOf(true) }

    var testJoystickX by remember { mutableFloatStateOf(0f) }
    var testJoystickY by remember { mutableFloatStateOf(0f) }

    val currentRendererName = remember(selectedRendererId) {
        availableRenderers.find { it.getUniqueIdentifier() == selectedRendererId }?.getRendererName()
            ?: if (selectedRendererId.isNotBlank()) selectedRendererId else "Auto (Best)"
    }

    val maxAllowedRam = remember { (totalRam * 0.85f).coerceAtLeast(2048f) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, GoogleOutline, RoundedCornerShape(24.dp)),
            color = GoogleSurfaceDark,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header: Google Apps Style Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoogleBlueDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_filled),
                            contentDescription = "Settings Gear",
                            tint = GoogleBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pre-Settings & Native Speed",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vrc Luancher Hardware Engine",
                            color = GoogleBlue,
                            fontSize = 12.5.sp
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = Color(0xFFE3E3E3),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card 1: Hardware Intelligence & Auto-Detect (Google Blue)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = GoogleCardBg),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoogleOutline))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "HARDWARE INTELLIGENCE",
                            color = GoogleBlue,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• GPU Architecture: $gpuVendor\n• Vulkan Acceleration: ${if (hasVulkan) "Supported (Vulkan 1.2+)" else "Not Detected / ES Only"}\n• Total Physical RAM: $totalRam MB",
                            color = Color(0xFFE3E3E3),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val result = AutoSetupManager.detectAndApplyBestSetup(context)
                                allocatedRam = result.recommendedRamMb.toFloat()
                                selectedRendererId = result.recommendedRenderer
                                Toast.makeText(
                                    context,
                                    "⚡ Optimized: ${result.recommendedRendererName} (${result.recommendedRamMb} MB RAM)",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoogleBlue,
                                contentColor = Color(0xFF041E49)
                            )
                        ) {
                            Text(
                                text = "⚡ AUTO-DETECT BEST OPENGL & SETUP",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card 2: Performance Presets (Google Green)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = GoogleCardBg),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoogleOutline))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PERFORMANCE PRESETS",
                            color = GoogleGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Preset 1: Native Speed
                            PresetChip(
                                title = "Native Speed",
                                isSelected = selectedPreset == AutoSetupManager.PerformancePreset.NATIVE_SPEED,
                                accentColor = GoogleBlue,
                                modifier = Modifier.weight(1f)
                            ) {
                                selectedPreset = AutoSetupManager.PerformancePreset.NATIVE_SPEED
                                AutoSetupManager.applyPreset(AutoSetupManager.PerformancePreset.NATIVE_SPEED, context)
                                allocatedRam = AutoSetupManager.computeOptimalRamMb(totalRam).toFloat()
                                selectedRendererId = AutoSetupManager.detectBestRenderer(context)
                                Toast.makeText(context, "Applied Native Speed Preset!", Toast.LENGTH_SHORT).show()
                            }

                            // Preset 2: Balanced
                            PresetChip(
                                title = "Balanced",
                                isSelected = selectedPreset == AutoSetupManager.PerformancePreset.BALANCED,
                                accentColor = GoogleGreen,
                                modifier = Modifier.weight(1f)
                            ) {
                                selectedPreset = AutoSetupManager.PerformancePreset.BALANCED
                                AutoSetupManager.applyPreset(AutoSetupManager.PerformancePreset.BALANCED, context)
                                allocatedRam = minOf(totalRam * 0.5f, 3072f)
                                Toast.makeText(context, "Applied Balanced Preset!", Toast.LENGTH_SHORT).show()
                            }

                            // Preset 3: Power Saver
                            PresetChip(
                                title = "Saver",
                                isSelected = selectedPreset == AutoSetupManager.PerformancePreset.POWER_SAVER,
                                accentColor = GoogleYellow,
                                modifier = Modifier.weight(1f)
                            ) {
                                selectedPreset = AutoSetupManager.PerformancePreset.POWER_SAVER
                                AutoSetupManager.applyPreset(AutoSetupManager.PerformancePreset.POWER_SAVER, context)
                                allocatedRam = minOf(totalRam * 0.35f, 1536f)
                                Toast.makeText(context, "Applied Power Saver Preset!", Toast.LENGTH_SHORT).show()
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedPreset.description,
                            color = Color(0xFFBDC1C6),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card 3: OpenGL & Graphics Backend (Google Yellow)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = GoogleCardBg),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoogleOutline))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "OPENGL RENDERER BACKEND",
                            color = GoogleYellow,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Material 3 Renderer Selector Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF333537))
                                .clickable { isRendererDropdownOpen = true }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = currentRendererName,
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                                    contentDescription = null,
                                    tint = GoogleBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isRendererDropdownOpen,
                                onDismissRequest = { isRendererDropdownOpen = false },
                                modifier = Modifier.background(Color(0xFF28292A))
                            ) {
                                availableRenderers.forEach { renderer ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${renderer.getRendererName()} (${renderer.getRendererId()})",
                                                color = if (renderer.getUniqueIdentifier() == selectedRendererId) GoogleBlue else Color.White,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            selectedRendererId = renderer.getUniqueIdentifier()
                                            AllSettings.renderer.save(selectedRendererId)
                                            try {
                                                Renderers.setCurrentRenderer(selectedRendererId)
                                            } catch (ignored: Throwable) {}
                                            isRendererDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Multi-threaded Mesa OpenGL (mesa_glthread)",
                                    color = Color(0xFFE3E3E3),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Offloads GL rendering calls to worker thread for native speed.",
                                    color = Color(0xFF9AA0A6),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = enableMesaGlThread,
                                onCheckedChange = { enableMesaGlThread = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GoogleBlue,
                                    checkedTrackColor = GoogleBlueDark
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card 4: Authentic Bedrock Touch Joystick Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = GoogleCardBg),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoogleOutline))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AUTHENTIC BEDROCK TOUCH JOYSTICK",
                                color = GoogleBlue,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = String.format(Locale.US, "X: %.2f | Y: %.2f", testJoystickX, testJoystickY),
                                color = GoogleBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Interactive 100% Jetpack Compose Square Joystick
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF202124)),
                            contentAlignment = Alignment.Center
                        ) {
                            McpeSquareJoystickCompose(
                                touchZoneSize = 150.dp,
                                baseSize = 120.dp,
                                knobSize = 56.dp,
                                enableGamePairing = false,
                                enableSprintLock = true,
                                onMove = { x, y ->
                                    testJoystickX = x
                                    testJoystickY = y
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card 5: Memory Allocation (Google Red)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = GoogleCardBg),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoogleOutline))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "JAVA HEAP ALLOCATION",
                                color = GoogleRed,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${allocatedRam.toInt()} MB",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = allocatedRam.coerceIn(512f, maxAllowedRam),
                            onValueChange = {
                                allocatedRam = it
                                AllSettings.ramAllocation.save(it.toInt())
                            },
                            valueRange = 512f..maxAllowedRam,
                            steps = ((maxAllowedRam - 512f) / 256f).toInt().coerceAtLeast(1),
                            colors = SliderDefaults.colors(
                                thumbColor = GoogleBlue,
                                activeTrackColor = GoogleBlue,
                                inactiveTrackColor = GoogleOutline
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Save & Apply Button
                Button(
                    onClick = {
                        AllSettings.ramAllocation.save(allocatedRam.toInt())
                        if (selectedRendererId.isNotBlank()) {
                            AllSettings.renderer.save(selectedRendererId)
                        }
                        Toast.makeText(context, "Settings Applied Successfully!", Toast.LENGTH_SHORT).show()
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoogleBlue,
                        contentColor = Color(0xFF041E49)
                    )
                ) {
                    Text(
                        text = "SAVE & APPLY SETTINGS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    title: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) GoogleBlueDark else Color(0xFF333537))
            .border(
                1.dp,
                if (isSelected) accentColor else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) accentColor else Color(0xFFE3E3E3),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
