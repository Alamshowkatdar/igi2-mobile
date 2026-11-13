package com.igi2.mobile

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.igi2.mobile.data.LauncherRepository
import com.igi2.mobile.data.NativePort
import com.igi2.mobile.data.PlayTarget
import com.igi2.mobile.ui.AboutScreen
import com.igi2.mobile.ui.DisclaimerDialog
import com.igi2.mobile.ui.HomeScreen
import com.igi2.mobile.ui.Igi2Black
import com.igi2.mobile.ui.Igi2Theme
import com.igi2.mobile.ui.MissionsScreen
import com.igi2.mobile.ui.SettingsScreen
import com.igi2.mobile.ui.SetupScreen
import com.igi2.mobile.web.WebPortActivity

private enum class Screen { HOME, MISSIONS, SETUP, SETTINGS, ABOUT }

class MainActivity : ComponentActivity() {

    private lateinit var repository: LauncherRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = LauncherRepository.get(this)
        repository.warmUp()

        setContent {
            Igi2Theme {
                App(repository)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The player may have installed or removed the native port while this activity was paused.
        if (::repository.isInitialized) repository.refreshNative()
    }
}

@Composable
private fun App(repository: LauncherRepository) {
    val state by repository.state.collectAsState()
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        repository.clearMessage()
    }

    BackHandler(enabled = screen != Screen.HOME) {
        screen = Screen.HOME
    }

    val target = repository.playTarget()
    val playLabel = when (target) {
        PlayTarget.NATIVE -> "PLAY I.G.I. 2"
        PlayTarget.WEB -> "PLAY IN BROWSER"
        PlayTarget.SETUP -> "SET UP & PLAY"
    }

    val play: () -> Unit = {
        when (repository.playTarget()) {
            PlayTarget.NATIVE -> {
                if (!NativePort.launch(context)) {
                    repository.message("Could not start the built-in engine.")
                    screen = Screen.SETUP
                }
            }

            PlayTarget.WEB -> {
                val url = repository.playbackUrl()
                if (url.isNullOrBlank()) {
                    repository.message("No browser port URL is available yet.")
                    screen = Screen.SETUP
                } else {
                    context.startActivity(WebPortActivity.intent(context, url))
                }
            }

            PlayTarget.SETUP -> {
                repository.message("No playable build found. Follow the setup guide.")
                screen = Screen.SETUP
            }
        }
    }

    val openWeb: () -> Unit = {
        val url = repository.playbackUrl()
        if (url.isNullOrBlank()) {
            repository.message("The browser port is not available yet.")
        } else {
            context.startActivity(WebPortActivity.intent(context, url))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Igi2Black)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        when (screen) {
            Screen.HOME -> HomeScreen(
                state = state,
                playLabel = playLabel,
                onPlay = play,
                onMissions = { screen = Screen.MISSIONS },
                onSetup = { screen = Screen.SETUP },
                onSettings = { screen = Screen.SETTINGS },
                onAbout = { screen = Screen.ABOUT },
                onRefresh = { repository.refreshPorts() }
            )

            Screen.MISSIONS -> MissionsScreen(
                onBack = { screen = Screen.HOME },
                onOpenBriefings = { openUrl(context, "https://playigi.com/missions.html") }
            )

            Screen.SETUP -> SetupScreen(
                state = state,
                onBack = { screen = Screen.HOME },
                onPlay = play,
                onProjectPage = { NativePort.openProjectPage(context) },
                onCheckAgain = { repository.refreshPorts() },
                onAppInfo = { NativePort.openAppInfo(context) },
                onOpenWeb = openWeb,
                onBriefings = { screen = Screen.MISSIONS }
            )

            Screen.SETTINGS -> SettingsScreen(
                state = state,
                onBack = { screen = Screen.HOME },
                onChoice = { repository.setChoice(it) },
                onWebUrl = { repository.setWebUrl(it) },
                onCheckWeb = { repository.refreshPorts() },
                onTouch = { repository.setTouchControls(it) },
                onWifi = { repository.setWifiOnly(it) },
                onClearWebCache = { repository.clearWebCache() }
            )

            Screen.ABOUT -> AboutScreen(
                version = BuildConfig.VERSION_NAME,
                state = state,
                onBack = { screen = Screen.HOME },
                onOpenUrl = { openUrl(context, it) }
            )
        }
    }

    if (!state.accepted) {
        DisclaimerDialog(
            onAccept = { repository.acceptDisclaimer() },
            onExit = { (context as? ComponentActivity)?.finishAffinity() }
        )
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open that link.", Toast.LENGTH_SHORT).show()
    }
}
