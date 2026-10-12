package com.soltini.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.soltini.app.homeautomation.ui.AddDeviceScreen
import com.soltini.app.homeautomation.ui.HomeAutomationScreen
import com.soltini.app.homeautomation.ui.HomeAutomationViewModel
import com.soltini.app.overlay.OverlayService
import com.soltini.app.ui.LogScreen
import com.soltini.app.ui.MainViewModel
import com.soltini.app.ui.MiuiOnboardingScreen
import com.soltini.app.ui.PermissionsScreen
import com.soltini.app.ui.SettingsScreen
import com.soltini.app.ui.VoiceScreen
import com.soltini.app.ui.isMiuiOnboardingNeeded
import com.soltini.app.ui.theme.*

/**
 * MainActivity
 *
 * Routing logic:
 *   1. "permissions" — First launch: only asks for Microphone (required to start).
 *   2. "voice"       — Main screen with floating iridescent glass orb.
 *   3. "settings"    — Full permissions dashboard (overlay, accessibility, assistant role…).
 *   4. "logs"        — Debug log viewer.
 */
class MainActivity : ComponentActivity() {

    override fun onStop() {
        super.onStop()
        if (com.soltini.app.media.MyraPlaybackContext.isPlaying()) {
            com.soltini.app.browser.BrowserController.getInstance(applicationContext).keepWebViewAlive()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.soltini.app.util.CrashReporter.install(this)
        val pendingCrashReport: String? = com.soltini.app.util.CrashReporter.consumeLastCrash(this)
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        val homeViewModel = ViewModelProvider(this)[HomeAutomationViewModel::class.java]

        setContent {
            GeminiVoiceTheme {
                var crashReport by remember { mutableStateOf(pendingCrashReport) }
                if (crashReport != null) {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    AlertDialog(
                        onDismissRequest = { crashReport = null },
                        containerColor = GlassCardSolid,
                        shape = RoundedCornerShape(24.dp),
                        title = {
                            Text(
                                "App crashed last time",
                                color = TextCharcoal,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Surface(
                                color = CodeBlockDarkBg,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .heightIn(max = 340.dp)
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    SelectionContainer {
                                        Text(
                                            text = crashReport ?: "",
                                            color = CodeBlockTextLight,
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp,
                                            fontFamily = JetBrainsMonoFamily
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                try {
                                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    cm.setPrimaryClip(android.content.ClipData.newPlainText("Myra crash report", crashReport ?: ""))
                                    android.widget.Toast.makeText(ctx, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {}
                            }) {
                                Text("Copy", color = BadgePurpleTint, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { crashReport = null }) {
                                Text("Close", color = TextMidGray)
                            }
                        }
                    )
                }

                val hasMic = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                var currentScreen by remember {
                    mutableStateOf(
                        when {
                            !hasMic -> "permissions"
                            isMiuiOnboardingNeeded(this@MainActivity) -> "miui_onboarding"
                            else -> "voice"
                        }
                    )
                }

                LaunchedEffect(currentScreen) {
                    if (currentScreen == "voice" || currentScreen == "settings" || currentScreen == "logs" || currentScreen == "home_automation") {
                        if (Settings.canDrawOverlays(this@MainActivity)) {
                            OverlayService.start(this@MainActivity)
                        }
                    }
                }

                val showBottomNav = currentScreen in listOf("voice", "browser", "home_automation", "logs", "settings", "add_device")

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SoftBackgroundBrush)
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        bottomBar = {
                            if (showBottomNav) {
                                FloatingGlassBottomNavBar(
                                    currentScreen = currentScreen,
                                    onSelectScreen = { currentScreen = it }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding())
                        ) {
                            when (currentScreen) {
                                "permissions" -> {
                                    PermissionsScreen(
                                        onAllPermissionsGranted = {
                                            currentScreen = if (isMiuiOnboardingNeeded(this@MainActivity)) {
                                                "miui_onboarding"
                                            } else {
                                                "voice"
                                            }
                                        },
                                        modifier = Modifier
                                    )
                                }
                                "miui_onboarding" -> {
                                    MiuiOnboardingScreen(
                                        onComplete = { currentScreen = "voice" }
                                    )
                                }
                                "logs" -> {
                                    LogScreen(
                                        onBack = { currentScreen = "voice" },
                                        modifier = Modifier
                                    )
                                }
                                "settings" -> {
                                    SettingsScreen(
                                        onBack = { currentScreen = "voice" },
                                        viewModel = viewModel,
                                        onOpenHomeAutomation = { currentScreen = "home_automation" },
                                        onOpenVoiceEnrollment = { currentScreen = "voiceprint_enrollment" },
                                        modifier = Modifier
                                    )
                                }
                                "voiceprint_enrollment" -> {
                                    val voiceprintViewModel: com.soltini.app.voiceprint.ui.VoiceprintViewModel =
                                        ViewModelProvider(this@MainActivity)[com.soltini.app.voiceprint.ui.VoiceprintViewModel::class.java]
                                    com.soltini.app.voiceprint.ui.EnrollmentScreen(
                                        viewModel = voiceprintViewModel,
                                        onNavigateBack = { currentScreen = "settings" },
                                        modifier = Modifier
                                    )
                                }
                                "home_automation" -> {
                                    HomeAutomationScreen(
                                        viewModel = homeViewModel,
                                        onNavigateToAddDevice = { currentScreen = "add_device" },
                                        onBack = { currentScreen = "voice" },
                                        modifier = Modifier
                                    )
                                }
                                "add_device" -> {
                                    AddDeviceScreen(
                                        viewModel = homeViewModel,
                                        onNavigateBack = { currentScreen = "home_automation" },
                                        modifier = Modifier
                                    )
                                }
                                "browser" -> {
                                    com.soltini.app.browser.MyraBrowserScreen(
                                        onOpenVoiceAi = { currentScreen = "voice" },
                                        modifier = Modifier
                                    )
                                }
                                else -> {
                                    VoiceScreen(
                                        viewModel = viewModel,
                                        onOpenLogs = { currentScreen = "logs" },
                                        onOpenSettings = { currentScreen = "settings" },
                                        onOpenHomeAutomation = { currentScreen = "home_automation" },
                                        modifier = Modifier
                                    )
                                }
                            }

                            if (currentScreen != "browser") {
                                com.soltini.app.browser.BrowserOverlayPanel()
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun FloatingGlassBottomNavBar(
    currentScreen: String,
    onSelectScreen: (String) -> Unit
) {
    val items = remember {
        listOf(
            NavDestination("voice", "Voice AI", Icons.Default.Mic),
            NavDestination("browser", "Browser", Icons.Default.Language),
            NavDestination("home_automation", "Smart Home", Icons.Default.ElectricBolt),
            NavDestination("logs", "Logs", Icons.AutoMirrored.Filled.Article),
            NavDestination("settings", "Settings", Icons.Default.Settings)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = GlassCardWhite,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = PrimaryPurple.copy(alpha = 0.18f),
                    spotColor = PrimaryPurple.copy(alpha = 0.24f)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = if (item.route == "home_automation") {
                        currentScreen == "home_automation" || currentScreen == "add_device"
                    } else {
                        currentScreen == item.route
                    }

                    val horizontalPadding by animateDpAsState(
                        targetValue = if (isSelected) 16.dp else 12.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "navPad"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) Color.White else TextMidGray,
                        animationSpec = tween(220),
                        label = "navTint"
                    )

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .shadow(
                                            elevation = 6.dp,
                                            shape = CircleShape,
                                            ambientColor = PrimaryPurple.copy(alpha = 0.3f),
                                            spotColor = PrimaryPink.copy(alpha = 0.35f)
                                        )
                                        .background(PrimaryHorizontalBrush, CircleShape)
                                } else {
                                    Modifier.background(Color.Transparent, CircleShape)
                                }
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelectScreen(item.route) }
                            )
                            .padding(horizontal = horizontalPadding, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.label,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
