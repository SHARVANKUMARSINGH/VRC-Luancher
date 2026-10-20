package com.movtery.zalithlauncher.ui.screens.content.minecraft

import android.content.Context
import android.widget.Toast
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersions
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.download.game.GameInstaller
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object InstallationDownloadManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _activeInstaller = MutableStateFlow<GameInstaller?>(null)
    val activeInstaller = _activeInstaller.asStateFlow()

    private val _showForegroundDialog = MutableStateFlow(false)
    val showForegroundDialog = _showForegroundDialog.asStateFlow()

    private val _activeInstallationName = MutableStateFlow<String?>(null)
    val activeInstallationName = _activeInstallationName.asStateFlow()

    fun dismissToBackground() {
        _showForegroundDialog.value = false
    }

    fun showDialog() {
        if (_activeInstaller.value != null) {
            _showForegroundDialog.value = true
        }
    }

    fun cancelActiveInstallation() {
        _activeInstaller.value = null
        _showForegroundDialog.value = false
        _activeInstallationName.value = null
    }

    fun startInstallation(
        context: Context,
        name: String,
        version: String,
        loader: String,
        iconRes: Int,
        onSuccess: (String) -> Unit = {}
    ) {
        val finalVersionName = if (name.isBlank()) "$version $loader" else name
        _activeInstallationName.value = finalVersionName
        _showForegroundDialog.value = true

        scope.launch(Dispatchers.IO) {
            try {
                var fabricVer = if (loader.equals("Fabric", ignoreCase = true)) {
                    FabricVersions.fetchFabricLoaderList(version)?.firstOrNull()
                } else null

                var forgeVer = if (loader.equals("Forge", ignoreCase = true)) {
                    ForgeVersions.fetchForgeList(version)?.firstOrNull()
                } else null

                var neoforgeVer = if (loader.equals("NeoForge", ignoreCase = true)) {
                    NeoForgeVersions.fetchNeoForgeList(gameVersion = version)?.firstOrNull()
                } else null

                var quiltVer = if (loader.equals("Quilt", ignoreCase = true)) {
                    QuiltVersions.fetchQuiltLoaderList(version)?.firstOrNull()
                } else null

                val downloadInfo = GameDownloadInfo(
                    gameVersion = version,
                    customVersionName = finalVersionName,
                    overwrite = true,
                    fabric = fabricVer,
                    forge = forgeVer,
                    neoforge = neoforgeVer,
                    quilt = quiltVer
                )

                val installer = GameInstaller(context, downloadInfo, scope)
                _activeInstaller.value = installer

                installer.installGame(
                    onInstalled = { installedVersion ->
                        saveIconToVersion(context, installedVersion, iconRes)
                        VersionsManager.refresh("NewInstallationCreated", trySetVersion = installedVersion)
                        _activeInstaller.value = null
                        _showForegroundDialog.value = false
                        _activeInstallationName.value = null
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, "Installation $installedVersion installed successfully!", Toast.LENGTH_SHORT).show()
                            onSuccess(installedVersion)
                        }
                    },
                    onError = { th ->
                        _activeInstaller.value = null
                        _showForegroundDialog.value = false
                        _activeInstallationName.value = null
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to install $finalVersionName: ${th.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    onGameAlreadyInstalled = {
                        _activeInstaller.value = null
                        _showForegroundDialog.value = false
                        _activeInstallationName.value = null
                    }
                )
            } catch (e: Throwable) {
                _activeInstaller.value = null
                _showForegroundDialog.value = false
                _activeInstallationName.value = null
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "Error setting up $loader: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
