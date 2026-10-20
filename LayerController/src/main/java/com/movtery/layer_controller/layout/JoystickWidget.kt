/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.layer_controller.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.movtery.layer_controller.data.JoystickDirection
import com.movtery.layer_controller.event.EventHandler
import com.movtery.layer_controller.observable.DefaultObservableJoystickStyle
import com.movtery.layer_controller.observable.ObservableControlLayer
import com.movtery.layer_controller.observable.ObservableJoystickData
import com.movtery.layer_controller.observable.ObservableJoystickData.Companion.calculateDirection
import com.movtery.layer_controller.observable.ObservableJoystickData.Companion.clampToRegion
import com.movtery.layer_controller.observable.ObservableJoystickData.Companion.toRegion
import com.movtery.layer_controller.observable.ObservableJoystickStyle
import com.movtery.layer_controller.observable.ObservableWidget
import com.movtery.layer_controller.observable.PointerEventBus
import com.movtery.layer_controller.utils.buttonSize
import com.movtery.layer_controller.utils.editMode
import com.movtery.layer_controller.utils.snap.GuideLine
import com.movtery.layer_controller.utils.snap.SnapMode

/**
 * 摇杆控件渲染组件
 */
@Composable
internal fun JoystickWidgetRenderer(
    data: ObservableJoystickData,
    joystickStyles: List<ObservableJoystickStyle>,
    screenSize: IntSize,
    isDark: Boolean,
    visible: Boolean = true,
    pointerEventBus: PointerEventBus? = null,
    eventHandler: EventHandler? = null,
    reversedLayers: List<ObservableControlLayer>? = null,
    onOccupiedPointer: (PointerId) -> Unit = {},
    onReleasePointer: (PointerId) -> Unit = {},
    isEditMode: Boolean = false,
    enableSnap: Boolean = false,
    snapMode: SnapMode = SnapMode.Local,
    localSnapRange: Dp = 20.dp,
    getOtherWidgets: () -> List<ObservableWidget> = { emptyList() },
    snapThresholdValue: Dp = 4.dp,
    drawLine: (ObservableWidget, List<GuideLine>) -> Unit = { _, _ -> },
    onLineCancel: (ObservableWidget) -> Unit = {},
    onTapInEditMode: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    // 获取样式
    val joystickStyle = data.joystickStyleId?.let { id ->
        joystickStyles.find { it.uuid == id }
    }

    val themeConfig = if (joystickStyle?.commonStyle == true || !isDark) {
        joystickStyle?.lightStyle ?: DefaultObservableJoystickStyle.lightStyle
    } else {
        joystickStyle?.darkStyle ?: DefaultObservableJoystickStyle.darkStyle
    }

    //已经经过验证，如果使用Modifier.alpha设置不透明度，会导致摇杆强制裁切超出范围的内容
    //graphicsLayer(alpha = alpha, clip = false)也一样
    //这里暂时只能统一修改颜色的alpha
    val alpha = themeConfig.alpha
    val currentBackgroundColor = remember(themeConfig.backgroundColor, alpha) {
        themeConfig.backgroundColor.applyAlpha(alpha)
    }
    val currentJoystickColor = remember(themeConfig.joystickColor, alpha) {
        themeConfig.joystickColor.applyAlpha(alpha)
    }
    val currentJoystickCanLockColor = remember(themeConfig.joystickCanLockColor, alpha) {
        themeConfig.joystickCanLockColor.applyAlpha(alpha)
    }
    val currentJoystickLockedColor = remember(themeConfig.joystickLockedColor, alpha) {
        themeConfig.joystickLockedColor.applyAlpha(alpha)
    }
    val currentLockMarkColor = remember(themeConfig.lockMarkColor, alpha) {
        themeConfig.lockMarkColor.applyAlpha(alpha)
    }
    val currentBorderColor = remember(themeConfig.borderColor, alpha) {
        themeConfig.borderColor.applyAlpha(alpha)
    }

    // 形状
    val backgroundShape = remember(themeConfig.backgroundShape) {
        if (themeConfig.backgroundShape == 50) CircleShape
        else RoundedCornerShape(percent = themeConfig.backgroundShape)
    }
    val joystickShape = remember(themeConfig.joystickShape) {
        if (themeConfig.joystickShape == 50) CircleShape
        else RoundedCornerShape(percent = themeConfig.joystickShape)
    }

    // 边框宽度比例
    val borderWidthRatio = remember(themeConfig.borderWidthRatio) {
        (themeConfig.borderWidthRatio.toFloat() / 100f).coerceIn(0.0f, 0.5f)
    }

    // 摇杆头大小
    val joystickSizeRatio = remember(themeConfig.joystickSize) {
        themeConfig.joystickSize.coerceIn(0.0f, 1.0f)
    }

    //使用这个标记来判断是否渲染摇杆组件，未完全初始化时，可能导致组件闪烁
    var initialized by remember { mutableStateOf(false) }

    // 当大小变化时重新初始化
    var currentSize by remember { mutableStateOf(IntSize.Zero) }

    // 当形状变化时重新计算区域
    LaunchedEffect(backgroundShape) {
        if (visible && currentSize != IntSize.Zero) {
            val sizePx = Size(currentSize.width.toFloat(), currentSize.height.toFloat())
            data.backgroundRegion = backgroundShape.toRegion(
                size = sizePx,
                density = density,
                layoutDirection = layoutDirection
            )
        }
    }

    val joystickBaseResId = com.movtery.layer_controller.R.drawable.joystick_base
    val joystickKnobResId = com.movtery.layer_controller.R.drawable.joystick_handle

    if (visible) {
        Box(
            modifier = Modifier
                .onSizeChanged { size ->
                    currentSize = size
                    if (size != IntSize.Zero) {
                        // 计算并设置背景区域
                        val sizePx = Size(size.width.toFloat(), size.height.toFloat())
                        data.backgroundRegion = backgroundShape.toRegion(
                            size = sizePx,
                            density = density,
                            layoutDirection = layoutDirection
                        )
                        data.knobOffset = Offset.Zero
                        initialized = true
                    }
                }
                .buttonSize(data, screenSize)
                .let { modifier ->
                    if (isEditMode) {
                        modifier.editMode(
                            isEditMode = true,
                            data = data,
                            screenSize = screenSize,
                            enableSnap = enableSnap,
                            snapMode = snapMode,
                            localSnapRange = localSnapRange,
                            getOtherWidgets = getOtherWidgets,
                            snapThresholdValue = snapThresholdValue,
                            drawLine = drawLine,
                            onLineCancel = onLineCancel,
                            onTapInEditMode = onTapInEditMode ?: {}
                        )
                    } else if (pointerEventBus != null && eventHandler != null && reversedLayers != null) {
                        with(data) {
                            modifier.touchModifier(
                                pointerEventBus = pointerEventBus,
                                eventHandler = eventHandler,
                                allLayers = reversedLayers,
                                screenSize = screenSize,
                                onOccupiedPointer = onOccupiedPointer,
                                onReleasePointer = onReleasePointer
                            )
                        }
                    } else modifier
                },
            contentAlignment = Alignment.Center
        ) {
            // Render Authentic New MCPE Square Joystick
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (initialized) {
                    val minSide = minOf(size.width, size.height)
                    val bgCenter = Offset(size.width / 2f, size.height / 2f)
                    val densityVal = density.density

                    val isPressed = data.activePointer != null
                    val knobCenter = Offset(
                        bgCenter.x + data.knobOffset.x,
                        bgCenter.y + data.knobOffset.y
                    )

                    drawAuthenticMcpeSquareJoystick(
                        size = size,
                        bgCenter = bgCenter,
                        knobCenter = knobCenter,
                        knobRatio = joystickSizeRatio.takeIf { it > 0.1f } ?: (64f / 136f),
                        density = densityVal,
                        isPressed = isPressed,
                        isLocked = data.isLocked
                    )
                }
            }

            DisposableEffect(Unit) {
                data.onCompositionStart(eventHandler)
                onDispose {
                    data.onCompositionDispose(eventHandler)
                    data.activePointer?.let { pointerId ->
                        onReleasePointer(pointerId)
                        data.activePointer = null
                    }
                }
            }
        }
    } else {
        Spacer(
            modifier = Modifier.buttonSize(data, screenSize)
        )
    }
}

/**
 * 绘制 Authentic New MCPE Bedrock 方形摇杆组件
 */
private fun DrawScope.drawAuthenticMcpeSquareJoystick(
    size: Size,
    bgCenter: Offset,
    knobCenter: Offset,
    knobRatio: Float,
    density: Float,
    isPressed: Boolean,
    isLocked: Boolean
) {
    val minSide = minOf(size.width, size.height)
    val basePx = minSide * 0.95f
    val knobPx = basePx * knobRatio

    val borderWidth = 4f * density
    val baseCornerRadius = CornerRadius(8f * density, 8f * density)
    val knobCornerRadius = CornerRadius(10f * density, 10f * density)
    val innerHighlightWidth = 3f * density
    val shadingWidth = 4f * density

    val baseTopLeft = Offset(bgCenter.x - basePx / 2f, bgCenter.y - basePx / 2f)
    val baseSize = Size(basePx, basePx)

    // 1. Base Fill: rgba(210, 210, 210, 0.25)
    drawRoundRect(
        color = Color(0x40D2D2D2),
        topLeft = baseTopLeft,
        size = baseSize,
        cornerRadius = baseCornerRadius
    )

    // 2. Base Border: 4px solid #1c1c1c
    drawRoundRect(
        color = Color(0xFF1C1C1C),
        topLeft = Offset(baseTopLeft.x + borderWidth / 2f, baseTopLeft.y + borderWidth / 2f),
        size = Size(basePx - borderWidth, basePx - borderWidth),
        cornerRadius = baseCornerRadius,
        style = Stroke(width = borderWidth)
    )

    // 3. Base Inner Highlight: inset 3px rgba(255, 255, 255, 0.15)
    val highlightInset = borderWidth + innerHighlightWidth / 2f
    drawRoundRect(
        color = Color(0x26FFFFFF),
        topLeft = Offset(baseTopLeft.x + highlightInset, baseTopLeft.y + highlightInset),
        size = Size(basePx - highlightInset * 2f, basePx - highlightInset * 2f),
        cornerRadius = CornerRadius(maxOf(1f, 8f * density - highlightInset), maxOf(1f, 8f * density - highlightInset)),
        style = Stroke(width = innerHighlightWidth)
    )

    // 4. Moving Knob
    val knobTopLeft = Offset(knobCenter.x - knobPx / 2f, knobCenter.y - knobPx / 2f)
    val knobSize = Size(knobPx, knobPx)

    // Surface Fill: rgba(110, 110, 110, 0.85) (active: rgba(140, 140, 140, 0.95))
    val knobFillColor = if (isPressed) {
        Color(0xF28C8C8C)
    } else {
        Color(0xD96E6E6E)
    }
    drawRoundRect(
        color = knobFillColor,
        topLeft = knobTopLeft,
        size = knobSize,
        cornerRadius = knobCornerRadius
    )

    // 3D Pixel Shading on Knob:
    val insetShade = borderWidth + shadingWidth / 2f
    val innerLeft = knobTopLeft.x + insetShade
    val innerTop = knobTopLeft.y + insetShade
    val innerRight = knobTopLeft.x + knobPx - insetShade
    val innerBottom = knobTopLeft.y + knobPx - insetShade

    val topHighlightColor = if (isPressed) Color(0x66000000) else Color(0x33FFFFFF)
    val bottomShadowColor = if (isPressed) Color(0x1AFFFFFF) else Color(0x59000000)

    // Top-Left bevel path
    val pathTopLeft = Path().apply {
        moveTo(innerLeft, innerBottom)
        lineTo(innerLeft, innerTop)
        lineTo(innerRight, innerTop)
    }
    drawPath(pathTopLeft, color = topHighlightColor, style = Stroke(width = shadingWidth))

    // Bottom-Right bevel path
    val pathBottomRight = Path().apply {
        moveTo(innerRight, innerTop)
        lineTo(innerRight, innerBottom)
        lineTo(innerLeft, innerBottom)
    }
    drawPath(pathBottomRight, color = bottomShadowColor, style = Stroke(width = shadingWidth))

    // Knob Dark Border: 4px solid #1c1c1c
    drawRoundRect(
        color = Color(0xFF1C1C1C),
        topLeft = Offset(knobTopLeft.x + borderWidth / 2f, knobTopLeft.y + borderWidth / 2f),
        size = Size(knobPx - borderWidth, knobPx - borderWidth),
        cornerRadius = knobCornerRadius,
        style = Stroke(width = borderWidth)
    )

    // Lock mark
    if (isLocked) {
        drawCircle(
            color = Color(0xFFFFA000),
            center = Offset(knobCenter.x, knobTopLeft.y + 8f * density),
            radius = 4f * density
        )
    }
}

/**
 * 绘制背景层
 */
private fun DrawScope.drawBackgroundLayer(
    layoutDirection: LayoutDirection,
    size: Size,
    shape: Shape,
    backgroundColor: Color,
    borderColor: Color,
    borderWidthPx: Float
) {
    val outline = shape.createOutline(
        size = size,
        layoutDirection = layoutDirection,
        density = this
    )

    val clipPath = when (outline) {
        is Outline.Generic -> outline.path
        is Outline.Rounded -> Path().apply {
            addRoundRect(outline.roundRect)
        }
        is Outline.Rectangle -> Path().apply {
            addRect(outline.rect)
        }
    }

    clipPath(clipPath) {
        drawOutline(
            outline = outline,
            color = backgroundColor
        )

        if (borderWidthPx > 0f) {
            drawOutline(
                outline = outline,
                color = borderColor,
                style = Stroke(width = borderWidthPx)
            )
        }
    }
}

/**
 * 绘制摇杆层
 */
private fun DrawScope.drawJoystick(
    layoutDirection: LayoutDirection,
    color: Color,
    center: Offset,
    size: Float,
    shape: Shape
) {
    val halfSize = size / 2
    val topLeftX = center.x - halfSize
    val topLeftY = center.y - halfSize

    val outline = shape.createOutline(
        size = Size(size, size),
        layoutDirection = layoutDirection,
        density = this
    )

    translate(
        left = topLeftX,
        top = topLeftY
    ) {
        drawOutline(
            outline = outline,
            color = color
        )
    }
}

private fun Color.applyAlpha(multiplier: Float): Color {
    return copy(alpha = this.alpha * multiplier)
}

@Composable
fun JoystickStyleWidget(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    style: ObservableJoystickStyle,
    widgetSize: Dp = 120.dp,
    deadZoneRatio: Float = 0.5f,
    lockThreshold: Float = 0.3f,
) {
    val theme = if (isDarkTheme) style.darkStyle else style.lightStyle

    val backgroundShape = remember(theme.backgroundShape) {
        RoundedCornerShape(percent = theme.backgroundShape)
    }

    val joystickShape = remember(theme.joystickShape) {
        RoundedCornerShape(percent = theme.joystickShape)
    }

    val borderWidthRatio = remember(theme.borderWidthRatio) {
        (theme.borderWidthRatio.toFloat() / 100f).coerceIn(0.0f, 0.5f)
    }

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val backgroundSizePx = remember(widgetSize) {
        with(density) { widgetSize.toPx() }
    }

    val backgroundRegion = remember(backgroundShape, backgroundSizePx) {
        backgroundShape.toRegion(
            size = Size(backgroundSizePx, backgroundSizePx),
            density = density,
            layoutDirection = layoutDirection
        )
    }
    val currentBackgroundRegion by rememberUpdatedState(backgroundRegion)

    val joystickSizePx = remember(backgroundSizePx, theme.joystickSize) {
        backgroundSizePx * theme.joystickSize.coerceIn(0.0f, 1.0f)
    }

    val centerPoint = remember(backgroundSizePx) {
        Offset(backgroundSizePx / 2, backgroundSizePx / 2)
    }

    val deadZoneRadius = remember(backgroundSizePx, deadZoneRatio) {
        backgroundSizePx * deadZoneRatio / 2
    }

    val lockThresholdPx = remember(backgroundSizePx, lockThreshold) {
        backgroundSizePx * lockThreshold
    }

    val lockPosition = remember(centerPoint) {
        Offset(centerPoint.x, 0f)
    }

    var joystickPosition by remember { mutableStateOf(centerPoint) }
    var isLocked by remember { mutableStateOf(false) }
    var internalCanLock by remember { mutableStateOf(false) }
    var lastDragPosition by remember { mutableStateOf(Offset.Zero) }

    fun updateJoystickState(position: Offset) {
        val clampedPosition = position.clampToRegion(
            region = currentBackgroundRegion,
            center = centerPoint
        )
        joystickPosition = clampedPosition

        val direction = calculateDirection(
            joystickPosition = clampedPosition,
            backgroundCenter = centerPoint,
            deadZoneRadius = deadZoneRadius
        )

        internalCanLock =
            direction == JoystickDirection.North &&
            lastDragPosition.y < -lockThresholdPx
    }

    val currentBackgroundColor = remember(theme.backgroundColor, theme.alpha) {
        theme.backgroundColor.applyAlpha(theme.alpha)
    }
    val currentJoystickColor = remember(theme.joystickColor, theme.alpha) {
        theme.joystickColor.applyAlpha(theme.alpha)
    }
    val currentJoystickCanLockColor = remember(theme.joystickCanLockColor, theme.alpha) {
        theme.joystickCanLockColor.applyAlpha(theme.alpha)
    }
    val currentJoystickLockedColor = remember(theme.joystickLockedColor, theme.alpha) {
        theme.joystickLockedColor.applyAlpha(theme.alpha)
    }
    val currentLockMarkColor = remember(theme.lockMarkColor, theme.alpha) {
        theme.lockMarkColor.applyAlpha(theme.alpha)
    }
    val currentBorderColor = remember(theme.borderColor, theme.alpha) {
        theme.borderColor.applyAlpha(theme.alpha)
    }

    Box(
        modifier = modifier.size(widgetSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (currentBackgroundRegion.contains(offset.x.toInt(), offset.y.toInt())) {
                                lastDragPosition = offset
                                updateJoystickState(offset)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            lastDragPosition = change.position
                            if (isLocked) isLocked = false
                            updateJoystickState(change.position)
                        },
                        onDragEnd = {
                            if (internalCanLock) {
                                isLocked = true
                                updateJoystickState(lockPosition)
                            } else {
                                isLocked = false
                                updateJoystickState(centerPoint)
                            }
                        },
                        onDragCancel = {
                            isLocked = false
                            updateJoystickState(centerPoint)
                        }
                    )
                }
        ) {
            val canvasSize = this.size

            if (style.uuid.contains("bedrock", ignoreCase = true) || theme.backgroundShape <= 15) {
                drawAuthenticMcpeSquareJoystick(
                    size = canvasSize,
                    bgCenter = centerPoint,
                    knobCenter = joystickPosition,
                    knobRatio = theme.joystickSize.takeIf { it > 0.1f } ?: (64f / 136f),
                    density = density.density,
                    isPressed = joystickPosition != centerPoint,
                    isLocked = isLocked
                )
            } else {
                drawBackgroundLayer(
                    layoutDirection = layoutDirection,
                    size = canvasSize,
                    shape = backgroundShape,
                    backgroundColor = currentBackgroundColor,
                    borderColor = currentBorderColor,
                    borderWidthPx = (minOf(canvasSize.width, canvasSize.height) * borderWidthRatio).coerceAtLeast(0f)
                )

                drawJoystick(
                    layoutDirection = layoutDirection,
                    color = when {
                        isLocked -> currentJoystickLockedColor
                        internalCanLock -> currentJoystickCanLockColor
                        else -> currentJoystickColor
                    },
                    center = joystickPosition,
                    size = joystickSizePx,
                    shape = joystickShape
                )

                if (isLocked) {
                    drawCircle(
                        color = currentLockMarkColor,
                        center = lockPosition,
                        radius = 4f
                    )
                }
            }
        }
    }
}
