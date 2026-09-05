package com.splitview.pad

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var pairManager: AppPairManager

    private lateinit var homeScreen: View
    private lateinit var splitWorkspace: View
    private lateinit var homePairsList: LinearLayout
    private lateinit var hintToast: TextView

    private lateinit var split: SplitLayoutController
    private lateinit var pane1: WebPane
    private lateinit var pane2: WebPane

    private var activePane: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Material You: on Android 12+ the accent follows the device wallpaper theme.
        DynamicColors.applyToActivityIfAvailable(this)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        pairManager = AppPairManager(this)

        applySystemBarAppearance()
        bindViews()
        setupPanes()
        setupSplit()
        setupToolbar()
        setupHomeScreen()
        setupBackNavigation()

        restoreSession()
        if (prefs.resumeInSplit) showSplitWorkspace(showHint = false) else showHomeScreen()
    }

    // ----------------------------------------------------------------- setup

    private fun bindViews() {
        homeScreen = findViewById(R.id.home_screen_container)
        splitWorkspace = findViewById(R.id.split_content_view)
        homePairsList = findViewById(R.id.home_pairs_list)
        hintToast = findViewById(R.id.tv_hint_toast)
    }

    private fun setupPanes() {
        pane1 = buildPane(
            id = 1,
            container = findViewById(R.id.pane1_container),
            webView = findViewById(R.id.webview_pane1),
            holder = findViewById(R.id.pane1_web_holder),
            urlBar = findViewById(R.id.et_pane1_url),
            progress = findViewById(R.id.pb_pane1),
            label = findViewById(R.id.tv_pane1_label),
            back = findViewById(R.id.btn_pane1_back),
            forward = findViewById(R.id.btn_pane1_forward),
            reload = findViewById(R.id.btn_pane1_reload),
            ua = findViewById(R.id.btn_pane1_ua),
            more = findViewById(R.id.btn_pane1_more)
        )
        pane2 = buildPane(
            id = 2,
            container = findViewById(R.id.pane2_container),
            webView = findViewById(R.id.webview_pane2),
            holder = findViewById(R.id.pane2_web_holder),
            urlBar = findViewById(R.id.et_pane2_url),
            progress = findViewById(R.id.pb_pane2),
            label = findViewById(R.id.tv_pane2_label),
            back = findViewById(R.id.btn_pane2_back),
            forward = findViewById(R.id.btn_pane2_forward),
            reload = findViewById(R.id.btn_pane2_reload),
            ua = findViewById(R.id.btn_pane2_ua),
            more = findViewById(R.id.btn_pane2_more)
        )

        // Rounded corners actually clip the web content.
        pane1.container.clipToOutline = true
        pane2.container.clipToOutline = true

        setActivePane(1)
    }

    @Suppress("LongParameterList")
    private fun buildPane(
        id: Int,
        container: View,
        webView: WebView,
        holder: FrameLayout,
        urlBar: EditText,
        progress: ProgressBar,
        label: TextView,
        back: ImageButton,
        forward: ImageButton,
        reload: ImageButton,
        ua: Button,
        more: ImageButton
    ) = WebPane(
        activity = this,
        id = id,
        container = container,
        webView = webView,
        webHolder = holder,
        urlBar = urlBar,
        progress = progress,
        label = label,
        btnBack = back,
        btnForward = forward,
        btnReload = reload,
        btnUa = ua,
        btnMore = more,
        onFocused = { setActivePane(it.id) },
        onUrlChanged = { pane, url ->
            if (pane.id == 1) prefs.pane1Url = url else prefs.pane2Url = url
        },
        onMoreClicked = { pane, _ -> showPaneMenu(pane) }
    )

    private fun setupSplit() {
        split = SplitLayoutController(
            context = this,
            splitContainer = findViewById(R.id.split_container),
            pane1 = pane1.container,
            pane2 = pane2.container,
            divider = findViewById(R.id.divider_handle),
            ratioTooltip = findViewById(R.id.tv_ratio_tooltip),
            onSwapRequested = { swapPanes() },
            onDividerTapped = { showSplitOptions() },
            onRatioSettled = { prefs.splitRatio = it },
            onMaximizedChanged = { maximized ->
                if (maximized != 0) {
                    setActivePane(maximized)
                    showHint(getString(R.string.hint_maximized))
                }
            }
        )
    }

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btn_home).setOnClickListener { showHomeScreen() }

        val r30 = findViewById<Button>(R.id.btn_ratio_3070)
        val r50 = findViewById<Button>(R.id.btn_ratio_5050)
        val r70 = findViewById<Button>(R.id.btn_ratio_7030)
        val ratioButtons = listOf(r30 to 1f / 3f, r50 to 0.5f, r70 to 2f / 3f)
        ratioButtons.forEach { (button, value) ->
            button.setOnClickListener {
                split.applyRatio(value, persist = true)
                ratioButtons.forEach { (other, _) -> other.isSelected = other === button }
            }
        }
        r50.isSelected = true

        findViewById<Button>(R.id.btn_swap_panes).setOnClickListener { swapPanes() }
        findViewById<Button>(R.id.btn_rotate_split).setOnClickListener {
            split.toggleDirection()
            prefs.verticalSplit = split.isVertical
            showHint(
                getString(
                    if (split.isVertical) R.string.hint_stacked else R.string.hint_side_by_side
                )
            )
        }
        findViewById<Button>(R.id.btn_launch_native_app).setOnClickListener { showAppPicker() }
        findViewById<Button>(R.id.btn_save_pair).setOnClickListener { promptSavePair() }
        findViewById<Button>(R.id.btn_saved_pairs).setOnClickListener { showPairsDialog() }
        findViewById<Button>(R.id.btn_floating_overlay).setOnClickListener {
            startFloating(prefs.pane2Url)
        }
    }

    private fun setupHomeScreen() {
        findViewById<View>(R.id.btn_mode_dual_web).setOnClickListener {
            showSplitWorkspace(showHint = true)
        }
        findViewById<View>(R.id.btn_mode_split_native).setOnClickListener {
            showSplitWorkspace(showHint = false)
            showAppPicker()
        }
        findViewById<View>(R.id.btn_mode_floating).setOnClickListener {
            startFloating(prefs.floatingUrl)
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val active = activePaneObject()
                when {
                    pane1.exitFullscreenIfNeeded() || pane2.exitFullscreenIfNeeded() -> Unit
                    split.maximizedPane != 0 -> split.restore()
                    active.canGoBack() -> active.goBack()
                    otherPane(active).canGoBack() -> otherPane(active).goBack()
                    splitWorkspace.visibility == View.VISIBLE -> showHomeScreen()
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun applySystemBarAppearance() {
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
    }

    // -------------------------------------------------------------- session

    private fun restoreSession() {
        split.setVertical(prefs.verticalSplit)
        split.applyRatio(prefs.splitRatio, persist = false)
        pane1.setDesktopMode(prefs.pane1Desktop, reload = false)
        pane2.setDesktopMode(prefs.pane2Desktop, reload = false)
        pane1.load(prefs.pane1Url)
        pane2.load(prefs.pane2Url)
    }

    private fun showHomeScreen() {
        prefs.resumeInSplit = false
        splitWorkspace.visibility = View.GONE
        homeScreen.visibility = View.VISIBLE
        renderHomePairs()
    }

    private fun showSplitWorkspace(showHint: Boolean) {
        prefs.resumeInSplit = true
        homeScreen.visibility = View.GONE
        splitWorkspace.visibility = View.VISIBLE
        if (showHint) showHint(getString(R.string.divider_hint))
    }

    private fun renderHomePairs() {
        homePairsList.removeAllViews()
        val pairs = pairManager.getSavedPairs()
        if (pairs.isEmpty()) {
            val empty = TextView(this).apply {
                setText(R.string.no_pairs_yet)
                setTextColor(colorOf(R.color.text_tertiary))
                textSize = 13f
            }
            homePairsList.addView(empty)
            return
        }
        val inflater = LayoutInflater.from(this)
        pairs.forEach { pair ->
            val row = inflater.inflate(R.layout.item_app_pair, homePairsList, false)
            bindPairRow(row, pair) { renderHomePairs() }
            homePairsList.addView(row)
        }
    }

    private fun bindPairRow(row: View, pair: AppPair, onChanged: () -> Unit) {
        row.findViewById<TextView>(R.id.tv_pair_title).text = pair.title
        row.findViewById<TextView>(R.id.tv_pair_urls).text = getString(
            R.string.pair_urls,
            UrlUtils.host(pair.urlPane1),
            UrlUtils.host(pair.urlPane2)
        )
        row.setOnClickListener { launchPair(pair) }
        row.findViewById<ImageButton>(R.id.btn_pair_menu).setOnClickListener {
            showActionSheet(
                pair.title,
                listOf(
                    Action(R.drawable.ic_open_in_new, getString(R.string.pair_launch)) {
                        launchPair(pair)
                    },
                    Action(R.drawable.ic_copy, getString(R.string.pair_rename)) {
                        promptRenamePair(pair, onChanged)
                    },
                    Action(R.drawable.ic_close, getString(R.string.pair_delete)) {
                        pairManager.deletePair(pair.id)
                        showHint(getString(R.string.pair_deleted))
                        onChanged()
                    }
                )
            )
        }
    }

    private fun launchPair(pair: AppPair) {
        pane1.load(pair.urlPane1)
        pane2.load(pair.urlPane2)
        split.restore()
        showSplitWorkspace(showHint = false)
        showHint(pair.title)
    }

    // ---------------------------------------------------------------- panes

    private fun activePaneObject(): WebPane = if (activePane == 2) pane2 else pane1

    private fun otherPane(pane: WebPane): WebPane = if (pane.id == 1) pane2 else pane1

    private fun setActivePane(id: Int) {
        activePane = id
        pane1.setActive(id == 1)
        pane2.setActive(id == 2)
    }

    private fun swapPanes() {
        val url1 = pane1.currentUrl
        val url2 = pane2.currentUrl
        val desktop1 = pane1.isDesktopMode
        val desktop2 = pane2.isDesktopMode

        pane1.setDesktopMode(desktop2, reload = false)
        pane2.setDesktopMode(desktop1, reload = false)
        pane1.load(url2)
        pane2.load(url1)

        prefs.pane1Desktop = desktop2
        prefs.pane2Desktop = desktop1
        showHint(getString(R.string.panes_swapped))
    }

    private fun showPaneMenu(pane: WebPane) {
        val maximized = split.maximizedPane == pane.id
        showActionSheet(
            getString(if (pane.id == 1) R.string.pane_1 else R.string.pane_2),
            listOf(
                Action(
                    R.drawable.ic_maximize,
                    getString(if (maximized) R.string.menu_restore else R.string.menu_maximize)
                ) { split.toggleMaximize(pane.id) },
                Action(R.drawable.ic_float, getString(R.string.menu_float)) {
                    startFloating(pane.currentUrl)
                },
                Action(R.drawable.ic_swap, getString(R.string.menu_send_other)) {
                    otherPane(pane).load(pane.currentUrl)
                },
                Action(
                    if (pane.isDesktopMode) R.drawable.ic_phone else R.drawable.ic_desktop,
                    getString(R.string.menu_desktop_site)
                ) {
                    pane.setDesktopMode(!pane.isDesktopMode, reload = true)
                    if (pane.id == 1) prefs.pane1Desktop = pane.isDesktopMode
                    else prefs.pane2Desktop = pane.isDesktopMode
                },
                Action(R.drawable.ic_copy, getString(R.string.menu_copy_url)) {
                    copyToClipboard(pane.currentUrl)
                },
                Action(R.drawable.ic_share, getString(R.string.menu_share)) {
                    shareUrl(pane.currentUrl)
                },
                Action(R.drawable.ic_open_in_new, getString(R.string.menu_open_browser)) {
                    openExternally(pane.currentUrl)
                }
            )
        )
    }

    private fun showSplitOptions() {
        showActionSheet(
            getString(R.string.quick_menu_title),
            listOf(
                Action(R.drawable.ic_swap, getString(R.string.quick_swap)) { swapPanes() },
                Action(R.drawable.ic_grid_split, getString(R.string.quick_even)) {
                    split.applyRatio(0.5f, persist = true)
                },
                Action(R.drawable.ic_maximize, getString(R.string.quick_max_1)) {
                    split.toggleMaximize(1)
                },
                Action(R.drawable.ic_maximize, getString(R.string.quick_max_2)) {
                    split.toggleMaximize(2)
                },
                Action(R.drawable.ic_rotate, getString(R.string.quick_rotate)) {
                    split.toggleDirection()
                    prefs.verticalSplit = split.isVertical
                },
                Action(R.drawable.ic_float, getString(R.string.quick_float_2)) {
                    startFloating(pane2.currentUrl)
                },
                Action(R.drawable.ic_star, getString(R.string.quick_save_pair)) { promptSavePair() }
            )
        )
    }

    // ------------------------------------------------------------ app pairs

    private fun promptSavePair() {
        val input = EditText(this).apply {
            setHint(R.string.save_pair_hint)
            setText(
                getString(
                    R.string.pair_urls,
                    UrlUtils.host(pane1.currentUrl),
                    UrlUtils.host(pane2.currentUrl)
                )
            )
            setSingleLine()
            setPadding(48, 40, 48, 40)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.save_pair_title)
            .setMessage(R.string.save_pair_msg)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    pairManager.savePair(name, pane1.currentUrl, pane2.currentUrl)
                    showHint(getString(R.string.pair_saved))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun promptRenamePair(pair: AppPair, onChanged: () -> Unit) {
        val input = EditText(this).apply {
            setText(pair.title)
            setSingleLine()
            setPadding(48, 40, 48, 40)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.pair_rename)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    pairManager.renamePair(pair.id, name)
                    onChanged()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPairsDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_pairs, null)
        val list: RecyclerView = view.findViewById(R.id.rv_pairs)
        val empty: TextView = view.findViewById(R.id.tv_pairs_empty)
        val pairs = pairManager.getSavedPairs().toMutableList()

        list.layoutManager = LinearLayoutManager(this)
        empty.visibility = if (pairs.isEmpty()) View.VISIBLE else View.GONE

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton(R.string.close, null)
            .create()

        list.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder =
                object : RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_app_pair, parent, false)
                ) {}

            override fun getItemCount() = pairs.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val pair = pairs[position]
                bindPairRow(holder.itemView, pair) {
                    pairs.clear()
                    pairs.addAll(pairManager.getSavedPairs())
                    notifyDataSetChanged()
                    empty.visibility = if (pairs.isEmpty()) View.VISIBLE else View.GONE
                    renderHomePairs()
                }
                holder.itemView.setOnClickListener {
                    dialog.dismiss()
                    launchPair(pair)
                }
            }
        }
        dialog.show()
    }

    // ----------------------------------------------------------- app picker

    private fun showAppPicker() {
        AppPickerDialog(this) { first, second -> launchChosenPair(first, second) }.show()
    }

    private fun launchChosenPair(first: AppEntry, second: AppEntry) {
        val bothInstalled = first.isInstalledApp && second.isInstalledApp
        when {
            bothInstalled && NativeSplitLauncher.canLaunchAdjacent(this) -> {
                NativeSplitLauncher.launchPair(this, first.packageName!!, second.packageName!!)
            }

            bothInstalled -> confirmSystemSplit(first, second)

            else -> {
                // At least one side is a web app: load whatever we can into the panes
                // and launch the installed one, if any, alongside.
                loadEntryIntoPane(first, pane1)
                loadEntryIntoPane(second, pane2)
                showSplitWorkspace(showHint = false)
                split.restore()
            }
        }
    }

    private fun loadEntryIntoPane(entry: AppEntry, pane: WebPane) {
        pane.load(entry.url)
    }

    /**
     * Two installed apps, but the system will not accept an adjacent launch from a
     * full-screen app. Explain it once and let the user choose.
     */
    private fun confirmSystemSplit(first: AppEntry, second: AppEntry) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.system_split_title)
            .setMessage(getString(R.string.system_split_msg, first.label, second.label))
            .setPositiveButton(R.string.system_split_open_both) { _, _ ->
                NativeSplitLauncher.launchPair(this, first.packageName!!, second.packageName!!)
                showHint(getString(R.string.split_launch_hint))
            }
            .setNeutralButton(R.string.system_split_use_web) { _, _ ->
                pane1.load(first.url)
                pane2.load(second.url)
                showSplitWorkspace(showHint = false)
                split.restore()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ------------------------------------------------------- floating window

    private fun startFloating(url: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.overlay_permission_title)
                .setMessage(R.string.overlay_permission_msg)
                .setPositiveButton(R.string.grant_permission) { _, _ ->
                    runCatching {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                        )
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return
        }

        prefs.floatingUrl = url
        val intent = Intent(this, FloatingOverlayService::class.java)
            .putExtra(FloatingOverlayService.EXTRA_OVERLAY_URL, url)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        showHint(getString(R.string.overlay_started))
    }

    // -------------------------------------------------------------- helpers

    private data class Action(val iconRes: Int, val label: String, val run: () -> Unit)

    /** Glass action sheet used for the divider menu, pane menu and pair menu. */
    private fun showActionSheet(title: String, actions: List<Action>) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_quick_actions, null)
        view.findViewById<TextView>(R.id.tv_quick_title).text = title
        val list = view.findViewById<LinearLayout>(R.id.quick_action_list)

        val dialog = MaterialAlertDialogBuilder(this).setView(view).create()
        val inflater = LayoutInflater.from(this)
        actions.forEach { action ->
            val row = inflater.inflate(R.layout.item_quick_action, list, false)
            row.findViewById<ImageView>(R.id.iv_quick_icon).setImageResource(action.iconRes)
            row.findViewById<TextView>(R.id.tv_quick_label).text = action.label
            row.setOnClickListener {
                dialog.dismiss()
                action.run()
            }
            list.addView(row)
        }
        dialog.show()
    }

    private fun showHint(message: String) {
        hintToast.text = message
        hintToast.visibility = View.VISIBLE
        hintToast.alpha = 0f
        hintToast.animate().cancel()
        hintToast.animate()
            .alpha(1f)
            .setDuration(160)
            .withEndAction {
                hintToast.postDelayed({
                    hintToast.animate()
                        .alpha(0f)
                        .setDuration(240)
                        .withEndAction { hintToast.visibility = View.GONE }
                        .start()
                }, HINT_VISIBLE_MS)
            }
            .start()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("url", text))
        showHint(getString(R.string.link_copied))
    }

    private fun shareUrl(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        runCatching { startActivity(Intent.createChooser(intent, null)) }
    }

    private fun openExternally(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private fun colorOf(resId: Int) = androidx.core.content.ContextCompat.getColor(this, resId)

    // --------------------------------------------------- keyboard shortcuts

    @SuppressLint("RestrictedApi")
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (event.isCtrlPressed) {
            when (keyCode) {
                KeyEvent.KEYCODE_1 -> { setActivePane(1); return true }
                KeyEvent.KEYCODE_2 -> { setActivePane(2); return true }
                KeyEvent.KEYCODE_E -> { swapPanes(); return true }
                KeyEvent.KEYCODE_R -> { activePaneObject().reload(); return true }
                KeyEvent.KEYCODE_D -> {
                    split.toggleDirection()
                    prefs.verticalSplit = split.isVertical
                    return true
                }
                KeyEvent.KEYCODE_M -> { split.toggleMaximize(activePane); return true }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    // ------------------------------------------------------------ lifecycle

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applySystemBarAppearance()
    }

    override fun onDestroy() {
        // Panes are only torn down with the activity, so media keeps playing while
        // the user works in the other half.
        pane1.destroy()
        pane2.destroy()
        super.onDestroy()
    }

    private companion object {
        const val HINT_VISIBLE_MS = 2200L
    }
}
