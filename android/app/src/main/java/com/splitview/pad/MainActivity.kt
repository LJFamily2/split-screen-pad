package com.splitview.pad

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var splitContainer: LinearLayout
    private lateinit var pane1Container: View
    private lateinit var pane2Container: View
    private lateinit var dividerHandle: View

    private lateinit var webViewPane1: WebView
    private lateinit var webViewPane2: WebView
    private lateinit var etPane1Url: EditText
    private lateinit var etPane2Url: EditText

    private lateinit var tbPane1Ua: ToggleButton
    private lateinit var tbPane2Ua: ToggleButton

    private lateinit var appPairManager: AppPairManager
    private var activePane: Int = 1 // 1 for Pane 1, 2 for Pane 2

    private val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private var mobileUserAgentPane1: String = ""
    private var mobileUserAgentPane2: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        appPairManager = AppPairManager(this)

        initViews()
        setupWebViews()
        setupDividerDragging()
        setupControls()
        setupModernBackNavigation()

        // Load Default Pair (YouTube + Google)
        loadUrlPane1("https://m.youtube.com")
        loadUrlPane2("https://www.google.com")
    }

    private fun initViews() {
        splitContainer = findViewById(R.id.split_container)
        pane1Container = findViewById(R.id.pane1_container)
        pane2Container = findViewById(R.id.pane2_container)
        dividerHandle = findViewById(R.id.divider_handle)

        webViewPane1 = findViewById(R.id.webview_pane1)
        webViewPane2 = findViewById(R.id.webview_pane2)

        etPane1Url = findViewById(R.id.et_pane1_url)
        etPane2Url = findViewById(R.id.et_pane2_url)

        tbPane1Ua = findViewById(R.id.tb_pane1_ua)
        tbPane2Ua = findViewById(R.id.tb_pane2_ua)
    }

    private fun setupWebViews() {
        configureWebView(webViewPane1, etPane1Url, 1)
        configureWebView(webViewPane2, etPane2Url, 2)

        mobileUserAgentPane1 = webViewPane1.settings.userAgentString
        mobileUserAgentPane2 = webViewPane2.settings.userAgentString

        tbPane1Ua.setOnCheckedChangeListener { _, isChecked ->
            webViewPane1.settings.userAgentString = if (isChecked) desktopUserAgent else mobileUserAgentPane1
            webViewPane1.reload()
        }

        tbPane2Ua.setOnCheckedChangeListener { _, isChecked ->
            webViewPane2.settings.userAgentString = if (isChecked) desktopUserAgent else mobileUserAgentPane2
            webViewPane2.reload()
        }
    }

    private fun configureWebView(webView: WebView, urlEditText: EditText, paneId: Int) {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        webView.setOnTouchListener { _, _ ->
            activePane = paneId
            false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let {
                    if (!urlEditText.hasFocus()) {
                        urlEditText.setText(it)
                    }
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }
        }
    }

    private fun setupControls() {
        // Pane 1 URL Navigation
        etPane1Url.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadUrlPane1(etPane1Url.text.toString())
                true
            } else false
        }
        etPane1Url.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) activePane = 1
        }

        // Pane 2 URL Navigation
        etPane2Url.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadUrlPane2(etPane2Url.text.toString())
                true
            } else false
        }
        etPane2Url.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) activePane = 2
        }

        findViewById<ImageButton>(R.id.btn_pane1_back).setOnClickListener {
            activePane = 1
            if (webViewPane1.canGoBack()) webViewPane1.goBack()
        }

        findViewById<ImageButton>(R.id.btn_pane1_reload).setOnClickListener {
            activePane = 1
            webViewPane1.reload()
        }

        findViewById<ImageButton>(R.id.btn_pane2_back).setOnClickListener {
            activePane = 2
            if (webViewPane2.canGoBack()) webViewPane2.goBack()
        }

        findViewById<ImageButton>(R.id.btn_pane2_reload).setOnClickListener {
            activePane = 2
            webViewPane2.reload()
        }

        // SWAP PANES (1-TAP SWAP PANE 1 & PANE 2)
        findViewById<Button>(R.id.btn_swap_panes).setOnClickListener {
            swapPanes()
        }

        // QUICK RATIO PRESETS
        findViewById<Button>(R.id.btn_ratio_3070).setOnClickListener { setSplitRatio(0.3f) }
        findViewById<Button>(R.id.btn_ratio_5050).setOnClickListener { setSplitRatio(0.5f) }
        findViewById<Button>(R.id.btn_ratio_7030).setOnClickListener { setSplitRatio(0.7f) }

        // Rotate Split Direction (Vertical vs Horizontal)
        val dividerPx = (14 * resources.displayMetrics.density).toInt()
        findViewById<Button>(R.id.btn_rotate_split).setOnClickListener {
            if (splitContainer.orientation == LinearLayout.VERTICAL) {
                splitContainer.orientation = LinearLayout.HORIZONTAL
                dividerHandle.layoutParams.width = dividerPx
                dividerHandle.layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT

                val p1Params = pane1Container.layoutParams as LinearLayout.LayoutParams
                p1Params.width = 0
                p1Params.height = LinearLayout.LayoutParams.MATCH_PARENT
                pane1Container.layoutParams = p1Params

                val p2Params = pane2Container.layoutParams as LinearLayout.LayoutParams
                p2Params.width = 0
                p2Params.height = LinearLayout.LayoutParams.MATCH_PARENT
                pane2Container.layoutParams = p2Params
            } else {
                splitContainer.orientation = LinearLayout.VERTICAL
                dividerHandle.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
                dividerHandle.layoutParams.height = dividerPx

                val p1Params = pane1Container.layoutParams as LinearLayout.LayoutParams
                p1Params.width = LinearLayout.LayoutParams.MATCH_PARENT
                p1Params.height = 0
                pane1Container.layoutParams = p1Params

                val p2Params = pane2Container.layoutParams as LinearLayout.LayoutParams
                p2Params.width = LinearLayout.LayoutParams.MATCH_PARENT
                p2Params.height = 0
                pane2Container.layoutParams = p2Params
            }
            splitContainer.requestLayout()
        }

        // 2-Stage App Split Picker (Select App 1 then App 2 with instant search & Smart Launch)
        val btnLaunch = findViewById<Button>(R.id.btn_launch_native_app)
        applyTouchAnimation(btnLaunch)
        btnLaunch.setOnClickListener {
            show2StageAppPicker()
        }

        val btnSwap = findViewById<Button>(R.id.btn_swap_panes)
        applyTouchAnimation(btnSwap)
        btnSwap.setOnClickListener {
            swapPanes()
        }

        val btnRotate = findViewById<Button>(R.id.btn_rotate_split)
        applyTouchAnimation(btnRotate)

        val btnSavePair = findViewById<Button>(R.id.btn_save_pair)
        applyTouchAnimation(btnSavePair)
        btnSavePair.setOnClickListener {
            showSavePairDialog()
        }

        val btnFloating = findViewById<Button>(R.id.btn_floating_overlay)
        applyTouchAnimation(btnFloating)
        btnFloating.setOnClickListener {
            checkAndLaunchOverlay()
        }
    }

    private fun setupModernBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (activePane == 1 && webViewPane1.canGoBack()) {
                    webViewPane1.goBack()
                } else if (activePane == 2 && webViewPane2.canGoBack()) {
                    webViewPane2.goBack()
                } else if (webViewPane1.canGoBack()) {
                    webViewPane1.goBack()
                } else if (webViewPane2.canGoBack()) {
                    webViewPane2.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun swapPanes() {
        val url1 = etPane1Url.text.toString()
        val url2 = etPane2Url.text.toString()

        loadUrlPane1(url2)
        loadUrlPane2(url1)

        val ua1Check = tbPane1Ua.isChecked
        tbPane1Ua.isChecked = tbPane2Ua.isChecked
        tbPane2Ua.isChecked = ua1Check

        Toast.makeText(this, "Panes Swapped!", Toast.LENGTH_SHORT).show()
    }

    private fun setSplitRatio(ratio: Float) {
        val p1Params = pane1Container.layoutParams as LinearLayout.LayoutParams
        val p2Params = pane2Container.layoutParams as LinearLayout.LayoutParams

        p1Params.weight = ratio
        p2Params.weight = 1.0f - ratio

        pane1Container.layoutParams = p1Params
        pane2Container.layoutParams = p2Params
        splitContainer.requestLayout()
    }

    private fun loadUrlPane1(url: String) {
        val formatted = formatUrl(url)
        etPane1Url.setText(formatted)
        webViewPane1.loadUrl(formatted)
    }

    private fun loadUrlPane2(url: String) {
        val formatted = formatUrl(url)
        etPane2Url.setText(formatted)
        webViewPane2.loadUrl(formatted)
    }

    private fun formatUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "https://www.google.com"
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://www.google.com/search?q=$trimmed"
        }
    }

    private fun setupDividerDragging() {
        dividerHandle.setOnTouchListener(object : View.OnTouchListener {
            private var lastTouchPos = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val isVertical = splitContainer.orientation == LinearLayout.VERTICAL
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastTouchPos = if (isVertical) event.rawY else event.rawX
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val currentPos = if (isVertical) event.rawY else event.rawX
                        val delta = currentPos - lastTouchPos
                        lastTouchPos = currentPos

                        val totalSize = if (isVertical) splitContainer.height else splitContainer.width
                        if (totalSize > 0) {
                            val p1Params = pane1Container.layoutParams as LinearLayout.LayoutParams
                            val p2Params = pane2Container.layoutParams as LinearLayout.LayoutParams

                            var p1Weight = p1Params.weight + (delta / totalSize.toFloat())
                            p1Weight = p1Weight.coerceIn(0.15f, 0.85f)
                            val p2Weight = 1.0f - p1Weight

                            p1Params.weight = p1Weight
                            p2Params.weight = p2Weight

                            pane1Container.layoutParams = p1Params
                            pane2Container.layoutParams = p2Params
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun applyTouchAnimation(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.94f)
                        .scaleY(0.94f)
                        .setDuration(120)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(220)
                        .setInterpolator(OvershootInterpolator(2.0f))
                        .start()
                }
            }
            false
        }
    }

    data class AppItem(
        val label: String,
        val url: String,
        val packageName: String? = null,
        val icon: android.graphics.drawable.Drawable? = null
    )

    // 2-STAGE STEP-BY-STAGE APP SPLIT PICKER WITH INSTANT SEARCH & DUAL PANE SPLIT LAUNCH
    private fun show2StageAppPicker() {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }

        val allAppsList = mutableListOf<AppItem>()

        // 1. Add top Web Apps & PWAs (Zalo Web, Chrome, YouTube, Facebook, TikTok, etc.)
        allAppsList.add(AppItem("💬 Zalo Web App", "https://chat.zalo.me"))
        allAppsList.add(AppItem("🌐 Google Chrome", "https://www.google.com"))
        allAppsList.add(AppItem("▶️ YouTube", "https://m.youtube.com"))
        allAppsList.add(AppItem("👤 Facebook", "https://m.facebook.com"))
        allAppsList.add(AppItem("🤖 ChatGPT AI", "https://chatgpt.com"))
        allAppsList.add(AppItem("🎵 TikTok", "https://www.tiktok.com"))
        allAppsList.add(AppItem("🛍️ Shopee", "https://shopee.vn"))
        allAppsList.add(AppItem("✈️ Telegram Web", "https://web.telegram.org"))
        allAppsList.add(AppItem("📚 Wikipedia", "https://www.wikipedia.org"))

        // 2. Add Installed Native Apps with auto web equivalents
        resolveInfos.forEach { rInfo ->
            val pkg = rInfo.activityInfo.packageName
            val label = rInfo.loadLabel(pm).toString()
            val url = getWebEquivalent(pkg)
            val icon = rInfo.loadIcon(pm)
            allAppsList.add(AppItem(label, url, pkg, icon))
        }

        var stage = 1
        var selectedApp1: AppItem? = null
        var selectedApp2: AppItem? = null

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_picker, null)
        val tvStageBadge = dialogView.findViewById<TextView>(R.id.tv_picker_stage_badge)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_picker_title)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tv_picker_subtitle)
        val btnAddCustom = dialogView.findViewById<Button>(R.id.btn_add_custom_url)
        val etSearch = dialogView.findViewById<EditText>(R.id.et_app_search)
        val listView = dialogView.findViewById<ListView>(R.id.lv_installed_apps)

        var filteredList = allAppsList.toMutableList()

        val adapter = object : ArrayAdapter<AppItem>(
            this,
            android.R.layout.select_dialog_item,
            android.R.id.text1,
            filteredList
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val text1 = view.findViewById<TextView>(android.R.id.text1)
                val item = getItem(position)
                if (item != null) {
                    text1.text = item.label
                    text1.setTextColor(androidx.core.content.ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                    if (item.icon != null) {
                        val sizePx = (32 * resources.displayMetrics.density).toInt()
                        item.icon.setBounds(0, 0, sizePx, sizePx)
                        text1.setCompoundDrawables(item.icon, null, null, null)
                        text1.compoundDrawablePadding = (12 * resources.displayMetrics.density).toInt()
                    } else {
                        text1.setCompoundDrawables(null, null, null, null)
                    }
                }
                return view
            }
        }

        listView.adapter = adapter

        var dialog: AlertDialog? = null

        fun proceedSelection(selected: AppItem) {
            if (stage == 1) {
                selectedApp1 = selected
                stage = 2
                tvStageBadge.text = "STEP 2 OF 2"
                tvTitle.text = "Select App 2 (Pane 2 / Right Screen)"
                tvSubtitle.text = "App 1: ${selected.label} selected. Now select App 2 (e.g. Chrome) to split!"
                etSearch.setText("")
                filteredList.clear()
                filteredList.addAll(allAppsList)
                adapter.notifyDataSetChanged()
            } else {
                selectedApp2 = selected
                dialog?.dismiss()
                val a1 = selectedApp1 ?: return
                val a2 = selectedApp2 ?: return
                executeSmartLaunch(a1, a2)
            }
        }

        btnAddCustom.setOnClickListener {
            showCustomUrlInputDialog { customItem ->
                proceedSelection(customItem)
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                filteredList.clear()
                if (query.isEmpty()) {
                    filteredList.addAll(allAppsList)
                } else {
                    filteredList.addAll(allAppsList.filter {
                        it.label.lowercase().contains(query) || it.url.lowercase().contains(query)
                    })
                }
                adapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            proceedSelection(filteredList[position])
        }

        dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showCustomUrlInputDialog(onSuccess: (AppItem) -> Unit) {
        val input = EditText(this)
        input.hint = "e.g. https://chat.zalo.me or https://mywebsite.com"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI

        AlertDialog.Builder(this)
            .setTitle("🌐 Custom Website / PWA URL")
            .setMessage("Enter the URL of the downloaded website or PWA you want to split:")
            .setView(input)
            .setPositiveButton("Use URL") { _, _ ->
                val rawUrl = input.text.toString().trim()
                if (rawUrl.isNotEmpty()) {
                    val formatted = formatUrl(rawUrl)
                    val label = "🌐 " + formatted.replace("https://", "").replace("http://", "").take(25)
                    onSuccess(AppItem(label, formatted))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeSmartLaunch(app1: AppItem, app2: AppItem) {
        if (app1.packageName != null || app2.packageName != null) {
            Toast.makeText(this, "⚡ Launching Apps via System...", Toast.LENGTH_SHORT).show()
            
            // Launch App 1 natively if it has a package name
            if (app1.packageName != null) {
                launchNativeAppInMultiWindow(app1.packageName)
            } else {
                loadUrlPane1(app1.url)
            }

            // Launch App 2 natively if it has a package name, with a slight delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (app2.packageName != null) {
                    launchNativeAppInMultiWindow(app2.packageName)
                } else {
                    loadUrlPane2(app2.url)
                }
            }, 800)

            // Suggest using Recents to complete the split
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                Toast.makeText(this, "Apps launched! Go to Recents menu to split them if not already in Split Screen.", Toast.LENGTH_LONG).show()
            }, 2000)

        } else {
            // Both are URLs, use internal dual web view
            loadUrlPane1(app1.url)
            loadUrlPane2(app2.url)

            Toast.makeText(
                this,
                "⚡ Dual Web View Loaded: ${app1.label} (Pane 1) + ${app2.label} (Pane 2)",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getWebEquivalent(packageName: String): String {
        val lower = packageName.lowercase()
        return when {
            lower.contains("zalo") -> "https://chat.zalo.me"
            lower.contains("chrome") or lower.contains("browser") -> "https://www.google.com"
            lower.contains("youtube") -> "https://m.youtube.com"
            lower.contains("facebook") or lower.contains("katana") -> "https://m.facebook.com"
            lower.contains("messenger") or lower.contains("orca") -> "https://www.messenger.com"
            lower.contains("telegram") -> "https://web.telegram.org"
            lower.contains("shopee") -> "https://shopee.vn"
            lower.contains("lazada") -> "https://www.lazada.vn"
            lower.contains("tiktok") -> "https://www.tiktok.com"
            lower.contains("instagram") -> "https://www.instagram.com"
            lower.contains("twitter") or lower.contains("x") -> "https://x.com"
            lower.contains("reddit") -> "https://www.reddit.com"
            lower.contains("chatgpt") or lower.contains("openai") -> "https://chatgpt.com"
            lower.contains("wikipedia") -> "https://www.wikipedia.org"
            lower.contains("twitch") -> "https://m.twitch.tv"
            lower.contains("netflix") -> "https://www.netflix.com"
            lower.contains("spotify") -> "https://open.spotify.com"
            lower.contains("maps") -> "https://maps.google.com"
            lower.contains("github") -> "https://github.com"
            else -> "https://www.google.com/search?q=" + packageName.substringAfterLast(".")
        }
    }

    private fun launchNativeAppInMultiWindow(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                )
            } else {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(launchIntent)
                Toast.makeText(this, "Launching $packageName in System Split...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Launched. Enable Split Screen in Xiaomi Recents if needed.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Unable to launch selected app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSavePairDialog() {
        val input = EditText(this)
        input.hint = "e.g., YouTube + Google Notes"
        AlertDialog.Builder(this)
            .setTitle("Save App Pair")
            .setMessage("Save current dual web views as a quick-launch pair:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    appPairManager.savePair(
                        name,
                        etPane1Url.text.toString(),
                        etPane2Url.text.toString()
                    )
                    Toast.makeText(this, "App pair saved!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSavedPairsDialog() {
        val pairs = appPairManager.getSavedPairs()
        if (pairs.isEmpty()) {
            Toast.makeText(this, "No saved app pairs", Toast.LENGTH_SHORT).show()
            return
        }

        val titles = pairs.map { it.title }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select App Pair")
            .setItems(titles) { _, which ->
                val selected = pairs[which]
                val pairOptions = arrayOf("🚀 Launch Pair", "🗑️ Delete Pair")
                AlertDialog.Builder(this)
                    .setTitle(selected.title)
                    .setItems(pairOptions) { _, optWhich ->
                        when (optWhich) {
                            0 -> {
                                loadUrlPane1(selected.urlPane1)
                                loadUrlPane2(selected.urlPane2)
                                Toast.makeText(this, "Loaded: ${selected.title}", Toast.LENGTH_SHORT).show()
                            }
                            1 -> {
                                appPairManager.deletePair(selected.id)
                                Toast.makeText(this, "Deleted: ${selected.title}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Back", null)
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun checkAndLaunchOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.overlay_permission_title)
                    .setMessage(R.string.overlay_permission_msg)
                    .setPositiveButton(R.string.grant_permission) { _, _ ->
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                startFloatingOverlayService()
            }
        } else {
            startFloatingOverlayService()
        }
    }

    private fun startFloatingOverlayService() {
        val serviceIntent = Intent(this, FloatingOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Toast.makeText(this, "Floating Overlay started over native apps", Toast.LENGTH_SHORT).show()
    }
}
