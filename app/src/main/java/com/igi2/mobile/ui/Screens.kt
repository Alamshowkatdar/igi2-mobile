package com.igi2.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.igi2.mobile.data.AppState
import com.igi2.mobile.data.Mission
import com.igi2.mobile.data.Missions
import com.igi2.mobile.data.PortChoice
import com.igi2.mobile.data.WebPortState
import java.util.Locale

object Legal {
    const val TEXT = "IGI 2 Mobile is an unofficial, fan-made companion for " +
        "Project I.G.I. 2: Covert Strike. It is not affiliated with, endorsed by, or " +
        "sponsored by Innerloop Studios, Codemasters, the current rights holders, " +
        "playigi.com, or the authors of the embedded engine.\n\n" +
        "This app contains an OpenIGI-based engine distributed under GPL-3.0, but it " +
        "bundles no game data. You must own a legitimate copy of the PC game, and the " +
        "engine copies your own pc folder into app-private storage. Project I.G.I. 2 " +
        "and all game content remain the property of their respective rights holders.\n\n" +
        "Mission briefing text is quoted from playigi.com for reference. Do not " +
        "redistribute game data."
}

@Composable
fun HomeScreen(
    state: AppState,
    playLabel: String,
    onPlay: () -> Unit,
    onMissions: () -> Unit,
    onSetup: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onRefresh: () -> Unit
) {
    val nativeValue = when {
        state.native != null -> buildString {
            append("READY")
            state.native.versionName?.takeIf { it.isNotBlank() }?.let { append("  v$it") }
        }
        state.nativeChecked -> "UNSUPPORTED CPU"
        else -> "CHECKING"
    }
    val nativeTone = when {
        state.native != null -> StatusTone.GOOD
        state.nativeChecked -> StatusTone.WAIT
        else -> StatusTone.WAIT
    }
    val webValue = when (state.web.state) {
        WebPortState.LIVE -> "LIVE"
        WebPortState.SOON -> "COMING SOON"
        WebPortState.CHECKING -> "PROBING"
        WebPortState.UNKNOWN -> if (state.webConfigured) "URL SET" else "NOT CHECKED"
    }
    val webTone = when (state.web.state) {
        WebPortState.LIVE -> StatusTone.LIVE
        WebPortState.SOON -> StatusTone.WAIT
        else -> StatusTone.WAIT
    }
    val choiceValue = when (state.choice) {
        PortChoice.AUTO -> "AUTO"
        PortChoice.NATIVE -> "BUILT-IN ENGINE"
        PortChoice.WEB -> "BROWSER PORT"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Igi2Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Spacer(Modifier.height(26.dp))
        Text(
            text = "PROJECT I.G.I. 2",
            style = MaterialTheme.typography.displaySmall,
            color = Igi2Amber
        )
        Text(
            text = "COVERT STRIKE  //  MOBILE",
            color = Igi2Cyan.copy(alpha = 0.85f),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Igi2Amber.copy(alpha = 0.22f))
        Spacer(Modifier.height(20.dp))

        TerminalPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("PORT STATUS")
                Grow()
                TextButton(onClick = onRefresh, enabled = !state.checking) {
                    Text(
                        text = if (state.checking) "PROBING…" else "RE-CHECK",
                        color = if (state.checking) Igi2Dim else Igi2Cyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Built-in engine",
                    color = Igi2Dim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                StatusPill("", nativeValue, nativeTone)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Browser port",
                    color = Igi2Dim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                StatusPill("", webValue, webTone)
            }
            Spacer(Modifier.height(10.dp))
            KeyValueRow("Preferred", choiceValue, Igi2Text)
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    state.native != null ->
                        "The engine is inside this app. PLAY opens it; your game files and progress stay on this phone."
                    state.web.state == WebPortState.LIVE ->
                        "The playigi.com browser port is live. PLAY opens it full screen."
                    else ->
                        "This build needs an ARM64 device. Use SETUP GUIDE to see the requirements and the optional browser fallback."
                },
                color = Igi2Dim,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        Igi2Button(
            text = playLabel,
            primary = true,
            enabled = !state.checking || state.native != null || state.web.state == WebPortState.LIVE,
            onClick = onPlay
        )
        Spacer(Modifier.height(10.dp))
        Igi2Button(text = "MISSION BRIEFINGS", onClick = onMissions)
        Spacer(Modifier.height(10.dp))
        Igi2Button(text = "SETUP GUIDE", onClick = onSetup)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Igi2Button(text = "SETTINGS", modifier = Modifier.weight(1f), onClick = onSettings)
            Igi2Button(text = "ABOUT", modifier = Modifier.weight(1f), onClick = onAbout)
        }

        Spacer(Modifier.height(22.dp))
        HorizontalDivider(color = Igi2Amber.copy(alpha = 0.14f))
        Spacer(Modifier.height(12.dp))
        Text(
            text = "An unofficial companion. No game data is bundled. I.G.I. 2 and all game " +
                "content belong to their rights holders.",
            color = Igi2Dim,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun MissionsScreen(
    onBack: () -> Unit,
    onOpenBriefings: () -> Unit
) {
    var expanded by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize().background(Igi2Black)) {
        TopBar("MISSION BRIEFINGS", onBack)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            TerminalPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("19 MISSIONS")
                    Grow()
                    Text(
                        "3 CAMPAIGNS",
                        color = Igi2Cyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                BodyText(
                    "Every published briefing for Project I.G.I. 2: Covert Strike, bundled as " +
                        "plain text so it works offline. Tap a mission to read its objectives."
                )
                Spacer(Modifier.height(6.dp))
                TextLink("OPEN ON PLAYIGI.COM", onClick = onOpenBriefings)
            }

            Spacer(Modifier.height(18.dp))

            Missions.campaigns.forEach { campaign ->
                Spacer(Modifier.height(6.dp))
                SectionLabel(
                    "CAMPAIGN $campaign  //  ${Missions.campaignTitle(campaign)}",
                    color = Igi2Amber
                )
                Text(
                    Missions.campaignRange(campaign),
                    color = Igi2Dim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Missions.forCampaign(campaign).forEach { mission ->
                    MissionCard(
                        mission = mission,
                        expanded = expanded == mission.number,
                        onToggle = {
                            expanded = if (expanded == mission.number) null else mission.number
                        }
                    )
                    Spacer(Modifier.height(7.dp))
                }
                Spacer(Modifier.height(14.dp))
            }

            Text(
                "Briefing text: playigi.com. Objectives are listed as published and are not " +
                    "extracted from the game.",
                color = Igi2Dim,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MissionCard(mission: Mission, expanded: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (expanded) Igi2Surface2 else Igi2Surface,
                RoundedCornerShape(2.dp)
            )
            .border(
                1.dp,
                if (expanded) Igi2Amber.copy(alpha = 0.45f) else Igi2Dim.copy(alpha = 0.22f),
                RoundedCornerShape(2.dp)
            )
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format(Locale.US, "%02d", mission.number),
                color = Igi2Amber,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = mission.name,
                color = Igi2Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (expanded) "–" else "+",
                color = Igi2Cyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            mission.objectives.forEachIndexed { index, objective ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text(
                        text = "${index + 1}.",
                        color = Igi2Amber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.width(20.dp)
                    )
                    Text(
                        text = objective,
                        color = Igi2Text,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SetupScreen(
    state: AppState,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onProjectPage: () -> Unit,
    onCheckAgain: () -> Unit,
    onAppInfo: () -> Unit,
    onOpenWeb: () -> Unit,
    onBriefings: () -> Unit
) {
    val supported = state.native != null

    Column(Modifier.fillMaxSize().background(Igi2Black)) {
        TopBar("SETUP GUIDE", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            TerminalPanel {
                SectionLabel("I.G.I. 2 ON ANDROID")
                Spacer(Modifier.height(8.dp))
                BodyText(
                    "The OpenIGI-based engine is built into this app, so there is no second " +
                        "app to install. You only need your own copy of the PC game files. " +
                        "playigi.com's browser port remains an optional fallback for when it ships."
                )
            }

            Spacer(Modifier.height(14.dp))
            TerminalPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("01  //  BUILT-IN ENGINE")
                    Grow()
                    StatusPill(
                        "",
                        if (supported) "READY" else "ARM64 REQUIRED",
                        if (supported) StatusTone.GOOD else StatusTone.WAIT
                    )
                }
                Spacer(Modifier.height(8.dp))
                BodyText(
                    "The Vulkan renderer, .NET engine and game activity are inside this APK. " +
                        "They ship no game data: the engine loads the original files from the " +
                        "folder you provide."
                )
                Spacer(Modifier.height(12.dp))
                if (supported) {
                    Igi2Button(text = "OPEN I.G.I. 2", primary = true, onClick = onPlay)
                    Spacer(Modifier.height(8.dp))
                    Igi2Button(text = "APP INFO", onClick = onAppInfo)
                } else {
                    Text(
                        "This engine is ARM64 only. This device reports a different CPU " +
                            "architecture, so the built-in game cannot start.",
                        color = Igi2Red,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Igi2Button(text = "CHECK AGAIN", onClick = onCheckAgain)
                }
                Spacer(Modifier.height(4.dp))
                TextLink("ENGINE SOURCE (GPL-3.0)", onClick = onProjectPage)
            }

            Spacer(Modifier.height(14.dp))
            TerminalPanel {
                SectionLabel("02  //  YOUR PC GAME FOLDER")
                Spacer(Modifier.height(8.dp))
                BodyText(
                    "Copy the pc folder from your I.G.I. 2 installation to the phone, for " +
                        "example to /sdcard/Download/IGI 2/pc. Use a file manager or a USB cable."
                )
                Spacer(Modifier.height(8.dp))
                KeyValueRow("Signature", "igi2.exe")
                KeyValueRow("Also required", "config.qvm, common/, missions/")
                KeyValueRow("", "menusystem/, language/")
                Spacer(Modifier.height(6.dp))
                Text(
                    "The engine verifies this structure before it enables PLAY.",
                    color = Igi2Dim,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(14.dp))
            TerminalPanel {
                SectionLabel("03  //  INSTALL THE FILES IN-APP")
                Spacer(Modifier.height(8.dp))
                BodyText(
                    "Press OPEN I.G.I. 2, then SELECT GAME FOLDER and pick the pc folder you " +
                        "copied. The engine verifies it, copies it into this app's private " +
                        "storage and enables PLAY. Later launches reuse that copy."
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Uninstalling this app removes the copied game files. Your original PC " +
                        "installation is never modified.",
                    color = Igi2Dim,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(14.dp))
            TerminalPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("OFFICIAL BROWSER PORT")
                    Grow()
                    StatusPill(
                        "",
                        when (state.web.state) {
                            WebPortState.LIVE -> "LIVE"
                            WebPortState.SOON -> "COMING SOON"
                            WebPortState.CHECKING -> "PROBING"
                            WebPortState.UNKNOWN -> "NOT CHECKED"
                        },
                        if (state.web.state == WebPortState.LIVE) StatusTone.LIVE else StatusTone.WAIT
                    )
                }
                Spacer(Modifier.height(8.dp))
                BodyText(
                    "playigi.com publishes all 19 briefings but says the sequel's browser port " +
                        "is not ready. It is a fallback only: the built-in engine does not need it. " +
                        "If the port appears at a new address, paste its URL in Settings."
                )
                Spacer(Modifier.height(12.dp))
                if (state.web.state == WebPortState.LIVE) {
                    Igi2Button(text = "PLAY IN BROWSER", primary = true, onClick = onOpenWeb)
                    Spacer(Modifier.height(8.dp))
                }
                Igi2Button(text = "CHECK AGAIN", onClick = onCheckAgain)
                Spacer(Modifier.height(4.dp))
                TextLink("MISSION BRIEFINGS", onClick = onBriefings)
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "The embedded engine is based on the OpenIGI project and is distributed under " +
                    "GPL-3.0. It is not affiliated with playigi.com or the original rights " +
                    "holders. Use your own copy of the game.",
                color = Igi2Dim,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsScreen(
    state: AppState,
    onBack: () -> Unit,
    onChoice: (PortChoice) -> Unit,
    onWebUrl: (String) -> Unit,
    onCheckWeb: () -> Unit,
    onTouch: (Boolean) -> Unit,
    onWifi: (Boolean) -> Unit,
    onClearWebCache: () -> Unit
) {
    var webDraft by remember(state.webUrl) { mutableStateOf(state.webUrl) }
    val webValue = when (state.web.state) {
        WebPortState.LIVE -> "LIVE"
        WebPortState.SOON -> "COMING SOON"
        WebPortState.CHECKING -> "PROBING"
        WebPortState.UNKNOWN -> if (state.webConfigured) "URL SET" else "NOT CHECKED"
    }

    Column(Modifier.fillMaxSize().background(Igi2Black)) {
        TopBar("SETTINGS", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            TerminalPanel {
                SectionLabel("WAY TO PLAY")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceButton(
                        label = "AUTO",
                        selected = state.choice == PortChoice.AUTO,
                        modifier = Modifier.weight(1f),
                        onClick = { onChoice(PortChoice.AUTO) }
                    )
                    ChoiceButton(
                        label = "ENGINE",
                        selected = state.choice == PortChoice.NATIVE,
                        modifier = Modifier.weight(1f),
                        onClick = { onChoice(PortChoice.NATIVE) }
                    )
                    ChoiceButton(
                        label = "WEB",
                        selected = state.choice == PortChoice.WEB,
                        modifier = Modifier.weight(1f),
                        onClick = { onChoice(PortChoice.WEB) }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "AUTO opens the built-in engine when this device supports it, otherwise " +
                        "the browser port when it is live, otherwise this setup guide.",
                    color = Igi2Dim,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(14.dp))
            TerminalPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("BROWSER PORT")
                    Grow()
                    Text(
                        webValue,
                        color = when (state.web.state) {
                            WebPortState.LIVE -> Igi2Cyan
                            else -> Igi2Dim
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = webDraft,
                    onValueChange = { webDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Igi2Text
                    ),
                    placeholder = {
                        Text(
                            "https://playigi.com/...",
                            color = Igi2Dim,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Igi2Amber,
                        unfocusedBorderColor = Igi2Dim.copy(alpha = 0.4f),
                        focusedTextColor = Igi2Text,
                        unfocusedTextColor = Igi2Text,
                        cursorColor = Igi2Amber,
                        focusedContainerColor = Igi2Surface2,
                        unfocusedContainerColor = Igi2Surface2
                    )
                )
                Spacer(Modifier.height(10.dp))
                Igi2Button(
                    text = "SAVE & CHECK",
                    primary = true,
                    enabled = !state.checking,
                    onClick = {
                        onWebUrl(webDraft)
                        onCheckWeb()
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Leave empty to probe playigi.com automatically. Paste a URL when the " +
                        "official port moves or when you host a build yourself.",
                    color = Igi2Dim,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.height(14.dp))
            TerminalPanel {
                SectionLabel("CONTROLS")
                Spacer(Modifier.height(4.dp))
                ToggleRow(
                    title = "Touch controls",
                    subtitle = "Ask the browser port for its on-screen controls by adding touch=1.",
                    checked = state.touchControls,
                    onChange = onTouch
                )
            }

            Spacer(Modifier.height(14.dp))
            TerminalPanel {
                SectionLabel("DOWNLOADS")
                Spacer(Modifier.height(4.dp))
                ToggleRow(
                    title = "Wi-Fi only",
                    subtitle = "Saved for offline downloads; the launcher itself only checks status.",
                    checked = state.wifiOnly,
                    onChange = onWifi
                )
            }

            Spacer(Modifier.height(14.dp))
            TerminalPanel {
                SectionLabel("STORAGE")
                Spacer(Modifier.height(10.dp))
                Igi2Button(text = "CLEAR WEB CACHE", onClick = onClearWebCache)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Clears cookies and cached pages used by the in-app browser player. It does " +
                        "not touch the built-in engine or your copied game files.",
                    color = Igi2Dim,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChoiceButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(
            1.dp,
            if (selected) Igi2Amber else Igi2Dim.copy(alpha = 0.4f)
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Igi2Amber else Color.Transparent,
            contentColor = if (selected) Color.Black else Igi2Dim
        )
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Igi2Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Igi2Dim, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Igi2Amber,
                uncheckedThumbColor = Igi2Dim,
                uncheckedTrackColor = Igi2Surface2,
                uncheckedBorderColor = Igi2Dim.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun AboutScreen(
    version: String,
    state: AppState,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Igi2Black)) {
        TopBar("ABOUT", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text("IGI 2 MOBILE", style = MaterialTheme.typography.headlineSmall, color = Igi2Amber)
            Text(
                "Version $version  •  unofficial launcher, companion and engine",
                color = Igi2Dim,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(16.dp))

            TerminalPanel {
                SectionLabel("WHAT THIS IS")
                Spacer(Modifier.height(6.dp))
                BodyText(
                    "A native Kotlin launcher and embedded OpenIGI engine for Project I.G.I. 2: " +
                        "Covert Strike on Android. It plays the game from your own PC files, " +
                        "keeps all 19 campaign briefings offline, and can also open playigi.com's " +
                        "browser port full screen when that port ships."
                )
            }

            Spacer(Modifier.height(12.dp))
            TerminalPanel {
                SectionLabel("WHAT IT ISN'T")
                Spacer(Modifier.height(6.dp))
                BodyText(
                    "It is not the game data. The engine is inside this app, but it contains " +
                        "no game assets and is not an official release. You must supply your own " +
                        "copy of the PC game files."
                )
            }

            Spacer(Modifier.height(12.dp))
            TerminalPanel {
                SectionLabel("PORT STATUS")
                Spacer(Modifier.height(6.dp))
                KeyValueRow(
                    "Built-in engine",
                    if (state.native != null) "READY" else "ARM64 REQUIRED",
                    if (state.native != null) Igi2Cyan else Igi2Dim
                )
                KeyValueRow(
                    "Browser port",
                    when (state.web.state) {
                        WebPortState.LIVE -> "LIVE"
                        WebPortState.SOON -> "COMING SOON"
                        WebPortState.CHECKING -> "PROBING"
                        WebPortState.UNKNOWN -> "NOT CHECKED"
                    },
                    if (state.web.state == WebPortState.LIVE) Igi2Cyan else Igi2Dim
                )
                KeyValueRow(
                    "Preferred",
                    when (state.choice) {
                        PortChoice.AUTO -> "AUTO"
                        PortChoice.NATIVE -> "BUILT-IN ENGINE"
                        PortChoice.WEB -> "BROWSER PORT"
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
            TerminalPanel {
                SectionLabel("CREDITS")
                Spacer(Modifier.height(6.dp))
                BodyText(
                    "Mission briefings: playigi.com\n" +
                        "Embedded engine: rayishere0 (IGI-2-Covert-Strike-Android), GPL-3.0\n" +
                        "Engine research: the OpenIGI project\n" +
                        "Original game: Innerloop Studios, published by Codemasters (2003)"
                )
            }

            Spacer(Modifier.height(12.dp))
            TerminalPanel {
                SectionLabel("LEGAL")
                Spacer(Modifier.height(6.dp))
                Text(Legal.TEXT, color = Igi2Text, fontSize = 12.sp, lineHeight = 18.sp)
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextLink("PLAYIGI") { onOpenUrl("https://playigi.com/missions.html") }
                TextLink("ENGINE SOURCE") {
                    onOpenUrl("https://github.com/rayishere0/IGI-2-Covert-Strike-Android")
                }
                TextLink("OPENIGI") { onOpenUrl("https://openigi.com") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun DisclaimerDialog(onAccept: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        containerColor = Igi2Surface,
        titleContentColor = Igi2Amber,
        textContentColor = Igi2Text,
        title = {
            Text(
                "UNOFFICIAL COMPANION",
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(Legal.TEXT, fontSize = 12.sp, lineHeight = 18.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("I UNDERSTAND", color = Igi2Amber, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onExit) {
                Text("EXIT", color = Igi2Dim, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
