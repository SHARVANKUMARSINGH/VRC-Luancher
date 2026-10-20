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

package com.movtery.zalithlauncher.components.jre

import android.content.Context
import android.content.res.AssetManager
import com.movtery.zalithlauncher.ZLApplication
import com.movtery.zalithlauncher.components.AbstractUnpackTask
import com.movtery.zalithlauncher.components.InstallableItem
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.utils.device.Architecture
import com.movtery.zalithlauncher.utils.file.readString
import com.movtery.zalithlauncher.utils.logging.Logger

private const val TAG = "UnpackJreTask"

class UnpackJreTask(
    private val context: Context,
    private val jre: Jre
) : AbstractUnpackTask() {
    private lateinit var assetManager: AssetManager
    private lateinit var launcherRuntimeVersion: String
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            assetManager = context.assets
            if (!isJreArchSupported()) {
                //当前设备架构不支持使用这个环境
                isCheckFailed = true
                return@runCatching
            }
            launcherRuntimeVersion = assetManager.open(jre.jrePath + "/version").readString()
        }.onFailure { e ->
            Logger.warning(TAG, "Failed to init jre version. assetsPath=${jre.jrePath}/version", e)
            isCheckFailed = true
        }
    }

    fun isCheckFailed() = isCheckFailed

    override fun checkState(): InstallableItem.State {
        if (isCheckFailed) return InstallableItem.State.NOT_EXISTS

        return runCatching {
            val installedRuntimeVersion = RuntimesManager.loadInternalRuntimeVersion(jre.jreName)
            when {
                //未安装该环境
                installedRuntimeVersion == null -> InstallableItem.State.NOT_STARTED
                launcherRuntimeVersion != installedRuntimeVersion -> InstallableItem.State.PENDING
                else -> InstallableItem.State.FINISHED
            }
        }.onFailure { e ->
            Logger.error("CheckJre", "An exception occurred while detecting the Java Runtime.", e)
        }.getOrElse {
            //检查失败，要求重新进行安装
            InstallableItem.State.NOT_STARTED
        }
    }

    private fun getAvailableRuntime(): String? {
        val allPacks = runCatching { assetManager.list(jre.jrePath) }.getOrNull() ?: return null
        val target = "bin-" + Architecture.archAsString(ZLApplication.DEVICE_ARCHITECTURE) + ".tar.xz"
        if (allPacks.contains(target)) return target
        if (ZLApplication.DEVICE_ARCHITECTURE == Architecture.ARCH_ARM64 && allPacks.contains("bin-arm.tar.xz")) {
            return "bin-arm.tar.xz"
        }
        if (ZLApplication.DEVICE_ARCHITECTURE == Architecture.ARCH_ARM && allPacks.contains("bin-arm64.tar.xz")) {
            return "bin-arm64.tar.xz"
        }
        return null
    }

    private fun isJreArchSupported(): Boolean {
        val runtime = getAvailableRuntime()
        return (runtime != null).also {
            Logger.info(TAG, "Device environment for ${jre.jrePath}: $runtime, supported = $it")
        }
    }

    override suspend fun run() {
        val runtimeBin = getAvailableRuntime() ?: throw IllegalStateException("No compatible JRE runtime found in assets for ${jre.jrePath}")
        runCatching {
            RuntimesManager.installRuntimeBinPack(
                universalFileInputStream = assetManager.open(jre.jrePath + "/universal.tar.xz"),
                platformBinsInputStream = assetManager.open(jre.jrePath + "/" + runtimeBin),
                name = jre.jreName,
                binPackVersion = launcherRuntimeVersion,
                updateProgress = { textRes, textArgs ->
                    updateMessage(context.getString(textRes, *textArgs))
                }
            )
            RuntimesManager.postPrepare(jre.jreName)
        }.onFailure {
            Logger.error("UnpackJre", "Internal JRE unpack failed", it)
        }.getOrThrow()
    }
}