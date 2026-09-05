package com.splitview.pad

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The always-on-top browser window, in the spirit of One UI's pop-up view and
 * HyperOS's small window:
 *
 *   - drag the header to move it, and it snaps to the nearest screen edge
 *   - drag the corner grip to resize it
 *   - minimise it to a floating bubble, tap the bubble to bring it back
 *   - cycle its opacity so you can read what is underneath
 *
 * Position, size and opacity survive restarts.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: Prefs

    private var overlayView: View? = null
    private var bubbleView: View? = null
    private var webView: WebView? = null

    private lateinit var params: WindowManager.LayoutParams
    private var bubbleParams: WindowManager.LayoutParams? = null

    private var minimized = false
    private var alphaStep = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundCompat()
        buildWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra(EXTRA_OVERLAY_URL)?.takeIf { it.isNotBlank() }?.let { load(it) }
        if (minimized) restoreFromBubble()
        return START_STICKY
    }

    // ------------------------------------------------------------- window

    private fun buildWindow() {
        val view = LayoutInflater.from(this).inflate(R.layout.floating_overlay, null)
        overlayView = view

        val metrics = resources.displayMetrics
        val defaultWidth = (360 * metrics.density).toInt()
            .coerceAtMost(metrics.widthPixels - (24 * metrics.density).toInt())
        val defaultHeight = (480 * metrics.density).toInt()
            .coerceAtMost(metrics.heightPixels - (96 * metrics.density).toInt())

        params = WindowManager.LayoutParams(
            if (prefs.floatingWidth > 0) prefs.floatingWidth else defaultWidth,
            if (prefs.floatingHeight > 0) prefs.floatingHeight else defaultHeight,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.floatingX
            y = prefs.floatingY
        }

        view.alpha = prefs.floatingAlpha
        windowManager.addView(view, params)

        setupWebView(view)
        setupChrome(view)
        setupDragging(view)
        setupResizing(view)

        load(prefs.floatingUrl)
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun setupWebView(root: View) {
        val web: WebView = root.findViewById(R.id.webview_floating)
        val progress: ProgressBar = root.findViewById(R.id.pb_floating)
        val urlBar: EditText = root.findViewById(R.id.et_floating_url)
        val title: TextView = root.findViewById(R.id.tv_floating_title)
        webView = web

        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let {
                    prefs.floatingUrl = it
                    if (!urlBar.hasFocus()) urlBar.setText(UrlUtils.prettify(it))
                }
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.INVISIBLE
            }

            override fun onReceivedTitle(view: WebView?, pageTitle: String?) {
                if (!pageTitle.isNullOrBlank()) title.text = pageTitle
            }
        }
    }

    private fun setupChrome(root: View) {
        val urlBar: EditText = root.findViewById(R.id.et_floating_url)

        root.findViewById<ImageButton>(R.id.btn_floating_close).setOnClickListener { stopSelf() }
        root.findViewById<ImageButton>(R.id.btn_floating_minimize).setOnClickListener {
            minimizeToBubble()
        }
        root.findViewById<ImageButton>(R.id.btn_floating_opacity).setOnClickListener {
            cycleOpacity()
        }
        root.findViewById<ImageButton>(R.id.btn_floating_back).setOnClickListener {
            webView?.let { if (it.canGoBack()) it.goBack() }
        }
        root.findViewById<ImageButton>(R.id.btn_floating_reload).setOnClickListener {
            webView?.reload()
        }
        root.findViewById<Button>(R.id.btn_floating_go).setOnClickListener {
            load(urlBar.text.toString())
            dismissKeyboard(urlBar)
        }

        // The window is normally not focusable so touches pass through to the app
        // underneath; it only takes focus while the address bar is being typed in.
        urlBar.setOnClickListener { setFocusable(true) }
        urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                load(urlBar.text.toString())
                dismissKeyboard(urlBar)
                true
            } else {
                false
            }
        }
    }

    private fun setupDragging(root: View) {
        val header = root.findViewById<View>(R.id.floating_header)
        header.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var moved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        moved = false
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - touchX
                        val dy = event.rawY - touchY
                        if (abs(dx) > 6 || abs(dy) > 6) moved = true
                        params.x = startX + dx.roundToInt()
                        params.y = startY + dy.roundToInt()
                        safeUpdate()
                        return true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (moved) snapToEdge()
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun setupResizing(root: View) {
        val grip = root.findViewById<View>(R.id.floating_resize_handle)
        val metrics = resources.displayMetrics
        val minWidth = (240 * metrics.density).toInt()
        val minHeight = (200 * metrics.density).toInt()

        grip.setOnTouchListener(object : View.OnTouchListener {
            private var startW = 0
            private var startH = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startW = params.width
                        startH = params.height
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        params.width = (startW + (event.rawX - touchX)).roundToInt()
                            .coerceIn(minWidth, metrics.widthPixels)
                        params.height = (startH + (event.rawY - touchY)).roundToInt()
                            .coerceIn(minHeight, metrics.heightPixels)
                        safeUpdate()
                        return true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        prefs.floatingWidth = params.width
                        prefs.floatingHeight = params.height
                        return true
                    }
                }
                return false
            }
        })
    }

    /** Slides the window to whichever vertical edge it is closest to. */
    private fun snapToEdge() {
        val metrics = resources.displayMetrics
        val margin = (8 * metrics.density).toInt()
        val centre = params.x + params.width / 2
        params.x = if (centre < metrics.widthPixels / 2) {
            margin
        } else {
            metrics.widthPixels - params.width - margin
        }
        params.y = params.y.coerceIn(0, (metrics.heightPixels - params.height).coerceAtLeast(0))
        safeUpdate()
        prefs.floatingX = params.x
        prefs.floatingY = params.y
    }

    private fun cycleOpacity() {
        alphaStep = (alphaStep + 1) % ALPHA_STEPS.size
        val alpha = ALPHA_STEPS[alphaStep]
        overlayView?.alpha = alpha
        prefs.floatingAlpha = alpha
    }

    // ------------------------------------------------------------- bubble

    private fun minimizeToBubble() {
        if (minimized) return
        minimized = true
        overlayView?.visibility = View.GONE

        val bubble = LayoutInflater.from(this).inflate(R.layout.floating_bubble, null)
        val metrics = resources.displayMetrics
        val bp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = params.x.coerceIn(0, metrics.widthPixels - (64 * metrics.density).toInt())
            y = params.y
        }

        bubble.findViewById<View>(R.id.iv_bubble).setOnClickListener { restoreFromBubble() }
        bubble.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = bp.x
                        startY = bp.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        bp.x = startX + (event.rawX - touchX).roundToInt()
                        bp.y = startY + (event.rawY - touchY).roundToInt()
                        bubbleView?.let {
                            runCatching { windowManager.updateViewLayout(it, bp) }
                        }
                        return true
                    }
                }
                return false
            }
        })

        bubbleParams = bp
        bubbleView = bubble
        runCatching { windowManager.addView(bubble, bp) }
        safeUpdate()
    }

    private fun restoreFromBubble() {
        if (!minimized) return
        minimized = false
        bubbleView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        bubbleParams?.let {
            params.x = it.x
            params.y = it.y
        }
        bubbleView = null
        bubbleParams = null
        overlayView?.visibility = View.VISIBLE
        safeUpdate()
        snapToEdge()
    }

    // -------------------------------------------------------------- misc

    private fun load(input: String) {
        val url = UrlUtils.normalize(input)
        prefs.floatingUrl = url
        overlayView?.findViewById<EditText>(R.id.et_floating_url)?.let {
            if (!it.hasFocus()) it.setText(UrlUtils.prettify(url))
        }
        webView?.loadUrl(url)
    }

    private fun setFocusable(focusable: Boolean) {
        params.flags = if (focusable) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        safeUpdate()
    }

    private fun dismissKeyboard(field: EditText) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(field.windowToken, 0)
        field.clearFocus()
        setFocusable(false)
    }

    private fun safeUpdate() {
        val view = overlayView ?: return
        runCatching { windowManager.updateViewLayout(view, params) }
        prefs.floatingX = params.x
        prefs.floatingY = params.y
    }

    private fun startForegroundCompat() {
        createChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            flags
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(R.drawable.ic_float)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        webView?.let {
            it.stopLoading()
            it.webChromeClient = null
            it.destroy()
        }
        webView = null
        bubbleView?.let { view -> runCatching { windowManager.removeView(view) } }
        overlayView?.let { view -> runCatching { windowManager.removeView(view) } }
        bubbleView = null
        overlayView = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_OVERLAY_URL = "com.splitview.pad.OVERLAY_URL"
        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private val ALPHA_STEPS = floatArrayOf(1.0f, 0.85f, 0.65f, 0.45f)
    }
}
