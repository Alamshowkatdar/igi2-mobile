package com.igi2.mobile.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.igi2.mobile.data.NativePort
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.igi2.mobile.data.Lan
import com.igi2.mobile.data.LanBrowser
import com.igi2.mobile.data.LanClient
import com.igi2.mobile.data.LanHost
import com.igi2.mobile.data.LanSession

@Composable
fun LanScreen(onBack: () -> Unit, onStartGame: (role: String, name: String, host: String) -> Unit) {
    val scope = rememberCoroutineScope()
    val host = remember { LanHost(scope) }
    val browser = remember { LanBrowser(scope) }
    val client = remember { LanClient(scope) }

    val hostState by host.state.collectAsState()
    val sessions by browser.sessions.collectAsState()
    val joinState by client.state.collectAsState()

    var playerName by rememberSaveable { mutableStateOf(Lan.defaultName().take(24)) }
    var manualAddress by rememberSaveable { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            client.leave()
            host.stop()
            browser.stop()
        }
    }

    Column(Modifier.fillMaxSize().background(Igi2Black)) {
        TopBar("LAN LOBBY", onBack)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            TerminalPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("FOUNDATION")
                    Grow()
                    StatusPill("", if (hostState.running || joinState.connected) "LIVE" else "BETA", StatusTone.LIVE)
                }
                Spacer(Modifier.height(8.dp))
                BodyText(
                    "This is a custom OpenIGI LAN protocol. It discovers, hosts and joins other " +
                        "phones running this app. It is not the retail protocol, so it cannot " +
                        "join a PC running the original I.G.I. 2, and in-game sync is not " +
                        "wired up yet."
                )
            }

            Spacer(Modifier.height(14.dp))
            TerminalPanel {
                SectionLabel("CALLSIGN")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = playerName,
                    onValueChange = { playerName = it.take(24) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !hostState.running && !joinState.connected && !joinState.joining,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Igi2Text
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Igi2Amber,
                        unfocusedBorderColor = Igi2Dim.copy(alpha = 0.4f),
                        focusedTextColor = Igi2Text,
                        unfocusedTextColor = Igi2Text,
                        disabledTextColor = Igi2Dim,
                        cursorColor = Igi2Amber,
                        focusedContainerColor = Igi2Surface2,
                        unfocusedContainerColor = Igi2Surface2,
                        disabledContainerColor = Igi2Surface2
                    )
                )
            }

            Spacer(Modifier.height(14.dp))

            when {
                hostState.running -> HostingPanel(
                    playerName = playerName,
                    sessionId = hostState.sessionId,
                    address = hostState.address,
                    port = hostState.port,
                    localAddresses = Lan.ipv4Addresses(),
                    players = listOf(playerName) + hostState.players.map { it.name },
                    onStop = { host.stop() },
                    onStartGame = { onStartGame("host", playerName, hostState.address) }
                )

                joinState.connected || joinState.joining -> LobbyPanel(
                    joining = joinState.joining,
                    hostName = joinState.hostName,
                    address = joinState.address,
                    port = joinState.port,
                    players = joinState.players,
                    error = joinState.error,
                    onLeave = { client.leave() },
                    onStartGame = { onStartGame("client", playerName, joinState.address) }
                )

                else -> JoinPanel(
                    sessions = sessions,
                    manualAddress = manualAddress,
                    onManualAddress = { manualAddress = it },
                    error = joinState.error,
                    onScan = { browser.scan() },
                    onHost = {
                        if (host.start(playerName.ifBlank { "Player" })) {
                            browser.stop()
                        }
                    },
                    onJoin = { session ->
                        client.join(session.address, playerName.ifBlank { "Player" }, session.port)
                    },
                    onJoinManual = {
                        client.join(manualAddress, playerName.ifBlank { "Player" })
                    }
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "UDP ${Lan.PORT}  //  protocol v${Lan.VERSION}  //  both ends must run this app",
                color = Igi2Dim,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HostingPanel(
    playerName: String,
    sessionId: String,
    address: String,
    port: Int,
    localAddresses: List<String>,
    players: List<String>,
    onStop: () -> Unit,
    onStartGame: () -> Unit
) {
    TerminalPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("HOSTING")
            Grow()
            StatusPill("", "OPEN", StatusTone.GOOD)
        }
        Spacer(Modifier.height(10.dp))
        KeyValueRow("Session", sessionId)
        KeyValueRow("Address", "$address:$port", Igi2Cyan)
        if (localAddresses.size > 1) {
            KeyValueRow("Also", localAddresses.filter { it != address }.joinToString(", "))
        }
        Spacer(Modifier.height(10.dp))
        SectionLabel("PLAYERS (${players.size}/${Lan.MAX_PLAYERS})")
        Spacer(Modifier.height(6.dp))
        players.forEachIndexed { index, name ->
            KeyValueRow(if (index == 0) "Host" else "Peer ${index + 1}", name)
        }
        Spacer(Modifier.height(12.dp))
        BodyText(
            "Keep this screen open. Another phone on the same Wi-Fi can scan for this " +
                "session. The host roster updates as peers join and time out.",
            color = Igi2Dim,
            size = 11,
            lineHeight = 16
        )
        Spacer(Modifier.height(12.dp))
        Igi2Button(text = "START GAME", primary = true, onClick = onStartGame)
        Spacer(Modifier.height(8.dp))
        BodyText(
            "Both peers must press START GAME. Gameplay is not synchronised yet: this " +
                "launches the engine on each device.",
            color = Igi2Dim,
            size = 11,
            lineHeight = 16
        )
        Spacer(Modifier.height(12.dp))
        Igi2Button(text = "STOP HOSTING", onClick = onStop)
    }
}

@Composable
private fun LobbyPanel(
    joining: Boolean,
    hostName: String,
    address: String,
    port: Int,
    players: List<String>,
    error: String?,
    onLeave: () -> Unit,
    onStartGame: () -> Unit
) {
    TerminalPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(if (joining) "CONNECTING" else "IN LOBBY")
            Grow()
            StatusPill("", if (joining) "WAIT" else "LIVE", if (joining) StatusTone.WAIT else StatusTone.GOOD)
        }
        Spacer(Modifier.height(10.dp))
        KeyValueRow("Host", hostName.ifBlank { address })
        KeyValueRow("Address", "$address:$port", Igi2Cyan)
        if (!joining) {
            Spacer(Modifier.height(10.dp))
            SectionLabel("PLAYERS")
            Spacer(Modifier.height(6.dp))
            if (players.isEmpty()) {
                Text(
                    "Waiting for the host roster…",
                    color = Igi2Dim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            } else {
                players.forEach { KeyValueRow("•", it) }
            }
        }
        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = Igi2Red, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.height(12.dp))
        BodyText(
            "A lobby connection is held open with a heartbeat. Gameplay sync is the next " +
                "step; this screen proves discovery and the session handshake.",
            color = Igi2Dim,
            size = 11,
            lineHeight = 16
        )
        if (!joining) {
            Spacer(Modifier.height(12.dp))
            Igi2Button(text = "START GAME", primary = true, onClick = onStartGame)
            Spacer(Modifier.height(8.dp))
            BodyText(
                "Both peers must press START GAME. Gameplay is not synchronised yet: this " +
                    "launches the engine on each device.",
                color = Igi2Dim,
                size = 11,
                lineHeight = 16
            )
        }
        Spacer(Modifier.height(12.dp))
        Igi2Button(text = "LEAVE LOBBY", onClick = onLeave)
    }
}

@Composable
private fun JoinPanel(
    sessions: List<LanSession>,
    manualAddress: String,
    onManualAddress: (String) -> Unit,
    error: String?,
    onScan: () -> Unit,
    onHost: () -> Unit,
    onJoin: (LanSession) -> Unit,
    onJoinManual: () -> Unit
) {
    TerminalPanel {
        SectionLabel("HOST OR JOIN")
        Spacer(Modifier.height(10.dp))
        Igi2Button(text = "HOST A SESSION", primary = true, onClick = onHost)
        Spacer(Modifier.height(10.dp))
        Igi2Button(text = "SCAN LAN", onClick = onScan)
        Spacer(Modifier.height(10.dp))
        BodyText(
            "Scanning sends a UDP broadcast on port ${Lan.PORT}. If the router blocks " +
                "broadcast, join by IP instead.",
            color = Igi2Dim,
            size = 11,
            lineHeight = 16
        )
    }

    Spacer(Modifier.height(14.dp))
    TerminalPanel {
        SectionLabel("FOUND SESSIONS")
        Spacer(Modifier.height(8.dp))
        if (sessions.isEmpty()) {
            Text(
                "No sessions yet. Press SCAN LAN with the host on the same Wi-Fi.",
                color = Igi2Dim,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        } else {
            sessions.forEach { session ->
                SessionsRow(session = session, onClick = { onJoin(session) })
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    TerminalPanel {
        SectionLabel("JOIN BY IP")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = manualAddress,
            onValueChange = onManualAddress,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Igi2Text
            ),
            placeholder = {
                Text(
                    "192.168.1.42",
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
            text = "JOIN",
            enabled = manualAddress.isNotBlank(),
            onClick = onJoinManual
        )
        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = Igi2Red, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun SessionsRow(session: LanSession, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Igi2Surface2, RoundedCornerShape(2.dp))
            .border(1.dp, Igi2Amber.copy(alpha = 0.24f), RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                session.hostName,
                color = Igi2Text,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${session.address}:${session.port}  //  ${session.players}/${session.maxPlayers}",
                color = Igi2Dim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
        Text(
            "JOIN >",
            color = Igi2Cyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
    }
}
