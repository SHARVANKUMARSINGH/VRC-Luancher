/*
 * Vrc Luancher - Authentic MCPE Square Joystick Component for Jetpack Compose
 * 100% Jetpack Compose implementation with hardware-accelerated Canvas rendering,
 * strict square track boundaries, 3D pixel shading highlights, and direct LWJGL game pairing.
 */

package com.movtery.zalithlauncher.ui.control.joystick

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.lwjgl.glfw.CallbackBridge
import kotlin.math.max
import kotlin.math.min

/**
 * Authentic New MCPE Bedrock Square Joystick built entirely in Jetpack Compose.
 *
 * Visual specification:
 * - Outer base: 136dp (default), rgba(210, 210, 210, 0.25)
 * - 4dp solid #1c1c1c border with 8dp rounded corners
 * - Inset 3dp highlight line rgba(255, 255, 255, 0.15)
 * - Inner moving knob: 64dp (default), rgba(110, 110, 110, 0.85) (active: rgba(140, 140, 140, 0.95))
 * - Inset 3D pixel shading highlights with active depression response
 * - Clamps strictly to square box track boundaries: [-maxRange, maxRange]
 * - Direct game pairing via LWJGL CallbackBridge
 */
@Composable
fun McpeSquareJoystickCompose(
    modifier: Modifier = Modifier,
    touchZoneSize: Dp = 170.dp,
    baseSize: Dp = 136.dp,
    knobSize: Dp = 64.dp,
    enableGamePairing: Boolean = false,
    enableSprintLock: Boolean = true,
    onMove: ((outputX: Float, outputY: Float) -> Unit)? = null
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val basePx = with(density) { baseSize.toPx() }
    val knobPx = with(density) { knobSize.toPx() }
    val maxRange = (basePx - knobPx) / 2f

    val borderWidth = with(density) { 4.dp.toPx() }
    val baseCornerRadius = with(density) { 8.dp.toPx() }
    val knobCornerRadius = with(density) { 10.dp.toPx() }
    val innerHighlightWidth = with(density) { 3.dp.toPx() }
    val shadingWidth = with(density) { 4.dp.toPx() }

    val knobAnimOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var isTracking by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }

    var currentOutputX by remember { mutableFloatStateOf(0f) }
    var currentOutputY by remember { mutableFloatStateOf(0f) }

    // LWJGL Game pairing state tracking
    var activeForward by remember { mutableStateOf(false) }
    var activeBackward by remember { mutableStateOf(false) }
    var activeLeft by remember { mutableStateOf(false) }
    var activeRight by remember { mutableStateOf(false) }
    var activeSprint by remember { mutableStateOf(false) }

    fun updateGameKeys(x: Float, y: Float, locked: Boolean) {
        if (!enableGamePairing) return
        val threshold = 0.28f
        val needForward = y > threshold
        val needBackward = y < -threshold
        val needLeft = x < -threshold
        val needRight = x > threshold
        val needSprint = locked || (enableSprintLock && y >= 0.88f)

        try {
            val mods = CallbackBridge.getCurrentMods()
            if (needForward != activeForward) {
                activeForward = needForward
                CallbackBridge.sendKeyPress(87, mods, needForward) // GLFW_KEY_W
            }
            if (needBackward != activeBackward) {
                activeBackward = needBackward
                CallbackBridge.sendKeyPress(83, mods, needBackward) // GLFW_KEY_S
            }
            if (needLeft != activeLeft) {
                activeLeft = needLeft
                CallbackBridge.sendKeyPress(65, mods, needLeft) // GLFW_KEY_A
            }
            if (needRight != activeRight) {
                activeRight = needRight
                CallbackBridge.sendKeyPress(68, mods, needRight) // GLFW_KEY_D
            }
            if (needSprint != activeSprint) {
                activeSprint = needSprint
                CallbackBridge.sendKeyPress(341, mods, needSprint) // GLFW_KEY_LEFT_CONTROL
            }
        } catch (ignored: Throwable) {}
    }

    fun releaseAllKeys() {
        if (!enableGamePairing) return
        try {
            val mods = CallbackBridge.getCurrentMods()
            if (activeForward) { CallbackBridge.sendKeyPress(87, mods, false); activeForward = false }
            if (activeBackward) { CallbackBridge.sendKeyPress(83, mods, false); activeBackward = false }
            if (activeLeft) { CallbackBridge.sendKeyPress(65, mods, false); activeLeft = false }
            if (activeRight) { CallbackBridge.sendKeyPress(68, mods, false); activeRight = false }
            if (activeSprint) { CallbackBridge.sendKeyPress(341, mods, false); activeSprint = false }
        } catch (ignored: Throwable) {}
    }

    DisposableEffect(Unit) {
        onDispose {
            releaseAllKeys()
        }
    }

    Box(
        modifier = modifier
            .size(touchZoneSize)
            .pointerInput(maxRange) {
                val center = Offset(size.width / 2f, size.height / 2f)

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue

                        if (change.pressed) {
                            change.consume()
                            isTracking = true

                            val touchPos = change.position
                            val deltaX = touchPos.x - center.x
                            val deltaY = touchPos.y - center.y

                            // Clamping rules to match square box track boundaries perfectly
                            val finalX = max(-maxRange, min(maxRange, deltaX))
                            val finalY = max(-maxRange, min(maxRange, deltaY))

                            val clampedOffset = Offset(finalX, finalY)
                            coroutineScope.launch { knobAnimOffset.snapTo(clampedOffset) }

                            // Universal game engine metrics [-1.00 to 1.00]
                            val outX = (finalX / maxRange).coerceIn(-1.0f, 1.0f)
                            val outY = (-(finalY / maxRange)).coerceIn(-1.0f, 1.0f)
                            currentOutputX = outX
                            currentOutputY = outY

                            onMove?.invoke(outX, outY)
                            updateGameKeys(outX, outY, isLocked)
                        } else {
                            // Touch released
                            if (isTracking) {
                                isTracking = false
                                change.consume()

                                if (enableSprintLock && currentOutputY >= 0.92f) {
                                    isLocked = true
                                    val lockOffset = Offset(0f, -maxRange)
                                    coroutineScope.launch {
                                        knobAnimOffset.animateTo(lockOffset, spring(dampingRatio = 0.7f, stiffness = 600f))
                                    }
                                    currentOutputX = 0f
                                    currentOutputY = 1.0f
                                    onMove?.invoke(0f, 1.0f)
                                    updateGameKeys(0f, 1.0f, true)
                                } else {
                                    isLocked = false
                                    coroutineScope.launch {
                                        knobAnimOffset.animateTo(Offset.Zero, spring(dampingRatio = 0.65f, stiffness = 500f))
                                    }
                                    currentOutputX = 0f
                                    currentOutputY = 0f
                                    onMove?.invoke(0f, 0f)
                                    releaseAllKeys()
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(baseSize)) {
            val canvasSize = this.size
            val bgCenter = Offset(canvasSize.width / 2f, canvasSize.height / 2f)

            val baseTopLeft = Offset(bgCenter.x - basePx / 2f, bgCenter.y - basePx / 2f)
            val baseCorner = CornerRadius(baseCornerRadius, baseCornerRadius)
            val knobCorner = CornerRadius(knobCornerRadius, knobCornerRadius)

            // 1. Outer Square Base Fill: rgba(210, 210, 210, 0.25)
            drawRoundRect(
                color = Color(0x40D2D2D2),
                topLeft = baseTopLeft,
                size = Size(basePx, basePx),
                cornerRadius = baseCorner
            )

            // 2. Base Border: 4px solid #1c1c1c
            drawRoundRect(
                color = Color(0xFF1C1C1C),
                topLeft = Offset(baseTopLeft.x + borderWidth / 2f, baseTopLeft.y + borderWidth / 2f),
                size = Size(basePx - borderWidth, basePx - borderWidth),
                cornerRadius = baseCorner,
                style = Stroke(width = borderWidth)
            )

            // 3. Base Inner Highlight line: inset 0 0 0 3px rgba(255, 255, 255, 0.15)
            val highlightInset = borderWidth + innerHighlightWidth / 2f
            drawRoundRect(
                color = Color(0x26FFFFFF),
                topLeft = Offset(baseTopLeft.x + highlightInset, baseTopLeft.y + highlightInset),
                size = Size(basePx - highlightInset * 2f, basePx - highlightInset * 2f),
                cornerRadius = CornerRadius(max(1f, baseCornerRadius - highlightInset), max(1f, baseCornerRadius - highlightInset)),
                style = Stroke(width = innerHighlightWidth)
            )

            // 4. Moving Square Knob
            val currentKnobOffset = knobAnimOffset.value
            val knobCenterX = bgCenter.x + currentKnobOffset.x
            val knobCenterY = bgCenter.y + currentKnobOffset.y

            val knobTopLeft = Offset(knobCenterX - knobPx / 2f, knobCenterY - knobPx / 2f)
            val knobDimensions = Size(knobPx, knobPx)

            // Knob Surface Fill: normal rgba(110, 110, 110, 0.85) / active rgba(140, 140, 140, 0.95)
            val knobColor = if (isTracking) Color(0xF28C8C8C) else Color(0xD96E6E6E)
            drawRoundRect(
                color = knobColor,
                topLeft = knobTopLeft,
                size = knobDimensions,
                cornerRadius = knobCorner
            )

            // 3D Pixel Shading highlights
            val insetShade = borderWidth + shadingWidth / 2f
            val innerLeft = knobTopLeft.x + insetShade
            val innerTop = knobTopLeft.y + insetShade
            val innerRight = knobTopLeft.x + knobPx - insetShade
            val innerBottom = knobTopLeft.y + knobPx - insetShade

            val topShadeColor = if (isTracking) Color(0x66000000) else Color(0x33FFFFFF)
            val bottomShadeColor = if (isTracking) Color(0x1AFFFFFF) else Color(0x59000000)

            // Top-Left bevel path
            val pathTopLeft = Path().apply {
                moveTo(innerLeft, innerBottom)
                lineTo(innerLeft, innerTop)
                lineTo(innerRight, innerTop)
            }
            drawPath(pathTopLeft, color = topShadeColor, style = Stroke(width = shadingWidth))

            // Bottom-Right bevel path
            val pathBottomRight = Path().apply {
                moveTo(innerRight, innerTop)
                lineTo(innerRight, innerBottom)
                lineTo(innerLeft, innerBottom)
            }
            drawPath(pathBottomRight, color = bottomShadeColor, style = Stroke(width = shadingWidth))

            // Knob Dark Border: 4px solid #1c1c1c
            drawRoundRect(
                color = Color(0xFF1C1C1C),
                topLeft = Offset(knobTopLeft.x + borderWidth / 2f, knobTopLeft.y + borderWidth / 2f),
                size = Size(knobPx - borderWidth, knobPx - borderWidth),
                cornerRadius = knobCorner,
                style = Stroke(width = borderWidth)
            )

            // Auto-walk / sprint lock indicator mark
            if (isLocked) {
                drawCircle(
                    color = Color(0xFFFFA000),
                    center = Offset(knobCenterX, knobTopLeft.y + 8f * density.density),
                    radius = 3.5f * density.density
                )
            }
        }
    }
}
