/*
 * Vrc Luancher - Authentic MCPE Square Joystick Component
 * Translated from Bedrock HUD Square Joystick specification into Kotlin
 */

package com.movtery.zalithlauncher.ui.control.joystick

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import org.lwjgl.glfw.CallbackBridge
import kotlin.math.max
import kotlin.math.min

/**
 * Authentic New MCPE Bedrock Square Joystick View.
 *
 * Implements pixel-accurate Bedrock HUD styling:
 * - Outer square base: 136dp (default), semi-transparent gray backing layer rgba(210, 210, 210, 0.25)
 * - 4dp solid #1c1c1c border with 8dp rounded corners
 * - Inset 3dp highlight line rgba(255, 255, 255, 0.15)
 * - Inner moving knob: 64dp (default), rgba(110, 110, 110, 0.85) (active: rgba(140, 140, 140, 0.95))
 * - Inset 3D pixel shading highlights with active-state depression response
 * - Clamps strictly to square box track boundaries
 * - Direct pairing with Minecraft Java Edition via LWJGL CallbackBridge
 */
class McpeSquareJoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Scaled DP dimensions
    private val density = resources.displayMetrics.density

    // Design baseline: 170dp touch zone, 136dp base, 64dp knob, 36dp max travel range
    var baseSizeDp: Float = 136f
        set(value) {
            field = value
            requestLayout()
        }

    var knobSizeDp: Float = 64f
        set(value) {
            field = value
            requestLayout()
        }

    // Border and corner styling matching Bedrock HUD
    private val borderWidth: Float get() = 4f * density
    private val baseCornerRadius: Float get() = 8f * density
    private val knobCornerRadius: Float get() = 10f * density
    private val innerHighlightWidth: Float get() = 3f * density
    private val pixelShadingWidth: Float get() = 4f * density

    // Movement state
    private var isTracking = false
    private var knobOffsetX = 0f
    private var knobOffsetY = 0f
    private var startCenterX = 0f
    private var startCenterY = 0f

    // Normalized outputs [-1.00, 1.00]
    var outputX: Float = 0f
        private set
    var outputY: Float = 0f
        private set

    // Auto-walk / sprint lock state
    var isLocked: Boolean = false
        private set
    var enableLock: Boolean = true

    // Callbacks
    var onJoystickMovedListener: ((x: Float, y: Float) -> Unit)? = null
    var onJoystickStateChangedListener: ((isPressed: Boolean, isLocked: Boolean) -> Unit)? = null

    // LWJGL Key Pairing configuration
    private var isPairedWithLwjgl = false
    private var activeForwardKey = false
    private var activeBackwardKey = false
    private var activeLeftKey = false
    private var activeRightKey = false
    private var activeSprintKey = false

    // GLFW keycodes
    companion object {
        const val GLFW_KEY_W = 87
        const val GLFW_KEY_S = 83
        const val GLFW_KEY_A = 65
        const val GLFW_KEY_D = 68
        const val GLFW_KEY_LEFT_CONTROL = 341
    }

    // Paints
    private val baseBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb((255 * 0.25f).toInt(), 210, 210, 210) // rgba(210, 210, 210, 0.25)
    }

    private val baseBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#1C1C1C")
    }

    private val baseInnerHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb((255 * 0.15f).toInt(), 255, 255, 255) // rgba(255, 255, 255, 0.15)
    }

    private val knobBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val knobBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#1C1C1C")
    }

    private val knobHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val knobShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val lockIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFA000")
    }

    // Reusable rects and paths to avoid allocations in onDraw
    private val baseRect = RectF()
    private val baseInnerRect = RectF()
    private val knobRect = RectF()
    private val knobInnerRect = RectF()
    private val tempPath = Path()

    init {
        isClickable = true
        isFocusable = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredTouchZonePx = (170f * density).toInt()
        val width = resolveSize(desiredTouchZonePx, widthMeasureSpec)
        val height = resolveSize(desiredTouchZonePx, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        startCenterX = w / 2f
        startCenterY = h / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val basePx = baseSizeDp * density
        val knobPx = knobSizeDp * density

        val baseLeft = (width - basePx) / 2f
        val baseTop = (height - basePx) / 2f
        val baseRight = baseLeft + basePx
        val baseBottom = baseTop + basePx

        // 1. Draw Outer Square Base
        baseRect.set(baseLeft, baseTop, baseRight, baseBottom)

        // Fill background
        canvas.drawRoundRect(baseRect, baseCornerRadius, baseCornerRadius, baseBgPaint)

        // Dark Border: 4px solid #1c1c1c
        baseBorderPaint.strokeWidth = borderWidth
        val halfBorder = borderWidth / 2f
        baseRect.inset(halfBorder, halfBorder)
        canvas.drawRoundRect(baseRect, baseCornerRadius, baseCornerRadius, baseBorderPaint)
        baseRect.inset(-halfBorder, -halfBorder)

        // Inner highlight line: inset 0 0 0 3px rgba(255, 255, 255, 0.15)
        val highlightInset = borderWidth + (innerHighlightWidth / 2f)
        baseInnerRect.set(
            baseRect.left + highlightInset,
            baseRect.top + highlightInset,
            baseRect.right - highlightInset,
            baseRect.bottom - highlightInset
        )
        baseInnerHighlightPaint.strokeWidth = innerHighlightWidth
        val innerCorner = max(1f, baseCornerRadius - highlightInset)
        canvas.drawRoundRect(baseInnerRect, innerCorner, innerCorner, baseInnerHighlightPaint)

        // 2. Draw Moving Square Knob
        val knobCenterX = startCenterX + knobOffsetX
        val knobCenterY = startCenterY + knobOffsetY
        val halfKnob = knobPx / 2f

        knobRect.set(
            knobCenterX - halfKnob,
            knobCenterY - halfKnob,
            knobCenterX + halfKnob,
            knobCenterY + halfKnob
        )

        // Knob background: rgba(110, 110, 110, 0.85) or active rgba(140, 140, 140, 0.95)
        if (isTracking) {
            knobBgPaint.color = Color.argb((255 * 0.95f).toInt(), 140, 140, 140)
        } else {
            knobBgPaint.color = Color.argb((255 * 0.85f).toInt(), 110, 110, 110)
        }
        canvas.drawRoundRect(knobRect, knobCornerRadius, knobCornerRadius, knobBgPaint)

        // 3D Pixel shading highlights on inner knob
        // Normal: Top/Left = rgba(255,255,255, 0.2), Bottom/Right = rgba(0,0,0, 0.35)
        // Active: Top/Left = rgba(0,0,0, 0.4), Bottom/Right = rgba(255,255,255, 0.1)
        val shadeWidth = pixelShadingWidth
        knobHighlightPaint.strokeWidth = shadeWidth
        knobShadowPaint.strokeWidth = shadeWidth

        if (isTracking) {
            knobHighlightPaint.color = Color.argb((255 * 0.4f).toInt(), 0, 0, 0) // inverted top/left shadow
            knobShadowPaint.color = Color.argb((255 * 0.1f).toInt(), 255, 255, 255) // inverted bottom/right
        } else {
            knobHighlightPaint.color = Color.argb((255 * 0.2f).toInt(), 255, 255, 255)
            knobShadowPaint.color = Color.argb((255 * 0.35f).toInt(), 0, 0, 0)
        }

        val insetShade = borderWidth + (shadeWidth / 2f)
        knobInnerRect.set(
            knobRect.left + insetShade,
            knobRect.top + insetShade,
            knobRect.right - insetShade,
            knobRect.bottom - insetShade
        )

        // Top & Left bevel highlight
        tempPath.reset()
        tempPath.moveTo(knobInnerRect.left, knobInnerRect.bottom)
        tempPath.lineTo(knobInnerRect.left, knobInnerRect.top)
        tempPath.lineTo(knobInnerRect.right, knobInnerRect.top)
        canvas.drawPath(tempPath, knobHighlightPaint)

        // Bottom & Right bevel shadow
        tempPath.reset()
        tempPath.moveTo(knobInnerRect.right, knobInnerRect.top)
        tempPath.lineTo(knobInnerRect.right, knobInnerRect.bottom)
        tempPath.lineTo(knobInnerRect.left, knobInnerRect.bottom)
        canvas.drawPath(tempPath, knobShadowPaint)

        // Knob dark border: 4px solid #1c1c1c
        knobBorderPaint.strokeWidth = borderWidth
        knobRect.inset(halfBorder, halfBorder)
        canvas.drawRoundRect(knobRect, knobCornerRadius, knobCornerRadius, knobBorderPaint)
        knobRect.inset(-halfBorder, -halfBorder)

        // Lock indicator mark if locked
        if (isLocked) {
            canvas.drawCircle(knobCenterX, knobRect.top + 8f * density, 3.5f * density, lockIndicatorPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val basePx = baseSizeDp * density
        val knobPx = knobSizeDp * density
        // Exact travel radius limit boundary calculations (Clamps the square shape boundaries)
        val maxRange = (basePx - knobPx) / 2f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isTracking = true
                parent?.requestDisallowInterceptTouchEvent(true)
                updateKnobPosition(event.x, event.y, maxRange)
                onJoystickStateChangedListener?.invoke(true, isLocked)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isTracking) {
                    updateKnobPosition(event.x, event.y, maxRange)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isTracking) {
                    isTracking = false

                    // If dragged all the way to top boundary and lock is enabled, toggle lock
                    if (enableLock && outputY >= 0.92f) {
                        isLocked = true
                        knobOffsetX = 0f
                        knobOffsetY = -maxRange
                        outputX = 0f
                        outputY = 1.0f
                        invalidate()
                        dispatchMovement(outputX, outputY)
                    } else {
                        isLocked = false
                        snapBackToCenter()
                    }

                    onJoystickStateChangedListener?.invoke(false, isLocked)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateKnobPosition(pointerX: Float, pointerY: Float, maxRange: Float) {
        val targetDeltaX = pointerX - startCenterX
        val targetDeltaY = pointerY - startCenterY

        // Clamping rules to match square box track boundaries perfectly
        val finalX = max(-maxRange, min(maxRange, targetDeltaX))
        val finalY = max(-maxRange, min(maxRange, targetDeltaY))

        knobOffsetX = finalX
        knobOffsetY = finalY

        // Convert movement tracking offsets into universal game engine metrics [-1.00 to 1.00]
        outputX = (finalX / maxRange).coerceIn(-1.0f, 1.0f)
        outputY = (-(finalY / maxRange)).coerceIn(-1.0f, 1.0f) // Flips value orientation natively so Forward is up

        invalidate()
        dispatchMovement(outputX, outputY)
    }

    private fun snapBackToCenter() {
        val startX = knobOffsetX
        val startY = knobOffsetY
        if (startX == 0f && startY == 0f) {
            outputX = 0f
            outputY = 0f
            dispatchMovement(0f, 0f)
            return
        }

        val animator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 120
            interpolator = OvershootInterpolator(0.8f)
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                knobOffsetX = startX * fraction
                knobOffsetY = startY * fraction
                outputX = 0f
                outputY = 0f
                invalidate()
            }
        }
        animator.start()
        dispatchMovement(0f, 0f)
    }

    private fun dispatchMovement(x: Float, y: Float) {
        onJoystickMovedListener?.invoke(x, y)

        if (isPairedWithLwjgl) {
            updateLwjglKeyEvents(x, y)
        }
    }

    /**
     * Connects this joystick to the Minecraft Java Edition LWJGL input engine.
     * When paired, moving the joystick generates authentic WASD keystrokes and Sprint events.
     */
    fun pairWithLwjgl(enableSprint: Boolean = true) {
        this.isPairedWithLwjgl = true
        this.enableLock = enableSprint
    }

    /**
     * Unpairs from the LWJGL game engine and safely releases all active movement keys.
     */
    fun unpairFromLwjgl() {
        this.isPairedWithLwjgl = false
        releaseAllGameKeys()
    }

    private fun updateLwjglKeyEvents(x: Float, y: Float) {
        val threshold = 0.28f

        val needForward = y > threshold
        val needBackward = y < -threshold
        val needLeft = x < -threshold
        val needRight = x > threshold
        val needSprint = isLocked || (enableLock && y >= 0.88f)

        try {
            val mods = CallbackBridge.getCurrentMods()

            if (needForward != activeForwardKey) {
                activeForwardKey = needForward
                CallbackBridge.sendKeyPress(GLFW_KEY_W, mods, needForward)
            }
            if (needBackward != activeBackwardKey) {
                activeBackwardKey = needBackward
                CallbackBridge.sendKeyPress(GLFW_KEY_S, mods, needBackward)
            }
            if (needLeft != activeLeftKey) {
                activeLeftKey = needLeft
                CallbackBridge.sendKeyPress(GLFW_KEY_A, mods, needLeft)
            }
            if (needRight != activeRightKey) {
                activeRightKey = needRight
                CallbackBridge.sendKeyPress(GLFW_KEY_D, mods, needRight)
            }
            if (needSprint != activeSprintKey) {
                activeSprintKey = needSprint
                CallbackBridge.sendKeyPress(GLFW_KEY_LEFT_CONTROL, mods, needSprint)
            }
        } catch (e: Throwable) {
            // CallbackBridge may not be available outside game runtime
        }
    }

    private fun releaseAllGameKeys() {
        try {
            val mods = CallbackBridge.getCurrentMods()
            if (activeForwardKey) {
                CallbackBridge.sendKeyPress(GLFW_KEY_W, mods, false)
                activeForwardKey = false
            }
            if (activeBackwardKey) {
                CallbackBridge.sendKeyPress(GLFW_KEY_S, mods, false)
                activeBackwardKey = false
            }
            if (activeLeftKey) {
                CallbackBridge.sendKeyPress(GLFW_KEY_A, mods, false)
                activeLeftKey = false
            }
            if (activeRightKey) {
                CallbackBridge.sendKeyPress(GLFW_KEY_D, mods, false)
                activeRightKey = false
            }
            if (activeSprintKey) {
                CallbackBridge.sendKeyPress(GLFW_KEY_LEFT_CONTROL, mods, false)
                activeSprintKey = false
            }
        } catch (ignored: Throwable) {
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        releaseAllGameKeys()
    }
}

/**
 * Native Android View bridge for Jetpack Compose.
 * Directly embeds the Native [McpeSquareJoystickView] within any Compose hierarchy,
 * giving native hardware-accelerated rendering and direct LWJGL Minecraft pairing.
 */
@androidx.compose.runtime.Composable
fun McpeSquareJoystickNative(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    enableSprintLock: Boolean = true,
    onMove: ((Float, Float) -> Unit)? = null
) {
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { ctx ->
            McpeSquareJoystickView(ctx).apply {
                pairWithLwjgl(enableSprintLock)
                onJoystickMovedListener = onMove
            }
        },
        update = { view ->
            view.enableLock = enableSprintLock
            if (onMove != null) {
                view.onJoystickMovedListener = onMove
            }
        }
    )
}
