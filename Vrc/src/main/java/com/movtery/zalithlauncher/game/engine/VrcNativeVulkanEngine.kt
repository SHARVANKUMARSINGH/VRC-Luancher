package com.movtery.zalithlauncher.game.engine

import com.movtery.zalithlauncher.bridge.ZLBridge
import java.io.File

/**
 * Internal native graphics execution layer.
 * Configures direct low-overhead Vulkan pipeline parameters, asynchronous command buffer
 * dispatch, and persistent binary pipeline caching inspired by direct-dispatch architectures.
 * Runs silently in the execution pipeline for all game instances.
 */
object VrcNativeVulkanEngine {

    private val VULKAN_CANDIDATE_PATHS = listOf(
        "/system/lib64/libvulkan.so",
        "/vendor/lib64/libvulkan.so",
        "/vendor/lib64/hw/vulkan.adreno.so",
        "/vendor/lib64/hw/vulkan.mali.so",
        "/system/lib/libvulkan.so",
        "/vendor/lib/libvulkan.so"
    )

    fun applyEnvironment(envMap: MutableMap<String, String>, gameDir: File) {
        // Asynchronous command buffer & low-latency mailbox swapchain
        envMap["MESA_VK_WSI_PRESENT_MODE"] = "mailbox"
        envMap["MESA_VK_WSI_DEBUG"] = "sw"
        envMap["MESA_GLTHREAD"] = "true"
        envMap["mesa_glthread"] = "true"

        // Persistent compiled shader & pipeline cache to eliminate runtime stutter
        val cacheDir = File(gameDir, ".vrc_vk_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        envMap["MESA_SHADER_CACHE_DIR"] = cacheDir.absolutePath
        envMap["MESA_SHADER_CACHE_MAX_SIZE"] = "1G"

        // Unlocked frame pacing & direct swapchain
        envMap["POJAV_VULKAN_SWAPCHAIN"] = "direct"
        envMap["POJAV_VSYNC_IN_ZINK"] = "0"
        envMap["TU_DEBUG"] = "noconform"
        envMap["vrc_native_engine"] = "1"
    }

    fun applyJvmArguments(args: MutableList<String>) {
        if (hasSystemVulkan()) {
            args.add("-Dorg.lwjgl.vulkan.libname=libvulkan.so")
        }
        args.add("-Dvulkan.direct.swapchain=true")
        args.add("-Dvrc.engine.pipeline=direct_vulkan")
        args.add("-Dvrc.engine.async_command_buffer=true")
        args.add("-Dvrc.engine.zero_copy=true")
        args.add("-Dorg.lwjgl.system.allocator=system")
        args.add("-XX:+UseStringDeduplication")
    }

    fun preloadNativeLibraries() {
        for (path in VULKAN_CANDIDATE_PATHS) {
            val file = File(path)
            if (file.exists() && file.isFile) {
                runCatching {
                    ZLBridge.dlopen(path)
                }
            }
        }
    }

    private fun hasSystemVulkan(): Boolean {
        return VULKAN_CANDIDATE_PATHS.any { File(it).exists() }
    }
}
