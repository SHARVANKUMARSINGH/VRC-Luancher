/*
 * Vrc Luancher
 * Minecraft Java Edition PC Mobile Replica Launcher UI
 */

package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.getAccountTypeName
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.MinecraftBlockCard
import com.movtery.zalithlauncher.ui.components.MinecraftButton
import com.movtery.zalithlauncher.ui.components.MinecraftButtonStyle
import com.movtery.zalithlauncher.ui.components.MinecraftPCBottomNavBar
import com.movtery.zalithlauncher.ui.guide.GuideKeys
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.CommonVersionInfoLayout
import com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace
import com.movtery.zalithlauncher.game.versioninfo.MinecraftArtworkManager
import com.movtery.zalithlauncher.ui.screens.content.minecraft.MinecraftInstallationsScreen
import com.movtery.zalithlauncher.ui.screens.content.minecraft.MinecraftModsScreen
import com.movtery.zalithlauncher.ui.screens.content.minecraft.MinecraftNewsScreen
import com.movtery.zalithlauncher.ui.screens.content.minecraft.MinecraftPatchNotesScreen
import com.movtery.zalithlauncher.ui.screens.content.minecraft.MinecraftProfileScreen
import com.movtery.zalithlauncher.ui.screens.content.minecraft.MinecraftSkinsScreen
import com.movtery.zalithlauncher.game.versioninfo.MinecraftOfficialContentManager
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.theme.MinecraftFontFamily
import com.movtery.zalithlauncher.ui.screens.content.minecraft.InstallationDownloadManager
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.ui.resolveAndroidString
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

object LauncherNavState {
    var selectedBottomNav by mutableIntStateOf(1)
    var activeTab by mutableIntStateOf(-1)
}

/**
 * Returns the appropriate official key art image resource based on the version name.
 * Supports all major versions and their respective subversions.
 */
fun getHeroArtForVersion(version: Version?): Int {
    val versionName = version?.getVersionName()?.lowercase() ?: "1.21"
    return when {
        versionName.contains("1.21") -> R.drawable.img_mc_hero_1_21
        versionName.contains("1.20") -> R.drawable.img_mc_hero_1_20
        versionName.contains("1.19") -> R.drawable.img_mc_hero_1_19
        versionName.contains("1.18") -> R.drawable.img_mc_hero_1_18
        versionName.contains("1.17") -> R.drawable.img_mc_hero_1_18
        versionName.contains("1.16") -> R.drawable.img_mc_hero_1_16
        versionName.contains("1.15") -> R.drawable.img_mc_hero_1_14
        versionName.contains("1.14") -> R.drawable.img_mc_hero_1_14
        versionName.contains("1.13") -> R.drawable.img_mc_hero_1_13
        else -> R.drawable.img_mc_hero_classic
    }
}

@Composable
fun LauncherScreen(
    backStackViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    onLaunchGame: (Version?) -> Unit,
    onOpenLink: (String) -> Unit,
    startGuideOnce: (GuideKeys.Keys) -> Unit,
) {
    // Guide intentionally omitted as requested by user to eliminate popup overlays

    BaseScreen(
        screenKey = NormalNavKey.LauncherMain,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { _ ->
        val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
        val accountsList by AccountsManager.accountsFlow.collectAsStateWithLifecycle()
        val currentVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()
        val allVersions by VersionsManager.versions.collectAsStateWithLifecycle()
        val isRefreshingVersions by VersionsManager.isRefreshing.collectAsStateWithLifecycle()
        val runningTasks by com.movtery.zalithlauncher.coroutine.TaskSystem.tasksFlow.collectAsStateWithLifecycle()

        // Active navigation state from singleton so any screen can trigger tab change
        val activeTab = LauncherNavState.activeTab
        val selectedBottomNav = LauncherNavState.selectedBottomNav

        var showVersionMenu by remember { mutableStateOf(false) }
        var showPreSettingsDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current

        val toAccountManageScreen = {
            LauncherNavState.selectedBottomNav = 4 // Switch directly to custom Profile tab
            LauncherNavState.activeTab = -1
        }

        // Handle Android back button for sub-tab navigation:
        // When in a sub-tab (Installations, Skins, News, Mods, Profile), go back to home view
        val isInSubView = activeTab != -1 || selectedBottomNav != 1
        BackHandler(enabled = isInSubView) {
            if (activeTab != -1) {
                LauncherNavState.activeTab = -1
            } else if (selectedBottomNav != 1) {
                LauncherNavState.selectedBottomNav = 1
            }
        }

        val cyberBgGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F1522),
                Color(0xFF0A0D14),
                Color(0xFF06080D)
            )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cyberBgGradient)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Tactical Command Header (Custom VRC Cyber HUD Bar)
                VrcTacticalHeader(
                    account = account,
                    accountsList = accountsList,
                    onSelectAccount = { selectedAcc ->
                        AccountsManager.setCurrentAccount(selectedAcc)
                        Toast.makeText(context, "Switched to ${selectedAcc.username}", Toast.LENGTH_SHORT).show()
                    },
                    onManageAccounts = toAccountManageScreen,
                    onOpenMultiplayer = {
                        backStackViewModel.mainScreen.removeAndNavigateTo(
                            removes = backStackViewModel.clearBeforeNavKeys,
                            screenKey = NormalNavKey.Multiplayer
                        )
                    },
                    onOpenPreSettings = {
                        showPreSettingsDialog = true
                    }
                )

                // Main Content Switching (Unified 5-Tab Command Center: News | Play | Versions | Mods | Profile)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedBottomNav) {
                        // 0: News Screen
                        0 -> {
                            MinecraftNewsScreen(
                                onOpenLink = onOpenLink,
                                onBackClick = { LauncherNavState.selectedBottomNav = 1 }
                            )
                        }
                        // 1: Play / Command Center Screen
                        1 -> {
                            when (activeTab) {
                                1 -> {
                                    // Skins Screen
                                    MinecraftSkinsScreen(
                                        account = account,
                                        onBackClick = { LauncherNavState.activeTab = -1 }
                                    )
                                }
                                2 -> {
                                    // Patch Notes Screen
                                    MinecraftPatchNotesScreen(
                                        onOpenLink = onOpenLink,
                                        onBackClick = { LauncherNavState.activeTab = -1 }
                                    )
                                }
                                else -> {
                                    // Default Home / Play Screen
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(bottom = 6.dp)
                                    ) {
                                        // 1. Holographic Version Showcase Pod
                                        VrcHoloStage(
                                            version = currentVersion,
                                            account = account,
                                            onVersionClick = { showVersionMenu = true }
                                        )

                                        // Version Dropdown Menu
                                        Box(modifier = Modifier.padding(start = 16.dp)) {
                                            DropdownMenu(
                                                expanded = showVersionMenu,
                                                onDismissRequest = { showVersionMenu = false },
                                                modifier = Modifier
                                                    .width(280.dp)
                                                    .background(Color(0xFF0E131F))
                                                    .border(BorderStroke(1.5.dp, Color(0xFF00F0FF)), RoundedCornerShape(6.dp))
                                            ) {
                                                if (allVersions.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                text = "Latest Release (1.21.4)",
                                                                color = Color.White,
                                                                fontFamily = MinecraftFontFamily,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        },
                                                        onClick = {
                                                            allVersions.firstOrNull()?.let { VersionsManager.saveVersion(it) }
                                                            showVersionMenu = false
                                                        }
                                                    )
                                                } else {
                                                    allVersions.forEach { ver ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    CommonVersionInfoLayout(
                                                                        modifier = Modifier.weight(1f),
                                                                        version = ver,
                                                                        iconSize = 24.dp
                                                                    )
                                                                }
                                                            },
                                                            onClick = {
                                                                VersionsManager.saveVersion(ver)
                                                                showVersionMenu = false
                                                            }
                                                        )
                                                    }
                                                }

                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = "+ Manage Installations",
                                                            color = Color(0xFF00FF88),
                                                            fontFamily = MinecraftFontFamily,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    },
                                                    onClick = {
                                                        showVersionMenu = false
                                                        LauncherNavState.selectedBottomNav = 2
                                                    }
                                                )
                                            }
                                        }

                                        // 2. Hyper-Tactile Launch Ignition Console
                                        VrcLaunchIgnitionConsole(
                                            account = account,
                                            isLaunching = isRefreshingVersions,
                                            onPlayClick = {
                                                val isDownloading = runningTasks.any { it.id != com.movtery.zalithlauncher.game.account.MICROSOFT_LOGGING_TASK }
                                                if (isDownloading) {
                                                    Toast.makeText(context, "Please wait for the game to finish downloading...", Toast.LENGTH_SHORT).show()
                                                    return@VrcLaunchIgnitionConsole
                                                }
                                                if (account == null) {
                                                    Toast.makeText(context, "Please create an account first!", Toast.LENGTH_SHORT).show()
                                                    LauncherNavState.selectedBottomNav = 4
                                                    LauncherNavState.activeTab = -1
                                                    return@VrcLaunchIgnitionConsole
                                                }
                                                val hasDownloadedVersion = allVersions.any { ver ->
                                                    val clientJar = ver.getClientJar()
                                                    val mcVer = ver.getVersionInfo()?.minecraftVersion ?: ver.getVersionName()
                                                    val inheritedJar = ver.getInheritedClientJar(mcVer)
                                                    (clientJar.exists() && clientJar.length() > 0) || (inheritedJar != null && inheritedJar.exists() && inheritedJar.length() > 0)
                                                }
                                                if (!hasDownloadedVersion) {
                                                    Toast.makeText(context, "Please create an installation first!", Toast.LENGTH_SHORT).show()
                                                    LauncherNavState.selectedBottomNav = 2
                                                    return@VrcLaunchIgnitionConsole
                                                }
                                                val targetVersion = currentVersion?.takeIf { ver ->
                                                    val clientJar = ver.getClientJar()
                                                    val mcVer = ver.getVersionInfo()?.minecraftVersion ?: ver.getVersionName()
                                                    val inheritedJar = ver.getInheritedClientJar(mcVer)
                                                    (clientJar.exists() && clientJar.length() > 0) || (inheritedJar != null && inheritedJar.exists() && inheritedJar.length() > 0)
                                                } ?: allVersions.firstOrNull { ver ->
                                                    val clientJar = ver.getClientJar()
                                                    val mcVer = ver.getVersionInfo()?.minecraftVersion ?: ver.getVersionName()
                                                    val inheritedJar = ver.getInheritedClientJar(mcVer)
                                                    (clientJar.exists() && clientJar.length() > 0) || (inheritedJar != null && inheritedJar.exists() && inheritedJar.length() > 0)
                                                }
                                                if (targetVersion == null) {
                                                    Toast.makeText(context, "Please create an installation first!", Toast.LENGTH_SHORT).show()
                                                    LauncherNavState.selectedBottomNav = 2
                                                    return@VrcLaunchIgnitionConsole
                                                }
                                                onLaunchGame(targetVersion)
                                            },
                                            onConfigureControls = { showPreSettingsDialog = true },
                                            onConfigureGraphics = { showPreSettingsDialog = true },
                                            onOpenFiles = {
                                                com.movtery.zalithlauncher.filemanager.FileManagerLauncher.launch(
                                                    context = context,
                                                    rootPath = com.movtery.zalithlauncher.path.PathManager.DIR_FILES_EXTERNAL.absolutePath,
                                                    currentPath = null,
                                                    logsDir = com.movtery.zalithlauncher.path.PathManager.DIR_LAUNCHER_LOGS.absolutePath
                                                )
                                            }
                                        )

                                        // 3. 2x2 Cyber Bento Grid (Touch HUD, GPU Core, Online Hub, Mods Store)
                                        VrcCyberBentoGrid(
                                            onConfigureControls = { showPreSettingsDialog = true },
                                            onConfigureGraphics = { showPreSettingsDialog = true },
                                            onOpenMultiplayer = {
                                                backStackViewModel.mainScreen.removeAndNavigateTo(
                                                    removes = backStackViewModel.clearBeforeNavKeys,
                                                    screenKey = NormalNavKey.Multiplayer
                                                )
                                            },
                                            onOpenModsHub = { LauncherNavState.selectedBottomNav = 3 }
                                        )

                                        // 4. Horizontal Holographic Game Cartridge Deck
                                        VrcHoloCartridgeDeck(
                                            allVersions = allVersions,
                                            currentVersion = currentVersion,
                                            onSelectVersion = { ver ->
                                                VersionsManager.saveVersion(ver)
                                            },
                                            onPlayVersion = { ver ->
                                                val isDownloading = runningTasks.any { it.id != com.movtery.zalithlauncher.game.account.MICROSOFT_LOGGING_TASK }
                                                if (isDownloading) {
                                                    Toast.makeText(context, "Please wait for the game to finish downloading...", Toast.LENGTH_SHORT).show()
                                                    return@VrcHoloCartridgeDeck
                                                }
                                                if (account == null) {
                                                    Toast.makeText(context, "Please create an account first!", Toast.LENGTH_SHORT).show()
                                                    LauncherNavState.selectedBottomNav = 4
                                                    LauncherNavState.activeTab = -1
                                                    return@VrcHoloCartridgeDeck
                                                }
                                                val clientJar = ver.getClientJar()
                                                val verMc = ver.getVersionInfo()?.minecraftVersion ?: ver.getVersionName()
                                                val inheritedJar = ver.getInheritedClientJar(verMc)
                                                val isDownloaded = (clientJar.exists() && clientJar.length() > 0) || (inheritedJar != null && inheritedJar.exists() && inheritedJar.length() > 0)
                                                if (!isDownloaded) {
                                                    Toast.makeText(context, "Please create an installation first!", Toast.LENGTH_SHORT).show()
                                                    LauncherNavState.selectedBottomNav = 2
                                                    return@VrcHoloCartridgeDeck
                                                }
                                                VersionsManager.saveVersion(ver)
                                                onLaunchGame(ver)
                                            },
                                            onAddNewClick = {
                                                LauncherNavState.selectedBottomNav = 2
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // 2: Versions Screen
                        2 -> {
                            MinecraftInstallationsScreen(
                                currentVersion = currentVersion,
                                backStackViewModel = backStackViewModel,
                                onLaunchGame = onLaunchGame,
                                onBackClick = { LauncherNavState.selectedBottomNav = 1 }
                            )
                        }
                        // 3: Mods Screen
                        3 -> {
                            MinecraftModsScreen(
                                currentVersion = currentVersion,
                                backStackViewModel = backStackViewModel,
                                navigateToVersions = navigateToVersions,
                                onBackClick = { LauncherNavState.selectedBottomNav = 1 }
                            )
                        }
                        // 4: Profile Screen
                        4 -> {
                            MinecraftProfileScreen(
                                account = account,
                                backStackViewModel = backStackViewModel,
                                onBackClick = { LauncherNavState.selectedBottomNav = 1 }
                            )
                        }
                        else -> {
                            // Default fallback
                        }
                    }
                }

                // Compact Bedrock Background Download Progress Bar Docked Above Bottom Nav Bar
                CompactBackgroundDownloadBar(
                    onExpand = {
                        if (InstallationDownloadManager.activeInstaller.value != null) {
                            InstallationDownloadManager.showDialog()
                        } else {
                            com.movtery.zalithlauncher.setting.AllSettings.launcherTaskMenuExpanded.save(true)
                        }
                    }
                )

                // 1:1 Unified Command Center Bottom Navigation Bar
                // Options: News | Play | Versions | Mods | Profile
                MinecraftPCBottomNavBar(
                    selectedItem = selectedBottomNav,
                    onItemSelected = { index ->
                        LauncherNavState.selectedBottomNav = index
                        LauncherNavState.activeTab = -1
                    }
                )
            }

            if (showPreSettingsDialog) {
                com.movtery.zalithlauncher.ui.control.setup.SetupPreSettingsDialog(
                    onDismissRequest = { showPreSettingsDialog = false }
                )
            }
        }
    }
}

@Composable
private fun MinecraftLaunchLoadingOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "mc_loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cube_rotation"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111113))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Authentic 3D Minecraft Java Edition Logo
            Image(
                painter = painterResource(R.drawable.img_mc_java_logo),
                contentDescription = "Minecraft Java Edition",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(68.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(42.dp))

            // Animated Rotating Minecraft Block
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.img_minecraft),
                    contentDescription = "Loading Block",
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Minecraft Green Loading Bar
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF222224))
                    .border(BorderStroke(1.dp, Color(0xFF383838)), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(pulseAlpha)
                        .background(Color(0xFF55FF55))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "LOADING VRC...",
                color = Color(0xFFAAAAAA),
                fontFamily = MinecraftFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * VRC Tactical Command Header:
 * Floating cyber HUD bar with glowing VRC Core Emblem, real-time status beacon,
 * interactive gamer profile capsule, and quick hardware / files actions.
 */
@Composable
private fun VrcTacticalHeader(
    account: Account?,
    accountsList: List<Account>,
    onSelectAccount: (Account) -> Unit,
    onManageAccounts: () -> Unit,
    onOpenMultiplayer: () -> Unit,
    onOpenPreSettings: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showAddOfflineDialog by remember { mutableStateOf(false) }
    var offlineUsername by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "vrc_beacon")
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xF0080C14),
                        Color(0xE60D1322),
                        Color(0xD9080C14)
                    )
                )
            )
            .border(BorderStroke(1.dp, Color(0x2200F0FF)))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: VRC Core Brand Emblem & Tactical Status
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cyber Emblem Container
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F1726))
                        .border(
                            BorderStroke(
                                1.5.dp,
                                Brush.linearGradient(listOf(Color(0xFF00F0FF), Color(0xFF00FF88)))
                            ),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_vrc_emblem),
                        contentDescription = "VRC Core",
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "VRC LAUNCHER",
                        color = Color.White,
                        fontFamily = MinecraftFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FF88).copy(alpha = beaconAlpha))
                        )
                        Text(
                            text = "VULKAN NATIVE • ONLINE",
                            color = Color(0xFF00FF88),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Interactive Gamer Profile Chip & Action Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Player Profile Capsule (Tap to open account switcher dropdown)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC111827))
                        .border(BorderStroke(1.dp, Color(0x3300F0FF)), RoundedCornerShape(20.dp))
                        .clickable {
                            com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                            expanded = true
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Avatar Face
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        val skinFile = account?.getSkinFile()
                        if (account != null && skinFile != null && skinFile.exists()) {
                            PlayerFace(
                                account = account,
                                avatarSize = 24.dp
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.img_vrc_emblem),
                                contentDescription = "Player",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = account?.username ?: "Guest",
                        color = Color.White,
                        fontFamily = MinecraftFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 75.dp)
                    )

                    Icon(
                        painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                        contentDescription = null,
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Gear Settings Action (Compact 32x32dp cyber glass)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC111827))
                        .border(BorderStroke(1.dp, Color(0x3300F0FF)), RoundedCornerShape(8.dp))
                        .clickable {
                            com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                            onOpenPreSettings()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_filled),
                        contentDescription = "Setup",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Files Action (Compact 32x32dp cyber glass)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC111827))
                        .border(BorderStroke(1.dp, Color(0x338E9BAE)), RoundedCornerShape(8.dp))
                        .clickable {
                            com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                            com.movtery.zalithlauncher.filemanager.FileManagerLauncher.launch(
                                context = context,
                                rootPath = com.movtery.zalithlauncher.path.PathManager.DIR_FILES_EXTERNAL.absolutePath,
                                currentPath = null,
                                logsDir = com.movtery.zalithlauncher.path.PathManager.DIR_LAUNCHER_LOGS.absolutePath
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder_filled),
                        contentDescription = "Files",
                        tint = Color(0xFFA0B0C4),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // Account Switcher Dropdown Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF0E131F))
                .border(BorderStroke(1.5.dp, Color(0xFF00F0FF)), RoundedCornerShape(6.dp))
        ) {
            Text(
                text = "ACCOUNT SELECTOR",
                color = Color(0xFF00F0FF),
                fontFamily = MinecraftFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            if (accountsList.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "No saved accounts",
                            color = Color(0xFF888888),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 12.sp
                        )
                    },
                    onClick = {
                        expanded = false
                        onManageAccounts()
                    }
                )
            } else {
                accountsList.forEach { acc ->
                    val isCurrent = account?.uniqueUUID == acc.uniqueUUID
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerFace(account = acc, avatarSize = 22.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = acc.username,
                                        color = if (isCurrent) Color(0xFF00FF88) else Color.White,
                                        fontFamily = MinecraftFontFamily,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = getAccountTypeName(acc),
                                        color = Color(0xFF888888),
                                        fontFamily = MinecraftFontFamily,
                                        fontSize = 10.sp
                                    )
                                }
                                if (isCurrent) {
                                    Text(
                                        text = "ACTIVE",
                                        color = Color(0xFF00FF88),
                                        fontFamily = MinecraftFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelectAccount(acc)
                        }
                    )
                }
            }

            DropdownMenuItem(
                text = {
                    Text(
                        text = "+ Add Offline Account",
                        color = Color(0xFF00FF88),
                        fontFamily = MinecraftFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                },
                onClick = {
                    expanded = false
                    showAddOfflineDialog = true
                }
            )

            DropdownMenuItem(
                text = {
                    Text(
                        text = "Manage Accounts & Logins",
                        color = Color(0xFF00F0FF),
                        fontFamily = MinecraftFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp
                    )
                },
                onClick = {
                    expanded = false
                    onManageAccounts()
                }
            )
        }
    }

    if (showAddOfflineDialog) {
        AlertDialog(
            onDismissRequest = { showAddOfflineDialog = false },
            containerColor = Color(0xFF101626),
            title = {
                Text(
                    text = "ADD OFFLINE ACCOUNT",
                    color = Color.White,
                    fontFamily = MinecraftFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter username for local offline play:",
                        color = Color(0xFFCCCCCC),
                        fontFamily = MinecraftFontFamily,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = offlineUsername,
                        onValueChange = { offlineUsername = it },
                        placeholder = { Text("Username", color = Color(0xFF666666), fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF182236),
                            unfocusedContainerColor = Color(0xFF182236),
                            focusedBorderColor = Color(0xFF00F0FF),
                            unfocusedBorderColor = Color(0xFF38455A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                MinecraftButton(
                    onClick = {
                        val trimmed = offlineUsername.trim()
                        if (trimmed.isNotBlank()) {
                            coroutineScope.launch {
                                val newAcc = Account(
                                    username = trimmed,
                                    accountType = com.movtery.zalithlauncher.game.account.AccountType.LOCAL.tag
                                )
                                withContext(Dispatchers.IO) {
                                    AccountsManager.suspendSaveAccount(newAcc)
                                }
                                AccountsManager.setCurrentAccount(newAcc)
                                Toast.makeText(context, "Account '$trimmed' added!", Toast.LENGTH_SHORT).show()
                                showAddOfflineDialog = false
                                offlineUsername = ""
                            }
                        } else {
                            Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    style = MinecraftButtonStyle.GREEN,
                    text = "CREATE",
                    fontSize = 11.5.sp,
                    modifier = Modifier
                        .width(84.dp)
                        .height(34.dp)
                )
            },
            dismissButton = {
                MinecraftButton(
                    onClick = { showAddOfflineDialog = false },
                    style = MinecraftButtonStyle.STONE,
                    text = "CANCEL",
                    fontSize = 11.5.sp,
                    modifier = Modifier
                        .width(84.dp)
                        .height(34.dp)
                )
            }
        )
    }
}

/**
 * VRC Holographic Showcase Stage:
 * Displays active game version deployment with 3D pedestal, version name typography,
 * loader indicators, and integrated 4-metric real-time telemetry HUD.
 */
@Composable
private fun VrcHoloStage(
    version: Version?,
    account: Account?,
    onVersionClick: () -> Unit
) {
    val verIconFile = remember(version) { version?.getVersionIconFile() }
    val verHasCustomIcon = verIconFile != null && verIconFile.exists()
    val verFallbackIcon = remember(version) {
        version?.let { com.movtery.zalithlauncher.ui.screens.content.elements.getLoaderIconRes(it.getVersionInfo()?.loaderInfo?.loader) } ?: R.drawable.img_minecraft
    }
    val loaderName = version?.getVersionInfo()?.loaderInfo?.loader?.displayName ?: "Vanilla"
    val versionName = version?.getVersionName() ?: "Minecraft 1.21.4"
    val mcVer = version?.getVersionInfo()?.minecraftVersion ?: versionName

    val infiniteTransition = rememberInfiniteTransition(label = "rog_aura")
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    val rogRainbowBorder = Brush.horizontalGradient(
        listOf(
            Color(0xFF00F0FF).copy(alpha = auraAlpha),
            Color(0xFF00FF88).copy(alpha = auraAlpha),
            Color(0xFFA855F7).copy(alpha = auraAlpha),
            Color(0xFFFF0055).copy(alpha = auraAlpha),
            Color(0xFFFFB300).copy(alpha = auraAlpha)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF111726),
                        Color(0xFF0B101C),
                        Color(0xFF070A12)
                    )
                )
            )
            .border(BorderStroke(1.5.dp, rogRainbowBorder), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Tag Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x3300F0FF))
                            .border(BorderStroke(1.dp, Color(0x6600F0FF)), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "// ACTIVE INSTANCE",
                            color = Color(0xFF00F0FF),
                            fontFamily = MinecraftFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Specs Badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x3300FF88))
                            .border(BorderStroke(1.dp, Color(0x5500FF88)), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "VULKAN NATIVE",
                            color = Color(0xFF00FF88),
                            fontFamily = MinecraftFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x33A855F7))
                            .border(BorderStroke(1.dp, Color(0x55A855F7)), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ARM64 JIT",
                            color = Color(0xFFD8B4FE),
                            fontFamily = MinecraftFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Center Showcase: Pedestal + Version Title + Switch Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 3D Hologram Pedestal Box
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF131C2D))
                        .border(
                            BorderStroke(
                                1.5.dp,
                                Brush.linearGradient(
                                    listOf(Color(0xFF00F0FF), Color(0xFFA855F7))
                                )
                            ),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (verHasCustomIcon) {
                        AsyncImage(
                            model = verIconFile,
                            contentDescription = versionName,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Image(
                            painter = painterResource(verFallbackIcon),
                            contentDescription = versionName,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = versionName,
                        color = Color.White,
                        fontFamily = MinecraftFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xAA00F0FF),
                                offset = Offset(0f, 2f),
                                blurRadius = 4f
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when (loaderName.lowercase()) {
                                        "fabric" -> Color(0x55A855F7)
                                        "forge" -> Color(0x55FF9800)
                                        else -> Color(0x5500FF88)
                                    }
                                )
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = loaderName.uppercase(),
                                color = when (loaderName.lowercase()) {
                                    "fabric" -> Color(0xFFD8B4FE)
                                    "forge" -> Color(0xFFFFB74D)
                                    else -> Color(0xFF00FF88)
                                },
                                fontFamily = MinecraftFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Text(
                            text = "Java Edition",
                            color = Color(0xFFA0B4C8),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Switch Version Pill Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF162032))
                        .border(BorderStroke(1.dp, Color(0x6600F0FF)), RoundedCornerShape(8.dp))
                        .clickable(onClick = {
                            com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                            onVersionClick()
                        })
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "SWITCH",
                            color = Color(0xFF00F0FF),
                            fontFamily = MinecraftFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                            contentDescription = null,
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Integrated Real-Time Gamer Telemetry Bar (Scrollable to prevent narrow-screen wrapping)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x990A0E18))
                    .border(BorderStroke(1.dp, Color(0x2238455A)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    // Metric 1: Renderer
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(text = "⚡", fontSize = 9.sp)
                        Text(
                            text = "VULKAN NATIVE",
                            color = Color(0xFF00FF88),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                item {
                    // Metric 2: Memory
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(text = "🧠", fontSize = 9.sp)
                        Text(
                            text = "4.0 GB ALLOC",
                            color = Color(0xFF00F0FF),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                item {
                    // Metric 3: Controls
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(text = "🎮", fontSize = 9.sp)
                        Text(
                            text = "BEDROCK HUD",
                            color = Color(0xFFA855F7),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                item {
                    // Metric 4: Runtime
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(text = "☕", fontSize = 9.sp)
                        Text(
                            text = "JDK 21",
                            color = Color(0xFFFFB300),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * VRC Launch Ignition Console:
 * High-energy glowing cyber launch bar with animated sweeping perimeter aura,
 * dynamic launch states, and docked quick utility shortcuts.
 */
@Composable
private fun VrcLaunchIgnitionConsole(
    account: Account?,
    isLaunching: Boolean,
    onPlayClick: () -> Unit,
    onConfigureControls: () -> Unit,
    onConfigureGraphics: () -> Unit,
    onOpenFiles: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ignition_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ignition_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Main Ignition Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF0052D4),
                            Color(0xFF4364F7),
                            Color(0xFF00C6FF),
                            Color(0xFF00E676)
                        )
                    )
                )
                .border(
                    BorderStroke(2.dp, Color(0xFF00FF88).copy(alpha = pulseAlpha)),
                    RoundedCornerShape(14.dp)
                )
                .clickable {
                    com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                    onPlayClick()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLaunching) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "ENGAGING VRC ENGINE...",
                        color = Color.White,
                        fontFamily = MinecraftFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Play Glyph Pill
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(BorderStroke(1.5.dp, Color.White), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play_arrow_filled),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "LAUNCH MINECRAFT",
                            color = Color.White,
                            fontFamily = MinecraftFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 1.2.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color(0x99000000),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                        Text(
                            text = "READY TO PLAY • NATIVE ENGINE",
                            color = Color(0xEEFFFFFF),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Sub-Action Utility Dock
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Button 1: Touch Layout Editor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xDD101626))
                    .border(BorderStroke(1.dp, Color(0x3300F0FF)), RoundedCornerShape(8.dp))
                    .clickable {
                        com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                        onConfigureControls()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_videogame_asset_outlined),
                        contentDescription = null,
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "CONTROLS",
                        color = Color(0xFF00F0FF),
                        fontFamily = MinecraftFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Button 2: GPU Renderer Setup
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xDD101626))
                    .border(BorderStroke(1.dp, Color(0x3300FF88)), RoundedCornerShape(8.dp))
                    .clickable {
                        com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                        onConfigureGraphics()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_filled),
                        contentDescription = null,
                        tint = Color(0xFF00FF88),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "RENDERER",
                        color = Color(0xFF00FF88),
                        fontFamily = MinecraftFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Button 3: Files Explorer
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xDD101626))
                    .border(BorderStroke(1.dp, Color(0x33A855F7)), RoundedCornerShape(8.dp))
                    .clickable {
                        com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                        onOpenFiles()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder_filled),
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "FILES",
                        color = Color(0xFFA855F7),
                        fontFamily = MinecraftFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * VRC Cyber Bento Grid:
 * 2x2 modular matrix of interactive hardware & gaming tiles.
 */
@Composable
private fun VrcCyberBentoGrid(
    onConfigureControls: () -> Unit,
    onConfigureGraphics: () -> Unit,
    onOpenMultiplayer: () -> Unit,
    onOpenModsHub: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Touch Layout & Vulkan Engine
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tile 1: Touch HUD
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDD0E1422))
                    .border(BorderStroke(1.dp, Color(0x3300F0FF)), RoundedCornerShape(12.dp))
                    .clickable {
                        com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                        onConfigureControls()
                    }
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOUCH HUD",
                            color = Color(0xFF00F0FF),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00F0FF))
                        )
                    }
                    Column {
                        Text(
                            text = "Bedrock D-Pad",
                            color = Color.White,
                            fontFamily = MinecraftFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Custom Controls",
                            color = Color(0xFF8E9BAE),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 8.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Tile 2: Graphics Core
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDD0E1422))
                    .border(BorderStroke(1.dp, Color(0x3300FF88)), RoundedCornerShape(12.dp))
                    .clickable {
                        com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                        onConfigureGraphics()
                    }
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GPU CORE",
                            color = Color(0xFF00FF88),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FF88))
                        )
                    }
                    Column {
                        Text(
                            text = "Vulkan Native",
                            color = Color.White,
                            fontFamily = MinecraftFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Zero-Copy Pipeline",
                            color = Color(0xFF8E9BAE),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 8.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Row 2: Online Hub & Mods Store
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tile 3: Online Hub
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDD0E1422))
                    .border(BorderStroke(1.dp, Color(0x33A855F7)), RoundedCornerShape(12.dp))
                    .clickable {
                        com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                        onOpenMultiplayer()
                    }
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ONLINE HUB",
                            color = Color(0xFFA855F7),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFA855F7))
                        )
                    }
                    Column {
                        Text(
                            text = "Servers & LAN",
                            color = Color.White,
                            fontFamily = MinecraftFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Direct Join & P2P",
                            color = Color(0xFF8E9BAE),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 8.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Tile 4: Mods Store
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xDD0E1422))
                    .border(BorderStroke(1.dp, Color(0x33FFB300)), RoundedCornerShape(12.dp))
                    .clickable {
                        com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                        onOpenModsHub()
                    }
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MODS STORE",
                            color = Color(0xFFFFB300),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFB300))
                        )
                    }
                    Column {
                        Text(
                            text = "Modrinth Hub",
                            color = Color.White,
                            fontFamily = MinecraftFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Shaders & Addons",
                            color = Color(0xFF8E9BAE),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 8.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * VRC Holographic Cartridge Deck:
 * Horizontal scrolling carousel of high-tech game cartridges.
 */
@Composable
private fun VrcHoloCartridgeDeck(
    allVersions: List<Version>,
    currentVersion: Version?,
    onSelectVersion: (Version) -> Unit,
    onPlayVersion: (Version) -> Unit,
    onAddNewClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "DEPLOYED PROFILES",
                color = Color.White,
                fontFamily = MinecraftFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                softWrap = false
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x3300FF88))
                    .border(BorderStroke(1.dp, Color(0x6600FF88)), RoundedCornerShape(6.dp))
                    .clickable(onClick = {
                        com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                        onAddNewClick()
                    })
                    .padding(horizontal = 8.dp, vertical = 3.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ NEW PROFILE",
                    color = Color(0xFF00FF88),
                    fontFamily = MinecraftFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (allVersions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x990E1422))
                    .border(BorderStroke(1.dp, Color(0x3338455A)), RoundedCornerShape(12.dp))
                    .clickable(onClick = {
                        com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                        onAddNewClick()
                    }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ CREATE YOUR FIRST MINECRAFT PROFILE",
                    color = Color(0xFF00F0FF),
                    fontFamily = MinecraftFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allVersions) { ver ->
                    val isSelected = ver.getVersionName() == currentVersion?.getVersionName()
                    val iconFile = remember(ver) { ver.getVersionIconFile() }
                    val hasCustomIcon = iconFile.exists()
                    val fallbackIcon = remember(ver) {
                        com.movtery.zalithlauncher.ui.screens.content.elements.getLoaderIconRes(ver.getVersionInfo()?.loaderInfo?.loader)
                    }
                    val loaderName = ver.getVersionInfo()?.loaderInfo?.loader?.displayName ?: "Vanilla"

                    Box(
                        modifier = Modifier
                            .width(170.dp)
                            .height(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xE6111C2B) else Color(0xCC0D121E))
                            .border(
                                BorderStroke(
                                    1.5.dp,
                                    if (isSelected) Color(0xFF00FF88) else Color(0x3338455A)
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                                onSelectVersion(ver)
                            }
                            .padding(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF162032)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasCustomIcon) {
                                        AsyncImage(
                                            model = iconFile,
                                            contentDescription = ver.getVersionName(),
                                            modifier = Modifier.size(24.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(fallbackIcon),
                                            contentDescription = ver.getVersionName(),
                                            modifier = Modifier.size(24.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isSelected) Color(0x3300FF88) else Color(0x22FFFFFF))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (isSelected) "ACTIVE" else loaderName.uppercase(),
                                        color = if (isSelected) Color(0xFF00FF88) else Color(0xFFA0B4C8),
                                        fontFamily = MinecraftFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 7.5.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = ver.getVersionName(),
                                    color = Color.White,
                                    fontFamily = MinecraftFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFF0072FF))
                                        .clickable {
                                            com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                                            onPlayVersion(ver)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "PLAY",
                                        color = Color.White,
                                        fontFamily = MinecraftFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Extra item: + Add installation card at end
                item {
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x660D121E))
                            .border(BorderStroke(1.dp, Color(0x4400F0FF)), RoundedCornerShape(12.dp))
                            .clickable {
                                com.movtery.zalithlauncher.ui.sound.MinecraftSoundHelper.playClickSound()
                                onAddNewClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "+",
                                color = Color(0xFF00F0FF),
                                fontFamily = MinecraftFontFamily,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "NEW INSTANCE",
                                color = Color(0xFFA0B4C8),
                                fontFamily = MinecraftFontFamily,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pixelated Minecraft Container Border:
 * Draws outer border and authentic retro L-bracket stepped notches at each corner
 */
@Composable
private fun MinecraftPixelBorderContainer(
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0xFF3C8527),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(3.dp))
            .drawWithContent {
                drawContent()
                val bracketLen = 10.dp.toPx()
                val step = 3.dp.toPx()
                val strokeW = 2.dp.toPx()

                // Top-Left corner bracket
                drawLine(borderColor, Offset(0f, step), Offset(step, step), strokeW)
                drawLine(borderColor, Offset(step, 0f), Offset(step, step), strokeW)
                drawLine(borderColor, Offset(0f, 0f), Offset(bracketLen, 0f), strokeW)
                drawLine(borderColor, Offset(0f, 0f), Offset(0f, bracketLen), strokeW)

                // Top-Right corner bracket
                val w = size.width
                drawLine(borderColor, Offset(w - step, step), Offset(w, step), strokeW)
                drawLine(borderColor, Offset(w - step, 0f), Offset(w - step, step), strokeW)
                drawLine(borderColor, Offset(w - bracketLen, 0f), Offset(w, 0f), strokeW)
                drawLine(borderColor, Offset(w, 0f), Offset(w, bracketLen), strokeW)

                // Bottom-Left corner bracket
                val h = size.height
                drawLine(borderColor, Offset(0f, h - step), Offset(step, h - step), strokeW)
                drawLine(borderColor, Offset(step, h - step), Offset(step, h), strokeW)
                drawLine(borderColor, Offset(0f, h), Offset(bracketLen, h), strokeW)
                drawLine(borderColor, Offset(0f, h - bracketLen), Offset(0f, h), strokeW)

                // Bottom-Right corner bracket
                drawLine(borderColor, Offset(w - step, h - step), Offset(w, h - step), strokeW)
                drawLine(borderColor, Offset(w - step, h - step), Offset(w - step, h), strokeW)
                drawLine(borderColor, Offset(w - bracketLen, h), Offset(w, h), strokeW)
                drawLine(borderColor, Offset(w, h - bracketLen), Offset(w, h), strokeW)
            }
    ) {
        content()
    }
}

/**
 * Patch Notes Modal Dialog
 */
@Composable
private fun PatchNotesDialog(
    onDismiss: () -> Unit,
    onOpenChangelog: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.img_minecraft),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Minecraft 1.21.4 Patch Notes",
                    color = Color.White,
                    fontFamily = MinecraftFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Welcome to the latest release of Minecraft Java Edition!\n\n" +
                            "Tricky Trials Features:\n" +
                            "• Trial Chambers: Explore sprawling underground structures filled with perilous challenges, trial spawners, and vaults.\n" +
                            "• The Breeze: A hostile mob that leaps circles around opponents and fires wind charges.\n" +
                            "• The Crafter: A new Redstone block that enables automated crafting of items.\n" +
                            "• The Mace: A powerful new weapon delivering devastating smash attacks from high falls.\n" +
                            "• Bug fixes, performance improvements, and security enhancements for Java Edition.",
                    color = Color(0xFFCCCCCC),
                    fontFamily = MinecraftFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        },
        confirmButton = {
            MinecraftButton(
                onClick = onOpenChangelog,
                style = MinecraftButtonStyle.GREEN,
                text = "CHANGELOG",
                fontSize = 12.sp,
                modifier = Modifier
                    .width(130.dp)
                    .height(38.dp)
            )
        },
        dismissButton = {
            MinecraftButton(
                onClick = onDismiss,
                style = MinecraftButtonStyle.STONE,
                text = "CLOSE",
                fontSize = 12.sp,
                modifier = Modifier
                    .width(85.dp)
                    .height(38.dp)
            )
        }
    )
}

/**
 * News Security Advisory Modal Dialog
 */
@Composable
private fun NewsSecurityDialog(
    onDismiss: () -> Unit,
    onOpenArticle: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Text(
                text = "Security Vulnerability Advisory",
                color = Color.White,
                fontFamily = MinecraftFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Image(
                    painter = painterResource(R.drawable.img_mc_news_cave),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Important Message regarding building and running Java Edition:\n\n" +
                            "Mojang Studios and the Minecraft security team have released critical security advisories regarding network packet processing and external asset loaders.\n\n" +
                            "Vrc Luancher includes patched native libraries and secure JVM arguments to keep your game, world data, and multiplayer sessions safe.",
                    color = Color(0xFFCCCCCC),
                    fontFamily = MinecraftFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        },
        confirmButton = {
            MinecraftButton(
                onClick = onOpenArticle,
                style = MinecraftButtonStyle.GREEN,
                text = "READ ADVISORY",
                fontSize = 12.sp,
                modifier = Modifier
                    .width(140.dp)
                    .height(38.dp)
            )
        },
        dismissButton = {
            MinecraftButton(
                onClick = onDismiss,
                style = MinecraftButtonStyle.STONE,
                text = "CLOSE",
                fontSize = 12.sp,
                modifier = Modifier
                    .width(85.dp)
                    .height(38.dp)
            )
        }
    )
}

@Composable
private fun CompactBackgroundDownloadBar(
    onExpand: () -> Unit
) {
    val runningTasks by com.movtery.zalithlauncher.coroutine.TaskSystem.tasksFlow.collectAsStateWithLifecycle()
    val activeInstaller by InstallationDownloadManager.activeInstaller.collectAsStateWithLifecycle()
    val showForegroundDialog by InstallationDownloadManager.showForegroundDialog.collectAsStateWithLifecycle()
    val activeInstallName by InstallationDownloadManager.activeInstallationName.collectAsStateWithLifecycle()

    val relevantTasks = remember(runningTasks) {
        runningTasks.filter { it.id != com.movtery.zalithlauncher.game.account.MICROSOFT_LOGGING_TASK }
    }

    val hasActiveTasks = relevantTasks.isNotEmpty() || (activeInstaller != null && !showForegroundDialog)
    if (!hasActiveTasks) return

    val currentTask = if (activeInstaller != null) {
        val tasks by activeInstaller!!.tasksFlow.collectAsStateWithLifecycle()
        tasks.find { it.task.stage.value == com.movtery.zalithlauncher.coroutine.TaskStage.RUNNING }?.task
            ?: tasks.firstOrNull()?.task
    } else {
        relevantTasks.firstOrNull()
    } ?: return

    val progress by currentTask.progress.collectAsStateWithLifecycle()
    val message by currentTask.message.collectAsStateWithLifecycle()
    val speed by currentTask.rateBytesPerSec.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE6101624))
            .border(BorderStroke(1.dp, Color(0x4400FF88)), RoundedCornerShape(12.dp))
            .clickable { onExpand() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "compact_dl")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "compact_alpha"
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_download),
                contentDescription = null,
                tint = Color(0xFF55FF55).copy(alpha = alpha),
                modifier = Modifier.size(20.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dlTitle = when {
                        activeInstallName != null -> "DOWNLOADING ${activeInstallName!!.uppercase()}"
                        message != null -> resolveAndroidString(message!!).text
                        else -> "DOWNLOADING GAME FILES..."
                    }
                    Text(
                        text = dlTitle,
                        color = Color.White,
                        fontFamily = MinecraftFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    val spd = speed ?: 0L
                    if (spd > 0L) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${formatFileSize(spd)}/s",
                            color = Color(0xFF55FF55),
                            fontFamily = MinecraftFontFamily,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Progress Bar
                val progressFloat = progress.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF2A2A2D))
                ) {
                    if (progressFloat > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFloat)
                                .fillMaxHeight()
                                .background(Color(0xFF55FF55))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.35f)
                                .fillMaxHeight()
                                .background(Color(0xFF3C8527).copy(alpha = alpha))
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF2E2E32))
                    .border(BorderStroke(1.dp, Color(0xFF444448)), RoundedCornerShape(2.dp))
                    .clickable { onExpand() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VIEW",
                    color = Color.White,
                    fontFamily = MinecraftFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
