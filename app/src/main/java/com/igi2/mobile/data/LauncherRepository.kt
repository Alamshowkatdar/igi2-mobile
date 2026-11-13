package com.igi2.mobile.data

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PortChoice { AUTO, NATIVE, WEB }

enum class PlayTarget { NATIVE, WEB, SETUP }

data class AppState(
    val accepted: Boolean = false,
    val choice: PortChoice = PortChoice.AUTO,
    val native: NativePort.Install? = null,
    val nativeChecked: Boolean = false,
    val web: WebPortResult = WebPortResult(),
    val webUrl: String = "",
    val touchControls: Boolean = true,
    val wifiOnly: Boolean = true,
    val checking: Boolean = false,
    val message: String? = null
) {
    val webConfigured: Boolean get() = webUrl.isNotBlank()
}

class LauncherRepository private constructor(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("igi2_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    companion object {
        private const val TAG = "Igi2Repository"
        private const val KEY_CHOICE = "port_choice"
        private const val KEY_WEB_URL = "web_url"
        private const val KEY_TOUCH = "touch_controls"
        private const val KEY_WIFI = "wifi_only"
        private const val KEY_ACCEPTED = "accepted"

        @Volatile
        private var instance: LauncherRepository? = null

        fun get(context: Context): LauncherRepository =
            instance ?: synchronized(this) {
                instance ?: LauncherRepository(context).also { instance = it }
            }
    }

    init {
        val storedChoice = prefs.getString(KEY_CHOICE, PortChoice.AUTO.name)
        _state.update {
            it.copy(
                choice = runCatching { PortChoice.valueOf(storedChoice.orEmpty()) }
                    .getOrDefault(PortChoice.AUTO),
                webUrl = prefs.getString(KEY_WEB_URL, "").orEmpty(),
                touchControls = prefs.getBoolean(KEY_TOUCH, true),
                wifiOnly = prefs.getBoolean(KEY_WIFI, true),
                accepted = prefs.getBoolean(KEY_ACCEPTED, false)
            )
        }
    }

    fun warmUp() {
        refreshPorts()
    }

    /** Cheap package-manager check used when the activity resumes; no network probe. */
    fun refreshNative() {
        val native = NativePort.find(app)
        _state.update { it.copy(native = native, nativeChecked = true) }
    }

    /** Re-checks the built-in engine and probes the optional browser port. */
    fun refreshPorts() {
        if (_state.value.checking) return
        val native = NativePort.find(app)
        _state.update { it.copy(native = native, nativeChecked = true, checking = true) }
        scope.launch {
            val result = WebPort.probe(_state.value.webUrl)
            _state.update { it.copy(web = result, checking = false) }
        }
    }

    fun setChoice(choice: PortChoice) {
        prefs.edit().putString(KEY_CHOICE, choice.name).apply()
        _state.update { it.copy(choice = choice) }
    }

    fun setWebUrl(url: String) {
        val clean = url.trim()
        prefs.edit().putString(KEY_WEB_URL, clean).apply()
        _state.update { it.copy(webUrl = clean, web = WebPortResult()) }
    }

    fun setTouchControls(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TOUCH, enabled).apply()
        _state.update { it.copy(touchControls = enabled) }
    }

    fun setWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI, enabled).apply()
        _state.update { it.copy(wifiOnly = enabled) }
    }

    fun acceptDisclaimer() {
        prefs.edit().putBoolean(KEY_ACCEPTED, true).apply()
        _state.update { it.copy(accepted = true) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun message(text: String) {
        _state.update { it.copy(message = text) }
    }

    fun clearWebCache() {
        scope.launch {
            try {
                app.cacheDir.deleteRecursively()
                app.externalCacheDir?.deleteRecursively()
                withContext(Dispatchers.Main) {
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().flush()
                }
            } catch (error: Exception) {
                Log.w(TAG, "Could not clear the web cache", error)
            }
            _state.update { it.copy(message = "Web cache cleared.") }
        }
    }

    fun playTarget(): PlayTarget {
        val snapshot = _state.value
        val nativeReady = snapshot.native != null
        val webReady = snapshot.web.state == WebPortState.LIVE && !snapshot.web.url.isNullOrBlank()
        return when (snapshot.choice) {
            PortChoice.NATIVE -> if (nativeReady) PlayTarget.NATIVE else PlayTarget.SETUP
            PortChoice.WEB -> if (webReady) PlayTarget.WEB else PlayTarget.SETUP
            PortChoice.AUTO -> when {
                nativeReady -> PlayTarget.NATIVE
                webReady -> PlayTarget.WEB
                else -> PlayTarget.SETUP
            }
        }
    }

    /** The URL to hand to the in-app player, with the touch-controls flag if it is enabled. */
    fun playbackUrl(): String? {
        val snapshot = _state.value
        val url = snapshot.web.url ?: snapshot.webUrl.takeIf { it.isNotBlank() }
        return url?.let { WebPort.withTouch(it, snapshot.touchControls) }
    }
}
