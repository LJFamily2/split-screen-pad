package com.splitview.pad

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * Everything one half of the split needs: the WebView, its address bar, the
 * navigation buttons, the progress bar and per-pane desktop/mobile mode.
 *
 * A pane never pauses its WebView, so audio and video in the pane the user is
 * *not* touching keeps playing — the main thing the stock split view gets wrong.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
class WebPane(
    private val activity: Activity,
    val id: Int,
    val container: View,
    val webView: WebView,
    private val webHolder: FrameLayout,
    private val urlBar: EditText,
    private val progress: ProgressBar,
    private val label: TextView,
    private val btnBack: ImageButton,
    private val btnForward: ImageButton,
    private val btnReload: ImageButton,
    private val btnUa: android.widget.Button,
    private val btnMore: ImageButton,
    private val onFocused: (WebPane) -> Unit,
    private val onUrlChanged: (WebPane, String) -> Unit,
    private val onMoreClicked: (WebPane, View) -> Unit
) {

    private val mobileUserAgent: String = webView.settings.userAgentString

    /** Fullscreen (e.g. a YouTube video going full screen) lives here. */
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    /** Held explicitly: WebView.getWebChromeClient() only exists from API 26. */
    private lateinit var chromeClient: WebChromeClient

    var isDesktopMode: Boolean = false
        private set

    /** False until this pane has been asked to load something. */
    var hasLoaded: Boolean = false
        private set

    val currentUrl: String
        get() = webView.url ?: urlBar.text.toString()

    init {
        configureWebView()
        wireControls()
    }

    private fun configureWebView() = with(webView.settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        @Suppress("DEPRECATION")
        databaseEnabled = true
        // Keeps background video/audio alive in the inactive pane.
        mediaPlaybackRequiresUserGesture = false
        useWideViewPort = true
        loadWithOverviewMode = true
        builtInZoomControls = true
        displayZoomControls = false
        setSupportZoom(true)
        allowFileAccess = false
        allowContentAccess = false
        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(false)
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        cacheMode = WebSettings.LOAD_DEFAULT

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(this, true)
        }

        webView.isVerticalScrollBarEnabled = true
        webView.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

        webView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) onFocused(this@WebPane)
            false
        }

        webView.setDownloadListener { url, _, _, _, _ ->
            // WebView cannot save files itself; hand downloads to the system.
            runCatching {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val target = request?.url ?: return false
                val scheme = target.scheme?.lowercase()
                if (scheme == "http" || scheme == "https") return false
                // intent://, mailto:, tel:, market:// … go to the system.
                return runCatching {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, target))
                    true
                }.getOrDefault(true)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progress.visibility = View.VISIBLE
                syncUrlBar(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progress.visibility = View.INVISIBLE
                syncUrlBar(url)
                updateNavButtons()
                url?.let { onUrlChanged(this@WebPane, it) }
            }
        }

        chromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.INVISIBLE
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                view?.let {
                    webHolder.addView(
                        it,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
                webView.visibility = View.GONE
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            override fun onHideCustomView() {
                customView?.let { webHolder.removeView(it) }
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                webView.visibility = View.VISIBLE
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        webView.webChromeClient = chromeClient
    }

    private fun wireControls() {
        urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                load(urlBar.text.toString())
                urlBar.clearFocus()
                hideKeyboard()
                true
            } else {
                false
            }
        }
        urlBar.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                onFocused(this)
                urlBar.setText(currentUrl)
                urlBar.selectAll()
            } else {
                syncUrlBar(currentUrl)
            }
        }

        label.setOnClickListener { onFocused(this) }
        container.setOnClickListener { onFocused(this) }

        btnBack.setOnClickListener {
            onFocused(this)
            if (webView.canGoBack()) webView.goBack()
        }
        btnForward.setOnClickListener {
            onFocused(this)
            if (webView.canGoForward()) webView.goForward()
        }
        btnReload.setOnClickListener {
            onFocused(this)
            webView.reload()
        }
        btnUa.setOnClickListener { setDesktopMode(!isDesktopMode, reload = true) }
        btnMore.setOnClickListener { onMoreClicked(this, btnMore) }

        updateNavButtons()
    }

    fun load(input: String) {
        hasLoaded = true
        val url = UrlUtils.normalize(input)
        syncUrlBar(url)
        webView.loadUrl(url)
        onUrlChanged(this, url)
    }

    fun reload() = webView.reload()

    fun canGoBack(): Boolean = webView.canGoBack()

    fun goBack() = webView.goBack()

    fun setDesktopMode(desktop: Boolean, reload: Boolean) {
        isDesktopMode = desktop
        webView.settings.userAgentString = if (desktop) DESKTOP_USER_AGENT else mobileUserAgent
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        btnUa.text = activity.getString(
            if (desktop) R.string.desktop_mode else R.string.mobile_mode
        )
        btnUa.isSelected = desktop
        if (reload) webView.reload()
    }

    fun setActive(active: Boolean) {
        container.isActivated = active
        label.alpha = if (active) 1f else 0.55f
    }

    /** Returns true when the pane consumed a fullscreen exit. */
    fun exitFullscreenIfNeeded(): Boolean {
        if (customView == null) return false
        chromeClient.onHideCustomView()
        return true
    }

    fun destroy() {
        webHolder.removeView(webView)
        webView.stopLoading()
        webView.webChromeClient = null
        webView.destroy()
    }

    private fun updateNavButtons() {
        btnBack.isEnabled = webView.canGoBack()
        btnBack.alpha = if (btnBack.isEnabled) 1f else 0.35f
        btnForward.isEnabled = webView.canGoForward()
        btnForward.alpha = if (btnForward.isEnabled) 1f else 0.35f
    }

    private fun syncUrlBar(url: String?) {
        if (url.isNullOrEmpty() || urlBar.hasFocus()) return
        urlBar.setText(UrlUtils.prettify(url))
    }

    private fun hideKeyboard() {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(urlBar.windowToken, 0)
    }

    companion object {
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"
    }
}
