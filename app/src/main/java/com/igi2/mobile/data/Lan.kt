package com.igi2.mobile.data

import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID

/**
 * The custom OpenIGI LAN protocol.
 *
 * This is deliberately its own protocol: it is not the retail I.G.I. 2 protocol and it
 * cannot talk to a PC running the original game. Both ends must run this port.
 *
 * Discovery is a UDP broadcast. A host listens on [PORT], answers discovery probes and
 * keeps a small roster of joined peers with a heartbeat.
 */
object Lan {
    const val PORT = 26099
    const val VERSION = 1
    const val MAX_PLAYERS = 8
    const val HEARTBEAT_MS = 2_000L
    const val TIMEOUT_MS = 8_000L

    private const val MAGIC = "IGI2LAN"
    internal const val TAG = "IGI2LAN"

    fun message(vararg parts: String): String =
        (listOf("$MAGIC/v$VERSION") + parts).joinToString("|")

    /** Returns the message parts after the magic/version header, or null if this is not ours. */
    fun parse(data: String): List<String>? {
        val parts = data.split('|')
        if (parts.size < 2 || parts[0] != "$MAGIC/v$VERSION") return null
        return parts
    }

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    fun decode(value: String): String =
        try {
            URLDecoder.decode(value, "UTF-8")
        } catch (_: IllegalArgumentException) {
            value
        }

    fun defaultName(): String {
        val model = Build.MODEL?.trim().orEmpty()
        return if (model.isBlank()) "Player" else model.take(24)
    }

    fun newSessionId(): String = UUID.randomUUID().toString().substring(0, 8).uppercase()

    fun localAddress(): String? = ipv4Addresses().firstOrNull()

    fun ipv4Addresses(): List<String> =
        try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .filter { !it.isLoopbackAddress }
                .mapNotNull { it.hostAddress }
                .distinct()
        } catch (_: Exception) {
            emptyList()
        }

    internal fun broadcastAddresses(): List<InetAddress> {
        val result = mutableListOf<InetAddress>()
        runCatching { InetAddress.getByName("255.255.255.255") }.getOrNull()?.let(result::add)
        try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
                .flatMap { it.interfaceAddresses }
                .mapNotNull { it.broadcast }
                .forEach(result::add)
        } catch (_: Exception) {
            // Keep the limited broadcast address.
        }
        return result.distinct()
    }
}

data class LanPeer(
    val id: String,
    val name: String,
    val address: String,
    val port: Int,
    val lastSeenMs: Long
)

data class LanHostState(
    val running: Boolean = false,
    val sessionId: String = "",
    val hostName: String = "",
    val address: String = "",
    val port: Int = Lan.PORT,
    val players: List<LanPeer> = emptyList(),
    val error: String? = null
)

data class LanSession(
    val sessionId: String,
    val hostName: String,
    val address: String,
    val port: Int,
    val players: Int,
    val maxPlayers: Int,
    val lastSeenMs: Long
)

data class LanJoinState(
    val joining: Boolean = false,
    val connected: Boolean = false,
    val sessionId: String = "",
    val hostName: String = "",
    val address: String = "",
    val port: Int = Lan.PORT,
    val players: List<String> = emptyList(),
    val error: String? = null
)

/**
 * Hosts a LAN lobby: answers discovery, accepts joins and expires peers that stop talking.
 */
class LanHost(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(LanHostState())
    val state: StateFlow<LanHostState> = _state.asStateFlow()

    private var socket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var tickJob: Job? = null
    private val players = LinkedHashMap<String, LanPeer>()
    private val lock = Any()

    fun start(hostName: String): Boolean {
        if (_state.value.running) return true
        val sock = try {
            DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(Lan.PORT))
            }
        } catch (e: Exception) {
            Log.w(Lan.TAG, "host bind failed", e)
            _state.value = LanHostState(error = "Could not open UDP ${Lan.PORT}: ${e.message}")
            return false
        }

        Log.i(Lan.TAG, "hosting on UDP ${Lan.PORT}, local=${Lan.localAddress()}")
        socket = sock
        players.clear()
        _state.value = LanHostState(
            running = true,
            sessionId = Lan.newSessionId(),
            hostName = hostName,
            address = Lan.localAddress() ?: "unknown",
            port = Lan.PORT
        )

        receiveJob = scope.launch(Dispatchers.IO) { receiveLoop(sock) }
        tickJob = scope.launch(Dispatchers.IO) { tickLoop() }
        return true
    }

    fun stop() {
        receiveJob?.cancel()
        tickJob?.cancel()
        receiveJob = null
        tickJob = null
        socket?.close()
        socket = null
        synchronized(lock) { players.clear() }
        _state.value = LanHostState()
    }

    private suspend fun receiveLoop(sock: DatagramSocket) {
        val buffer = ByteArray(2048)
        while (scope.isActive && !sock.isClosed) {
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                sock.receive(packet)
            } catch (_: SocketTimeoutException) {
                continue
            } catch (_: SocketException) {
                break
            } catch (_: IOException) {
                continue
            }

            val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
            handle(sock, packet.address, packet.port, text)
        }
    }

    private fun handle(sock: DatagramSocket, from: InetAddress, fromPort: Int, text: String) {
        val parts = Lan.parse(text) ?: return
        val state = _state.value
        if (!state.running) return
        Log.d(Lan.TAG, "host got ${parts[1]} from ${from.hostAddress}:$fromPort")

        when (parts[1]) {
            "DISCOVER" -> send(
                sock,
                Lan.message(
                    "OFFER",
                    state.sessionId,
                    Lan.encode(state.hostName),
                    players.size.toString(),
                    Lan.MAX_PLAYERS.toString()
                ),
                from,
                fromPort
            )

            "JOIN" -> {
                val id = parts.getOrNull(2) ?: return
                val name = Lan.decode(parts.getOrNull(3).orEmpty()).ifBlank { "Player" }.take(24)
                val peer = LanPeer(id, name, from.hostAddress ?: "", fromPort, System.currentTimeMillis())
                synchronized(lock) { players[id] = peer }
                Log.i(Lan.TAG, "peer joined: $name ($id) from ${from.hostAddress}:$fromPort")
                publishPlayers()
                send(sock, Lan.message("WELCOME", state.sessionId, Lan.encode(state.hostName), id), from, fromPort)
                broadcastState(sock)
            }

            "PING" -> {
                val id = parts.getOrNull(2) ?: return
                synchronized(lock) {
                    players[id]?.let { players[id] = it.copy(lastSeenMs = System.currentTimeMillis()) }
                }
            }

            "LEAVE" -> {
                val id = parts.getOrNull(2) ?: return
                val removed = synchronized(lock) { players.remove(id) != null }
                if (removed) {
                    publishPlayers()
                    broadcastState(sock)
                }
            }
        }
    }

    private suspend fun tickLoop() {
        while (scope.isActive) {
            kotlinx.coroutines.delay(Lan.HEARTBEAT_MS)
            val sock = socket ?: continue
            val cutoff = System.currentTimeMillis() - Lan.TIMEOUT_MS
            val expired = synchronized(lock) {
                val ids = players.values.filter { it.lastSeenMs < cutoff }.map { it.id }
                ids.forEach(players::remove)
                ids
            }
            if (expired.isNotEmpty()) {
                publishPlayers()
                broadcastState(sock)
            }
        }
    }

    private fun publishPlayers() {
        val snapshot = synchronized(lock) { players.values.toList() }
        _state.value = _state.value.copy(players = snapshot)
    }

    private fun broadcastState(sock: DatagramSocket) {
        val state = _state.value
        val snapshot = synchronized(lock) { players.values.toList() }
        val text = Lan.message(
            "STATE",
            state.sessionId,
            snapshot.size.toString(),
            *snapshot.map { Lan.encode(it.name) }.toTypedArray()
        )
        snapshot.forEach { peer ->
            val address = runCatching { InetAddress.getByName(peer.address) }.getOrNull() ?: return@forEach
            send(sock, text, address, peer.port)
        }
    }

    private fun send(sock: DatagramSocket, text: String, address: InetAddress, port: Int) {
        val data = text.toByteArray(Charsets.UTF_8)
        try {
            sock.send(DatagramPacket(data, data.size, address, port))
        } catch (_: IOException) {
            // A peer that vanished is cleaned up by the timeout.
        }
    }
}

/**
 * Finds hosts on the local network. Each scan is a short burst of UDP broadcasts.
 */
class LanBrowser(private val scope: CoroutineScope) {

    private val _sessions = MutableStateFlow<List<LanSession>>(emptyList())
    val sessions: StateFlow<List<LanSession>> = _sessions.asStateFlow()

    private var scanJob: Job? = null

    fun scan(durationMs: Long = 1_500) {
        scanJob?.cancel()
        scanJob = scope.launch(Dispatchers.IO) {
            val found = LinkedHashMap<String, LanSession>()
            val sock = try {
                DatagramSocket().apply {
                    broadcast = true
                    soTimeout = 200
                }
            } catch (e: Exception) {
                _sessions.value = emptyList()
                return@launch
            }

            try {
                val probe = Lan.message("DISCOVER").toByteArray(Charsets.UTF_8)
                Lan.broadcastAddresses().forEach { address ->
                    runCatching {
                        sock.send(DatagramPacket(probe, probe.size, address, Lan.PORT))
                    }
                }

                val deadline = System.currentTimeMillis() + durationMs
                val buffer = ByteArray(2048)
                while (scope.isActive && System.currentTimeMillis() < deadline) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        sock.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }

                    val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val parts = Lan.parse(text) ?: continue
                    if (parts.getOrNull(1) != "OFFER") continue

                    val sessionId = parts.getOrNull(2) ?: continue
                    found[sessionId] = LanSession(
                        sessionId = sessionId,
                        hostName = Lan.decode(parts.getOrNull(3).orEmpty()).ifBlank { "IGI 2 host" },
                        address = packet.address.hostAddress ?: "",
                        port = packet.port,
                        players = parts.getOrNull(4)?.toIntOrNull() ?: 0,
                        maxPlayers = parts.getOrNull(5)?.toIntOrNull() ?: Lan.MAX_PLAYERS,
                        lastSeenMs = System.currentTimeMillis()
                    )
                    _sessions.value = found.values.sortedBy { it.hostName }
                }
            } finally {
                sock.close()
            }
            _sessions.value = found.values.sortedBy { it.hostName }
        }
    }

    fun stop() {
        scanJob?.cancel()
        scanJob = null
    }
}

/**
 * Joins one host and holds the lobby connection open with a heartbeat.
 */
class LanClient(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(LanJoinState())
    val state: StateFlow<LanJoinState> = _state.asStateFlow()

    private val clientId = UUID.randomUUID().toString()
    private var socket: DatagramSocket? = null
    private var job: Job? = null

    fun join(address: String, playerName: String, port: Int = Lan.PORT) {
        if (_state.value.joining || _state.value.connected) return
        val target = runCatching { InetAddress.getByName(address.trim()) }.getOrNull()
        if (target == null) {
            _state.value = LanJoinState(error = "That is not a valid IP address.")
            return
        }

        Log.i(Lan.TAG, "joining $address:$port as $playerName")
        _state.value = LanJoinState(joining = true, address = address.trim(), port = port)
        job = scope.launch(Dispatchers.IO) { run(target, playerName, port) }
    }

    fun leave() {
        val state = _state.value
        if (state.connected) {
            send(Lan.message("LEAVE", clientId), state.address, state.port)
        }
        job?.cancel()
        job = null
        socket?.close()
        socket = null
        _state.value = LanJoinState()
    }

    private suspend fun run(host: InetAddress, playerName: String, port: Int) {
        val sock = try {
            DatagramSocket().apply { soTimeout = 500 }
        } catch (e: Exception) {
            _state.value = LanJoinState(error = "Could not open a UDP socket: ${e.message}")
            return
        }
        socket = sock

        try {
            send(Lan.message("JOIN", clientId, Lan.encode(playerName)), host.hostAddress ?: "", port)
            val deadline = System.currentTimeMillis() + 3_500
            var connected = false
            var lastPing = 0L
            val buffer = ByteArray(2048)

            while (scope.isActive) {
                val now = System.currentTimeMillis()
                if (!connected && now > deadline) {
                    _state.value = _state.value.copy(
                        joining = false,
                        error = "No answer from $host:$port. Check that both devices are on the same Wi-Fi."
                    )
                    break
                }
                if (connected && now - lastPing >= Lan.HEARTBEAT_MS) {
                    lastPing = now
                    send(Lan.message("PING", clientId), host.hostAddress ?: "", port)
                }

                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    sock.receive(packet)
                } catch (_: SocketTimeoutException) {
                    continue
                } catch (_: SocketException) {
                    break
                }

                val parts = Lan.parse(String(packet.data, 0, packet.length, Charsets.UTF_8)) ?: continue
                when (parts.getOrNull(1)) {
                    "WELCOME" -> {
                        connected = true
                        Log.i(Lan.TAG, "welcome from ${host.hostAddress}:$port")
                        _state.value = LanJoinState(
                            joining = false,
                            connected = true,
                            sessionId = parts.getOrNull(2).orEmpty(),
                            hostName = Lan.decode(parts.getOrNull(3).orEmpty()).ifBlank { "IGI 2 host" },
                            address = host.hostAddress ?: "",
                            port = port
                        )
                        lastPing = System.currentTimeMillis()
                    }

                    "STATE" -> {
                        if (!connected) continue
                        val names = parts.drop(4).map { Lan.decode(it) }
                        _state.value = _state.value.copy(players = names)
                    }
                }
            }
        } finally {
            sock.close()
            socket = null
            if (_state.value.joining) {
                _state.value = _state.value.copy(joining = false)
            }
        }
    }

    private fun send(text: String, address: String, port: Int) {
        val sock = socket ?: return
        val target = runCatching { InetAddress.getByName(address) }.getOrNull() ?: return
        val data = text.toByteArray(Charsets.UTF_8)
        try {
            sock.send(DatagramPacket(data, data.size, target, port))
        } catch (_: IOException) {
            // The heartbeat retries; a dead host times out in the UI.
        }
    }
}
