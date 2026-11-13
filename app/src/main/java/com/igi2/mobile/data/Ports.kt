package com.igi2.mobile.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * The OpenIGI-based engine embedded in this app.
 *
 * The Java package of the engine activity is still `com.projectigi2.app` because the JNI entry
 * points in `libprojectigi.so` resolve against that name. This launcher is the only exported
 * entry point; the engine activity is launched explicitly and never appears as a second app.
 *
 * The engine is ARM64 only and still requires the player's own `pc/` game folder.
 */
object NativePort {

    const val VERSION = "2.0.0"
    const val ACTIVITY = "com.projectigi2.app.MainActivity"
    const val EXTRA_AUTO_PLAY = "auto_play"

    const val PROJECT_URL =
        "https://github.com/rayishere0/IGI-2-Covert-Strike-Android"

    data class Install(val packageName: String, val versionName: String?)

    val isSupported: Boolean
        get() = Build.SUPPORTED_ABIS.any { it.equals("arm64-v8a", ignoreCase = true) }

    fun find(context: Context): Install? =
        if (isSupported) Install(context.packageName, VERSION) else null

    fun launch(context: Context): Boolean {
        if (!isSupported) return false
        val intent = Intent()
            .setClassName(context.packageName, ACTIVITY)
            .putExtra(EXTRA_AUTO_PLAY, true)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    fun openProjectPage(context: Context) = open(context, PROJECT_URL)

    fun openAppInfo(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun open(context: Context, url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

enum class WebPortState { UNKNOWN, CHECKING, SOON, LIVE }

data class WebPortResult(
    val state: WebPortState = WebPortState.UNKNOWN,
    val url: String? = null,
    val detail: String? = null
)

/**
 * The official playigi.com browser port of I.G.I. 2.
 *
 * At the time this app was written playigi.com said the sequel's port was "coming soon" and served
 * no build. The app therefore probes a short list of candidate URLs (and an optional URL supplied in
 * Settings) and only offers the in-app browser player once one of them really serves the sequel.
 *
 * When the port goes live, add its URL here — or paste it into Settings — and the launcher picks it
 * up without an app update.
 */
object WebPort {

    val CANDIDATES = listOf(
        "https://playigi.com/game2/",
        "https://playigi.com/igi2/",
        "https://playigi.com/igi-2/",
        "https://playigi.com/covert-strike/"
    )

    private val MARKERS = listOf(
        "covert strike",
        "igi 2",
        "igi2",
        "project i.g.i. 2"
    )

    suspend fun probe(customUrl: String?): WebPortResult = withContext(Dispatchers.IO) {
        val custom = customUrl?.trim().orEmpty()
        val candidates = buildList {
            if (custom.isNotEmpty()) add(custom)
            addAll(CANDIDATES.filter { it != custom })
        }

        for (candidate in candidates) {
            val result = check(candidate, trusted = candidate == custom)
            if (result != null) return@withContext result
        }

        WebPortResult(
            state = WebPortState.SOON,
            detail = "playigi.com has not published the I.G.I. 2 browser port yet."
        )
    }

    private fun check(url: String, trusted: Boolean): WebPortResult? = try {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", Net.USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml")
            .build()

        Net.client.newBuilder()
            .callTimeout(12, TimeUnit.SECONDS)
            .build()
            .newCall(request)
            .execute()
            .use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                val lower = body.lowercase()
                // A hand-entered URL is taken at its word; only the built-in candidates have to
                // prove they are the sequel rather than the I.G.I. 1 page.
                if (!trusted && MARKERS.none { lower.contains(it) }) return null
                WebPortResult(
                    state = WebPortState.LIVE,
                    url = response.request.url.toString()
                )
            }
    } catch (_: Exception) {
        null
    }

    /** Adds the touch-controls query flag the playigi.com builds understand. */
    fun withTouch(url: String, touch: Boolean): String {
        if (!touch || url.contains("touch=1")) return url
        val separator = if (url.contains('?')) '&' else '?'
        return url + separator + "touch=1"
    }
}
