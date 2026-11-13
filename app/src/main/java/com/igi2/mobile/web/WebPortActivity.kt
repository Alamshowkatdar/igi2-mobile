package com.igi2.mobile.web

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.igi2.mobile.BuildConfig
import com.igi2.mobile.data.Net

/**
 * Full-screen landscape host for playigi.com's browser port.
 *
 * The page is treated as a game: no browser chrome, no zoom, screen kept on, and back first sends
 * an Escape key into the page (the builds use it to open their pause menu) before it exits.
 */
class WebPortActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var overlay: FrameLayout
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var retryButton: Button

    private lateinit var startUrl: String
    private var mainFrameFailed = false
    private var lastBackPress = 0L
    private var pendingCameraRequest: PermissionRequest? = null

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val request = pendingCameraRequest
        pendingCameraRequest = null
        if (request == null) return@registerForActivityResult
        if (granted) {
            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        } else {
            request.deny()
            Toast.makeText(this, "Camera permission is needed to scan co-op QR codes.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (startUrl.isBlank()) {
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        hideSystemBars(root)

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            isFocusable = true
            isFocusableInTouchMode = true
        }
        root.addView(webView)

        overlay = buildOverlay()
        root.addView(
            overlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        configureWebView()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val now = SystemClock.elapsedRealtime()
                if (now - lastBackPress < 2200L) {
                    finish()
                    return
                }
                lastBackPress = now
                sendEscape()
                Toast.makeText(
                    this@WebPortActivity,
                    "Press back again to leave the game",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(startUrl)
        }
    }

    private fun buildOverlay(): FrameLayout {
        val box = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        progressBar = ProgressBar(this).apply { isIndeterminate = true }
        statusText = TextView(this).apply {
            text = "LOADING I.G.I. 2"
            setTextColor(Color.parseColor("#FFB347"))
            textSize = 16f
            letterSpacing = 0.16f
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 12)
        }
        retryButton = Button(this).apply {
            text = "RETRY"
            visibility = View.GONE
            setOnClickListener {
                showLoading()
                webView.reload()
            }
        }
        column.addView(progressBar)
        column.addView(statusText)
        column.addView(retryButton)
        box.addView(
            column,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        return box
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(false)
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            textZoom = 100
            userAgentString = Net.USER_AGENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = false
            }
            if (Build.VERSION.SDK_INT in Build.VERSION_CODES.Q..Build.VERSION_CODES.S_V2) {
                @Suppress("DEPRECATION")
                forceDark = WebSettings.FORCE_DARK_OFF
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                mainFrameFailed = false
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (!mainFrameFailed) overlay.visibility = View.GONE
                // Kill the rubber-band scroll that makes a full-screen game feel like a web page.
                view.evaluateJavascript(
                    "(function(){try{document.documentElement.style.overscrollBehavior='none';" +
                        "document.body.style.overscrollBehavior='none'}catch(e){}})()",
                    null
                )
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                return when (uri.scheme?.lowercase()) {
                    "http", "https" -> false
                    else -> openExternal(uri)
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    mainFrameFailed = true
                    showError("Could not load the browser port. ${error.description}")
                }
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                Toast.makeText(
                    this@WebPortActivity,
                    "The game renderer stopped. Reopen the app to continue.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                val host = request.origin?.host.orEmpty()
                val wantsCamera = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                val trusted = host.equals("playigi.com", ignoreCase = true) ||
                    host.endsWith(".playigi.com", ignoreCase = true) ||
                    host.equals(Uri.parse(startUrl).host.orEmpty(), ignoreCase = true)
                if (!trusted || !wantsCamera) {
                    request.deny()
                    return
                }
                val granted = ContextCompat.checkSelfPermission(
                    this@WebPortActivity,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                } else {
                    pendingCameraRequest?.deny()
                    pendingCameraRequest = request
                    cameraPermission.launch(Manifest.permission.CAMERA)
                }
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                if (pendingCameraRequest === request) pendingCameraRequest = null
            }
        }

        webView.setDownloadListener { url, _, _, _, _ -> openExternal(Uri.parse(url)) }
        webView.setOnLongClickListener { true }
    }

    private fun showLoading() {
        overlay.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        retryButton.visibility = View.GONE
        statusText.text = "LOADING I.G.I. 2"
    }

    private fun showError(message: String) {
        overlay.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        retryButton.visibility = View.VISIBLE
        statusText.text = message
    }

    private fun sendEscape() {
        webView.evaluateJavascript(
            "(function(){try{var e=new KeyboardEvent('keydown',{key:'Escape',code:'Escape'," +
                "keyCode:27,which:27,bubbles:true});document.dispatchEvent(e);" +
                "window.dispatchEvent(e)}catch(e){}})()",
            null
        )
    }

    private fun openExternal(uri: Uri): Boolean = runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    }.getOrDefault(false)

    private fun hideSystemBars(view: View) {
        WindowInsetsControllerCompat(window, view).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars(window.decorView)
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onPause() {
        if (::webView.isInitialized) webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.loadUrl("about:blank")
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_URL = "com.igi2.mobile.extra.WEB_URL"

        fun intent(context: Context, url: String): Intent =
            Intent(context, WebPortActivity::class.java).putExtra(EXTRA_URL, url)
    }
}
