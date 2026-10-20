/*
 * Vrc Luancher - Auto Hardware & OpenGL Optimizer
 * Automatically detects device GPU, Vulkan capabilities, CPU, and RAM,
 * configuring optimal renderers, memory, and JVM flags for native speed execution.
 */

package com.movtery.zalithlauncher.game.setup

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.renderer.renderers.FreedrenoRenderer
import com.movtery.zalithlauncher.game.renderer.renderers.GL4ESRenderer
import com.movtery.zalithlauncher.game.renderer.renderers.KopperZinkRenderer
import com.movtery.zalithlauncher.game.renderer.renderers.NGGL4ESRenderer
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File
import kotlin.math.min

object AutoSetupManager {
    private const val TAG = "AutoSetupManager"

    // High performance GC and JVM arguments tuned for Minecraft Java on Android
    const val NATIVE_SPEED_JVM_ARGS =
        "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -XX:+TieredCompilation -XX:TieredStopAtLevel=4 -Dsun.misc.URLClassPath.disableJarChecking=true -Djava.net.preferIPv4Stack=true"

    const val BALANCED_JVM_ARGS =
        "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=250 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=25 -XX:G1MaxNewSizePercent=35 -XX:SurvivorRatio=32 -Dsun.misc.URLClassPath.disableJarChecking=true"

    const val LOW_END_JVM_ARGS =
        "-XX:+UseSerialGC -Dsun.misc.URLClassPath.disableJarChecking=true"

    enum class PerformancePreset(val title: String, val description: String) {
        NATIVE_SPEED("Native Speed (Max FPS)", "Multi-threaded Mesa OpenGL, aggressive low-latency G1GC, maximized heap memory"),
        BALANCED("Balanced", "Balanced frame stability, standard GC pauses, battery friendly"),
        POWER_SAVER("Power Saver / Low End", "Reduced memory usage, single-threaded GL4ES, cooler device temperature")
    }

    data class SetupResult(
        val gpuVendor: String,
        val totalRamMb: Int,
        val recommendedRamMb: Int,
        val recommendedRenderer: String,
        val recommendedRendererName: String,
        val vulkanSupported: Boolean,
        val multithreadedGlEnabled: Boolean,
        val jvmArgsApplied: String
    )

    /**
     * Inspects device hardware and queries total physical RAM in MB.
     */
    fun getTotalPhysicalRamMb(context: Context): Int {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            (memInfo.totalMem / (1024 * 1024)).toInt()
        } catch (e: Exception) {
            2048
        }
    }

    /**
     * Checks whether the device hardware supports Vulkan.
     */
    fun isVulkanSupported(context: Context): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) &&
                pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
    }

    /**
     * Detects GPU manufacturer string from build properties or SoC name.
     */
    fun detectGpuSocVendor(): String {
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MANUFACTURER.lowercase()
        } else ""

        return when {
            hardware.contains("qcom") || board.contains("qcom") || soc.contains("qualcomm") ||
                    hardware.contains("adreno") || board.contains("adreno") -> "Qualcomm Adreno"
            hardware.contains("mali") || board.contains("mali") || hardware.contains("exynos") ||
                    board.contains("universal") || soc.contains("samsung") -> "ARM Mali / Exynos"
            hardware.contains("mt") || board.contains("mt") || soc.contains("mediatek") ||
                    hardware.contains("dimensity") -> "MediaTek Dimensity"
            hardware.contains("tensor") || soc.contains("google") -> "Google Tensor"
            hardware.contains("powervr") || hardware.contains("rogue") -> "PowerVR"
            else -> "$manufacturer ${Build.MODEL}"
        }
    }

    /**
     * Computes optimal RAM allocation in MB based on total device RAM.
     * Leaves sufficient overhead for native Mesa/Vulkan driver buffers and Android OS.
     */
    fun computeOptimalRamMb(totalRamMb: Int): Int {
        return when {
            totalRamMb >= 16384 -> 6144
            totalRamMb >= 12288 -> 5120
            totalRamMb >= 8192 -> 4096
            totalRamMb >= 6144 -> 3072
            totalRamMb >= 4096 -> 2048
            totalRamMb >= 3072 -> 1536
            else -> 1024
        }
    }

    /**
     * Automatically analyzes the device and selects the best OpenGL / Vulkan renderer.
     */
    fun detectBestRenderer(context: Context): String {
        Renderers.init()
        val hasVulkan = isVulkanSupported(context)
        val soc = detectGpuSocVendor().lowercase()

        return when {
            // Qualcomm Adreno with Vulkan has highest native performance using Turnip / Kopper Zink
            soc.contains("adreno") && hasVulkan -> {
                // Check if Kopper Zink or Freedreno is available
                val renderers = Renderers.getRenderers()
                if (renderers.any { it.getUniqueIdentifier() == KopperZinkRenderer.getUniqueIdentifier() }) {
                    KopperZinkRenderer.getUniqueIdentifier()
                } else if (renderers.any { it.getUniqueIdentifier() == FreedrenoRenderer.getUniqueIdentifier() }) {
                    FreedrenoRenderer.getUniqueIdentifier()
                } else {
                    NGGL4ESRenderer.getUniqueIdentifier()
                }
            }

            // Mali / Tensor / Dimensity with modern Vulkan
            hasVulkan -> {
                val renderers = Renderers.getRenderers()
                if (renderers.any { it.getUniqueIdentifier() == KopperZinkRenderer.getUniqueIdentifier() }) {
                    KopperZinkRenderer.getUniqueIdentifier()
                } else {
                    NGGL4ESRenderer.getUniqueIdentifier()
                }
            }

            // Fallback for devices without modern Vulkan
            else -> {
                NGGL4ESRenderer.getUniqueIdentifier()
            }
        }
    }

    /**
     * Performs complete auto-setup, applying best OpenGL, memory, and speed settings.
     */
    fun detectAndApplyBestSetup(context: Context): SetupResult {
        Renderers.init()
        val totalRam = getTotalPhysicalRamMb(context)
        val optimalRam = computeOptimalRamMb(totalRam)
        val bestRendererId = detectBestRenderer(context)
        val gpuVendor = detectGpuSocVendor()
        val hasVulkan = isVulkanSupported(context)

        // 1. Apply optimal RAM
        AllSettings.ramAllocation.save(optimalRam)

        // 2. Apply best OpenGL renderer
        AllSettings.renderer.save(bestRendererId)
        try {
            Renderers.setCurrentRenderer(bestRendererId)
        } catch (e: Exception) {
            Logger.warning(TAG, "Failed to set current renderer to $bestRendererId", e)
        }

        // 3. Apply native speed JVM arguments
        AllSettings.jvmArgs.save(NATIVE_SPEED_JVM_ARGS)

        val rendererName = Renderers.getRenderers().find { it.getUniqueIdentifier() == bestRendererId }?.getRendererName() ?: bestRendererId

        Logger.info(TAG, "Auto-Setup Applied: GPU=$gpuVendor, RAM=$optimalRam MB / $totalRam MB, Renderer=$rendererName, Vulkan=$hasVulkan")

        return SetupResult(
            gpuVendor = gpuVendor,
            totalRamMb = totalRam,
            recommendedRamMb = optimalRam,
            recommendedRenderer = bestRendererId,
            recommendedRendererName = rendererName,
            vulkanSupported = hasVulkan,
            multithreadedGlEnabled = true,
            jvmArgsApplied = NATIVE_SPEED_JVM_ARGS
        )
    }

    /**
     * Applies a chosen Performance Preset.
     */
    fun applyPreset(preset: PerformancePreset, context: Context) {
        val totalRam = getTotalPhysicalRamMb(context)
        val baseRam = computeOptimalRamMb(totalRam)

        when (preset) {
            PerformancePreset.NATIVE_SPEED -> {
                AllSettings.ramAllocation.save(baseRam)
                AllSettings.jvmArgs.save(NATIVE_SPEED_JVM_ARGS)
                val bestRenderer = detectBestRenderer(context)
                AllSettings.renderer.save(bestRenderer)
            }
            PerformancePreset.BALANCED -> {
                AllSettings.ramAllocation.save(min(baseRam, 3072))
                AllSettings.jvmArgs.save(BALANCED_JVM_ARGS)
            }
            PerformancePreset.POWER_SAVER -> {
                AllSettings.ramAllocation.save(min(baseRam, 1536))
                AllSettings.jvmArgs.save(LOW_END_JVM_ARGS)
                AllSettings.renderer.save(GL4ESRenderer.getUniqueIdentifier())
            }
        }
    }
}
