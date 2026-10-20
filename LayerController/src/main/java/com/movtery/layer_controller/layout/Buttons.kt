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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.layer_controller.data.TextAlignment
import com.movtery.layer_controller.event.EventHandler
import com.movtery.layer_controller.observable.DefaultObservableButtonStyle
import com.movtery.layer_controller.observable.ObservableButtonStyle
import com.movtery.layer_controller.observable.ObservableNormalData
import com.movtery.layer_controller.observable.ObservableTextData
import com.movtery.layer_controller.observable.ObservableTranslatableString
import com.movtery.layer_controller.observable.ObservableWidget
import com.movtery.layer_controller.utils.buttonContentColorAsState
import com.movtery.layer_controller.utils.buttonFontSizeAsState
import com.movtery.layer_controller.utils.buttonSize
import com.movtery.layer_controller.utils.buttonStyle
import com.movtery.layer_controller.utils.editMode
import com.movtery.layer_controller.utils.snap.GuideLine
import com.movtery.layer_controller.utils.snap.SnapMode

private fun resolveBedrockIcon(uuid: String, text: String): String? {
    val id = uuid.lowercase()
    val t = text.lowercase().trim()
    return when {
        id.contains("esc") || t == "esc" || id.contains("pause") || t.contains("pause") || t == "||" -> "pause"
        id.contains("chat") || t.contains("chat") || t == "t" -> "chat"
        id.contains("f5") || t.contains("f5") || id.contains("perspective") || t.contains("perspective") || t.contains("third") -> "perspective"
        id.contains("inv") || t == "inv" || t.contains("inventory") || t == "e" || t == "..." -> "inventory"
        id.contains("atk") || id.contains("attack") || t.contains("attack") || t.contains("mine") || t == "pri" -> "attack"
        id.contains("use") || id.contains("interact") || t.contains("use") || t.contains("interact") || t == "sec" -> "use_item"
        id.contains("jump") || t.contains("jump") || t == "space" -> "jump"
        id.contains("sneak") || t.contains("sneak") || id.contains("crouch") || t.contains("crouch") || t.contains("shift") -> "sneak"
        id.contains("swap") || t.contains("swap") || t == "f" || (t.contains("f") && !t.contains("f3") && !t.contains("f5")) -> "swap_hands"
        id.contains("drop") || t.contains("drop") || t == "q" -> "drop_item"
        id.contains("f3") || t.contains("f3") || id.contains("debug") -> "f3_debug"
        id.contains("tab") || t.contains("tab") || id.contains("list") -> "player_list"
        id.contains("sprint") || t.contains("sprint") -> "sprint"
        id.contains("fly_up") || t.contains("fly up") -> "fly_up"
        id.contains("fly_down") || t.contains("fly down") -> "fly_down"
        id.contains("fly") || t.contains("fly") -> "fly_button"
        id.contains("dismount") || t.contains("dismount") -> "dismount"
        id.contains("gui") || t == "gui" || id.contains("menu") || t == "menu" || id.contains("layers") || t.contains("layer") -> "gui"
        id.contains("key") || t == "key" || id.contains("ime") || t == "ime" || id.contains("keyboard") || t.contains("keyboard") || id.contains("agent") || t.contains("agent") -> "keyboard"
        id.contains("pick") || t.contains("pick") -> "pick_block"
        id.contains("forward") || (id.contains("up") && !id.contains("fly")) || t == "▲" || (id.contains("dpad") && t == "w") -> "forward_button_be2"
        id.contains("backward") || (id.contains("down") && !id.contains("fly")) || t == "▼" || (id.contains("dpad") && t == "s") -> "backward_button_be2"
        id.contains("left") || (id.contains("dpad") && t == "a") || t == "◀" -> "left_button_be2"
        id.contains("right") || (id.contains("dpad") && t == "d") || t == "▶" -> "right_button_be2"
        else -> null
    }
}

private fun getBedrockIconResId(iconName: String?, isPressed: Boolean = false): Int {
    if (iconName.isNullOrEmpty()) return 0
    val key = iconName.removeSuffix(".png").lowercase()
    if (isPressed) {
        when (key) {
            "jump_button_be2", "jump_button", "jump" -> return com.movtery.layer_controller.R.drawable.jump_pressed
            "sneak_button_be2", "sneak_button_be1", "sneak_button", "sneak" -> return com.movtery.layer_controller.R.drawable.sneak_pressed
            "attack_button", "attack", "pri", "mine" -> return com.movtery.layer_controller.R.drawable.attack_pressed
            else -> {}
        }
    }
    return when (key) {
        "tab_button_be2", "tab_button", "tab", "player_list" -> com.movtery.layer_controller.R.drawable.player_list
        "f3_button_be2", "f3_button", "f3", "f3_debug" -> com.movtery.layer_controller.R.drawable.f3_debug
        "esc_button_be2", "esc_button", "esc", "pause_button", "pause" -> com.movtery.layer_controller.R.drawable.pause
        "chat_button_be2", "chat_button_be1", "chat_button", "chat", "t" -> com.movtery.layer_controller.R.drawable.chat
        "f5_button_be2", "perspective_button", "perspective", "f5" -> com.movtery.layer_controller.R.drawable.perspective
        "inv_button_be2", "inv_button", "inv", "inventory", "e" -> com.movtery.layer_controller.R.drawable.inventory
        "jump_button_be2", "jump_button", "jump" -> com.movtery.layer_controller.R.drawable.jump
        "sneak_button_be2", "sneak_button_be1", "sneak_button", "sneak" -> com.movtery.layer_controller.R.drawable.sneak
        "sneak_toggled_button_be1", "sneak_toggled" -> com.movtery.layer_controller.R.drawable.sneak_toggled
        "f_swap_button_be2", "f_swap_button", "swap", "swap_hands", "f" -> com.movtery.layer_controller.R.drawable.swap_hands
        "q_drop_button_be2", "q_drop_button", "q", "drop", "drop_item" -> com.movtery.layer_controller.R.drawable.drop_item
        "interact_button", "interact_button_be1", "interact", "sec", "use_item", "use" -> com.movtery.layer_controller.R.drawable.use_item
        "attack_button", "attack", "pri", "mine" -> com.movtery.layer_controller.R.drawable.attack
        "keyboard", "keyboard_button", "key", "key_button_be2", "agent_button_be2", "agent_button", "ime" -> com.movtery.layer_controller.R.drawable.keyboard
        "gui", "gui_button", "menu_button", "menu", "cancel_action" -> com.movtery.layer_controller.R.drawable.gui
        "forward_button_be2", "forward_button_be1", "forward_button", "forward", "dpad_up" -> com.movtery.layer_controller.R.drawable.forward_button_be2
        "backward_button_be2", "backward_button_be1", "backward_button", "backward", "dpad_down" -> com.movtery.layer_controller.R.drawable.backward_button_be2
        "left_button_be2", "left_button_be1", "left_button", "left", "strafe_left", "dpad_left" -> com.movtery.layer_controller.R.drawable.left_button_be2
        "right_button_be2", "right_button_be1", "right_button", "right", "strafe_right", "dpad_right" -> com.movtery.layer_controller.R.drawable.right_button_be2
        "forward_left_button_be2", "forward_left_button_be1", "forward_left" -> com.movtery.layer_controller.R.drawable.forward_left_button_be2
        "forward_right_button_be2", "forward_right_button_be1", "forward_right" -> com.movtery.layer_controller.R.drawable.forward_right_button_be2
        "down_left_button_be", "backward_left" -> com.movtery.layer_controller.R.drawable.down_left_button_be
        "down_right_button", "backward_right" -> com.movtery.layer_controller.R.drawable.down_right_button
        "dpad_center" -> com.movtery.layer_controller.R.drawable.dpad_center
        "sprint_button", "sprint" -> com.movtery.layer_controller.R.drawable.sprint
        "fly_button", "fly" -> com.movtery.layer_controller.R.drawable.fly_button
        "fly_up_button", "flying_ascend_button", "fly_up" -> com.movtery.layer_controller.R.drawable.fly_up
        "fly_down_button", "flying_descend_button", "fly_down" -> com.movtery.layer_controller.R.drawable.fly_down
        "dismount_button", "dismount" -> com.movtery.layer_controller.R.drawable.dismount
        "emote_button_be2", "emote_button_be1", "emote" -> com.movtery.layer_controller.R.drawable.emote
        "pick_block_button", "pick_block", "pick" -> com.movtery.layer_controller.R.drawable.pick_block
        "crosshair" -> com.movtery.layer_controller.R.drawable.crosshair
        "shield" -> com.movtery.layer_controller.R.drawable.shield
        "normal_button" -> com.movtery.layer_controller.R.drawable.normal_button
        "pressed_button" -> com.movtery.layer_controller.R.drawable.pressed_button
        else -> 0
    }
}

private data class ButtonTextStyle(
    val text: ObservableTranslatableString,
    val textAlignment: TextAlignment,
    val textBold: Boolean,
    val textItalic: Boolean,
    val textUnderline: Boolean
)

/**
 * 基础文本控件
 * @param allStyles 当前控制布局所有的样式（用于加载控件的样式）
 * @param enableSnap 编辑模式下，是否开启吸附功能
 * @param snapMode 吸附模式
 * @param localSnapRange 局部吸附范围（仅在Local模式下有效）
 * @param getOtherWidgets 获取其他控件的信息，在编辑模式下，用于计算吸附位置
 * @param snapThresholdValue 吸附距离阈值
 * @param eventHandler 事件处理器
 * @param drawLine 绘制吸附参考线
 * @param onLineCancel 取消吸附参考线
 */
@Composable
internal fun TextButton(
    isEditMode: Boolean,
    data: ObservableWidget,
    allStyles: List<ObservableButtonStyle>,
    screenSize: IntSize,
    isDark: Boolean = isSystemInDarkTheme(),
    visible: Boolean = true,
    enableSnap: Boolean = false,
    snapMode: SnapMode = SnapMode.FullScreen,
    localSnapRange: Dp = 50.dp,
    getOtherWidgets: () -> List<ObservableWidget>,
    snapThresholdValue: Dp,
    eventHandler: EventHandler? = null,
    drawLine: (ObservableWidget, List<GuideLine>) -> Unit = { _, _ -> },
    onLineCancel: (ObservableWidget) -> Unit = {},
    isPressed: Boolean,
    onTapInEditMode: () -> Unit = {}
) {
    if (visible) {
        val styleId = data.styleId
        val style = allStyles
            .takeIf { data.styleId != null }
            ?.find { it.uuid == styleId }
            ?: DefaultObservableButtonStyle

        val locale = LocalConfiguration.current.locales[0]

        val buttonTextStyle = when (data) {
            is ObservableNormalData -> ButtonTextStyle(
                text = data.text,
                textAlignment = data.textAlignment,
                textBold = data.textBold,
                textItalic = data.textItalic,
                textUnderline = data.textUnderline
            )
            is ObservableTextData -> ButtonTextStyle(
                text = data.text,
                textAlignment = data.textAlignment,
                textBold = data.textBold,
                textItalic = data.textItalic,
                textUnderline = data.textUnderline
            )
            else -> error("Unknown widget type")
        }

        val normalIcon = if (data is ObservableNormalData) data.icon else null
        val widgetUuid = when (data) {
            is ObservableNormalData -> data.uuid
            is ObservableTextData -> data.uuid
            else -> ""
        }
        val translatedText = buttonTextStyle.text.translate(locale)
        val iconName = normalIcon ?: resolveBedrockIcon(widgetUuid, translatedText)
        val context = LocalContext.current
        val iconResId = remember(iconName, isPressed) {
            val directRes = getBedrockIconResId(iconName, isPressed)
            if (directRes != 0) directRes
            else runCatching {
                val name = iconName?.removeSuffix(".png") ?: ""
                var res = context.resources.getIdentifier(name, "drawable", context.packageName)
                if (res == 0) {
                    res = context.resources.getIdentifier(name, "drawable", "com.movtery.zalithlauncher")
                }
                res
            }.getOrDefault(0)
        }

        Box(
            modifier = Modifier
                .buttonSize(data, screenSize)
                .then(
                    if (isEditMode) {
                        Modifier.border(BorderStroke(1.dp, Color(0x66FFFFFF)), RoundedCornerShape(4.dp))
                    } else Modifier
                )
                .editMode(
                    isEditMode = isEditMode,
                    data = data,
                    screenSize = screenSize,
                    enableSnap = enableSnap,
                    snapMode = snapMode,
                    localSnapRange = localSnapRange,
                    getOtherWidgets = getOtherWidgets,
                    snapThresholdValue = snapThresholdValue,
                    drawLine = drawLine,
                    onLineCancel = onLineCancel,
                    onTapInEditMode = onTapInEditMode
                ),
            contentAlignment = Alignment.Center
        ) {
            val fontSize by buttonFontSizeAsState(
                style = style,
                isDark = isDark,
                isPressed = isPressed
            )

            if (iconResId != 0) {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isPressed) 2.dp else 0.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                val fallbackBaseRes = if (isPressed) {
                    com.movtery.layer_controller.R.drawable.pressed_button
                } else {
                    com.movtery.layer_controller.R.drawable.normal_button
                }
                Image(
                    painter = painterResource(id = fallbackBaseRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isPressed) 2.dp else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RtLText(
                        text = translatedText,
                        color = Color.White,
                        fontSize = fontSize.sp,
                        textAlign = buttonTextStyle.textAlignment.textAlign,
                        fontWeight = FontWeight.Bold,
                        fontStyle = if (buttonTextStyle.textItalic) FontStyle.Italic else null,
                        textDecoration = if (buttonTextStyle.textUnderline) TextDecoration.Underline else null,
                        style = LocalTextStyle.current.copy(
                            lineHeight = (fontSize * 1.1).sp
                        )
                    )
                }
            }

            DisposableEffect(Unit) {
                data.onCompositionStart(eventHandler)
                onDispose {
                    data.onCompositionDispose(eventHandler)
                }
            }
        }
    } else {
        //虚假的控件，使用一个空的组件，只是让Layout有东西能测
        Spacer(
            modifier = Modifier.buttonSize(data, screenSize)
        )
    }
}

/**
 * 仅渲染控件外观的组件
 */
@Composable
fun RendererStyleBox(
    style: ObservableButtonStyle,
    modifier: Modifier = Modifier,
    text: String = "",
    isDark: Boolean,
    isPressed: Boolean
) {
    Box(
        modifier = modifier.buttonStyle(
            style = style,
            isDark = isDark,
            isPressed = isPressed
        ),
        contentAlignment = Alignment.Center
    ) {
        val color by buttonContentColorAsState(style = style, isDark = isDark, isPressed = isPressed)
        val fontSize by buttonFontSizeAsState(style = style, isDark = isDark, isPressed = isPressed)
        RtLText(
            text = text,
            color = color,
            fontSize = fontSize.sp
        )
    }
}