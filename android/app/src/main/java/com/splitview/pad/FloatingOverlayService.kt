package com.splitview.pad

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.app.NotificationCompat

class FloatingOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var webViewFloating: WebView? = null
    private var isMinimized = false
    private var expandedHeight = 0

    companion object {
        const val EXTRA_OVERLAY_URL = "com.splitview.pad.OVERLAY_URL"
        const val EXTRA_APP_NAME = "com.splitview.pad.APP_NAME"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val targetUrl = intent?.getStringExtra(EXTRA_OVERLAY_URL)
        val appName = intent?.getStringExtra(EXTRA_APP_NAME)
        if (!targetUrl.isNullOrEmpty()) {
            val etUrl = overlayView?.findViewById<EditText>(R.id.et_floating_url)
            etUrl?.setText(targetUrl)
            loadFloatingUrl(targetUrl)
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1001,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1001, createNotification())
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupFloatingWindow()
    }

    private fun setupFloatingWindow() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.floating_overlay, null)

        val metrics = resources.displayMetrics
        val widthPx = (360 * metrics.density).toInt().coerceAtMost(metrics.widthPixels - 40)
        val heightPx = (480 * metrics.density).toInt().coerceAtMost(metrics.heightPixels - 80)
        expandedHeight = heightPx

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 60
        params.y = 120

        windowManager.addView(overlayView, params)

        // Setup Floating Window Touch Dragging
        val headerView = overlayView?.findViewById<View>(R.id.floating_header)
        headerView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })

        // Minimize / Restore Toggle Button
        val btnMinimize = overlayView?.findViewById<ImageButton>(R.id.btn_floating_minimize)
        val navBar = overlayView?.findViewById<View>(R.id.et_floating_url)?.parent as? View
        btnMinimize?.setOnClickListener {
            if (isMinimized) {
                params.height = expandedHeight
                webViewFloating?.visibility = View.VISIBLE
                navBar?.visibility = View.VISIBLE
                isMinimized = false
            } else {
                params.height = (44 * metrics.density).toInt()
                webViewFloating?.visibility = View.GONE
                navBar?.visibility = View.GONE
                isMinimized = true
            }
            windowManager.updateViewLayout(overlayView, params)
        }

        // Close Floating Window Button
        val btnClose = overlayView?.findViewById<ImageButton>(R.id.btn_floating_close)
        btnClose?.setOnClickListener {
            stopSelf()
        }

        // Floating Navigation
        val etUrl = overlayView?.findViewById<EditText>(R.id.et_floating_url)
        val btnGo = overlayView?.findViewById<Button>(R.id.btn_floating_go)
        webViewFloating = overlayView?.findViewById(R.id.webview_floating)

        // Soft Keyboard Input Focus Handling
        etUrl?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            windowManager.updateViewLayout(overlayView, params)
        }

        etUrl?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadFloatingUrl(etUrl.text.toString())
                etUrl.clearFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(etUrl.windowToken, 0)
                true
            } else false
        }

        webViewFloating?.let { webView ->
            val settings = webView.settings
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webView.webViewClient = WebViewClient()
            webView.loadUrl("https://www.google.com")
        }

        btnGo?.setOnClickListener {
            loadFloatingUrl(etUrl?.text.toString())
            etUrl?.clearFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etUrl?.windowToken, 0)
        }
    }

    private fun loadFloatingUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.isNotEmpty()) {
            val finalUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "https://www.google.com/search?q=$trimmed"
            }
            webViewFloating?.loadUrl(finalUrl)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) {
            windowManager.removeView(overlayView)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "overlay_channel",
                "Floating Window Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "overlay_channel")
            .setContentTitle("Split Screen Pad")
            .setContentText("Floating overlay active over native apps")
            .setSmallIcon(android.R.drawable.ic_menu_crop)
            .build()
    }
}
