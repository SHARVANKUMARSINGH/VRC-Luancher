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
package com.movtery.zalithlauncher.game.multirt

import com.movtery.zalithlauncher.ZLApplication.Companion.DEVICE_ARCHITECTURE
import com.movtery.zalithlauncher.utils.device.Architecture.archAsInt

data class Runtime(
    val name: String,
    val versionString: String?,
    val arch: String?,
    val javaVersion: Int,
    val isProvidedByLauncher: Boolean,
    val isJDK8: Boolean
) {
    constructor(name: String) : this(
        name = name,
        versionString = null,
        arch = null,
        javaVersion = 0,
        isProvidedByLauncher = false,
        isJDK8 = false
    )

    fun isCompatible(): Boolean {
        if (versionString == null) return false
        val rtArch = archAsInt(arch)
        if (DEVICE_ARCHITECTURE == rtArch) return true
        if (DEVICE_ARCHITECTURE == com.movtery.zalithlauncher.utils.device.Architecture.ARCH_ARM64 && rtArch == com.movtery.zalithlauncher.utils.device.Architecture.ARCH_ARM) return true
        if (DEVICE_ARCHITECTURE == com.movtery.zalithlauncher.utils.device.Architecture.ARCH_ARM && rtArch == com.movtery.zalithlauncher.utils.device.Architecture.ARCH_ARM64) return true
        return false
    }
}
