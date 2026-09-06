package com.splitview.pad

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
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
    private lateinit var btnLayoutTwo: Button
    private lateinit var btnLayoutThree: Button

    // Native split guide overlay views
    private lateinit var nativeGuideOverlay: View
    private lateinit var tvGuideStep1: TextView
    private lateinit var tvGuideStep3: TextView

    private lateinit var split: SplitLayoutController
    private lateinit var panes: List<WebPane>

    private var activePane: Int = 1

    /** Panes only fetch their page once the workspace is actually on screen. */
    private var panesLoaded = false

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

        restoreLayout()
        if (prefs.resumeInSplit) showSplitWorkspace(showHint = false) else showHomeScreen()

        // Scan installed apps in the background so the picker opens instantly.
        AppCatalog.warmUp(this)
    }

    // ----------------------------------------------------------------- setup

    private fun bindViews() {
        homeScreen = findViewById(R.id.home_screen_container)
        splitWorkspace = findViewById(R.id.split_content_view)
        homePairsList = findViewById(R.id.home_pairs_list)
        hintToast = findViewById(R.id.tv_hint_toast)
        btnLayoutTwo = findViewById(R.id.btn_layout_two)
        btnLayoutThree = findViewById(R.id.btn_layout_three)

        // Guide overlay
        nativeGuideOverlay = findViewById(R.id.native_guide_overlay)
        tvGuideStep1 = findViewById(R.id.tv_guide_step1)
        tvGuideStep3 = findViewById(R.id.tv_guide_step3)
    }

    private fun setupPanes() {
        panes = listOf(
            buildPane(
                1,
                findViewById(R.id.pane1_container), findViewById(R.id.webview_pane1),
                findViewById(R.id.pane1_web_holder), findViewById(R.id.et_pane1_url),
                findViewById(R.id.pb_pane1), findViewById(R.id.tv_pane1_label),
                findViewById(R.id.btn_pane1_back), findViewById(R.id.btn_pane1_forward),
                findViewById(R.id.btn_pane1_reload), findViewById(R.id.btn_pane1_ua),
                findViewById(R.id.btn_pane1_more)
            ),
            buildPane(
                2,
                findViewById(R.id.pane2_container), findViewById(R.id.webview_pane2),
                findViewById(R.id.pane2_web_holder), findViewById(R.id.et_pane2_url),
                findViewById(R.id.pb_pane2), findViewById(R.id.tv_pane2_label),
                findViewById(R.id.btn_pane2_back), findViewById(R.id.btn_pane2_forward),
                findViewById(R.id.btn_pane2_reload), findViewById(R.id.btn_pane2_ua),
                findViewById(R.id.btn_pane2_more)
            ),
            buildPane(
                3,
                findViewById(R.id.pane3_container), findViewById(R.id.webview_pane3),
                findViewById(R.id.pane3_web_holder), findViewById(R.id.et_pane3_url),
                findViewById(R.id.pb_pane3), findViewById(R.id.tv_pane3_label),
                findViewById(R.id.btn_pane3_back), findViewById(R.id.btn_pane3_forward),
                findViewById(R.id.btn_pane3_reload), findViewById(R.id.btn_pane3_ua),
                findViewById(R.id.btn_pane3_more)
            )
        )
        panes.forEach { it.container.clipToOutline = true }
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
        onUrlChanged = { pane, url -> prefs.setPaneUrl(pane.id, url) },
        onMoreClicked = { pane, _ -> showPaneMenu(pane) }
    )

    private fun setupSplit() {
        split = SplitLayoutController(
            context = this,
            splitContainer = findViewById(R.id.split_container),
            secondaryGroup = findViewById(R.id.secondary_group),
            panes = panes.map { it.container },
            primaryDivider = findViewById(R.id.divider_handle),
            secondaryDivider = findViewById(R.id.divider_handle_2),
            primaryTooltip = findViewById(R.id.tv_ratio_tooltip),
            secondaryTooltip = findViewById(R.id.tv_ratio_tooltip_2),
            onDividerTapped = { showSplitOptions() },
            onRatioSettled = { primary, secondary ->
                prefs.splitRatio = primary
                prefs.secondaryRatio = secondary
            },
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

        val ratioButtons = listOf(
            findViewById<Button>(R.id.btn_ratio_3070) to 1f / 3f,
            findViewById<Button>(R.id.btn_ratio_5050) to 0.5f,
            findViewById<Button>(R.id.btn_ratio_7030) to 2f / 3f
        )
        ratioButtons.forEach { (button, value) ->
            button.setOnClickListener {
                split.applyRatio(value, persist = true)
                ratioButtons.forEach { (other, _) -> other.isSelected = other === button }
            }
        }
        ratioButtons[1].first.isSelected = true

        findViewById<Button>(R.id.btn_swap_panes).setOnClickListener { swapPanes(1, 2) }
        findViewById<Button>(R.id.btn_rotate_split).setOnClickListener { rotate() }
        findViewById<Button>(R.id.btn_launch_native_app).setOnClickListener { showAppPicker() }
        findViewById<Button>(R.id.btn_save_pair).setOnClickListener { promptSavePair() }
        findViewById<Button>(R.id.btn_saved_pairs).setOnClickListener { showPairsDialog() }

        btnLayoutTwo.setOnClickListener { setPaneCount(2) }
        btnLayoutThree.setOnClickListener { setPaneCount(3) }
    }

    private fun setupHomeScreen() {
        findViewById<View>(R.id.btn_mode_dual_web).setOnClickListener {
            showSplitWorkspace(showHint = true)
        }
        findViewById<View>(R.id.btn_mode_split_native).setOnClickListener {
            showSplitWorkspace(showHint = false)
            showAppPicker()
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val active = paneAt(activePane)
                when {
                    nativeGuideOverlay.visibility == View.VISIBLE -> hideNativeGuide()
                    panes.any { it.exitFullscreenIfNeeded() } -> Unit
                    split.maximizedPane != 0 -> split.restore()
                    active.canGoBack() -> active.goBack()
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

    private fun restoreLayout() {
        split.setVertical(prefs.verticalSplit)
        split.setPaneCount(prefs.paneCount)
        split.applyRatio(prefs.splitRatio, persist = false)
        split.applySecondaryRatio(prefs.secondaryRatio, persist = false)
        panes.forEach { it.setDesktopMode(prefs.paneDesktop(it.id), reload = false) }
        updateLayoutButtons()
    }

    /** Pages are fetched the first time the workspace is shown, not at launch. */
    private fun loadPanesIfNeeded() {
        val needed = split.paneCount
        panes.take(needed).forEach { pane ->
            if (!pane.hasLoaded) pane.load(prefs.paneUrl(pane.id))
        }
        panesLoaded = true
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
        loadPanesIfNeeded()
        if (showHint) showHint(getString(R.string.divider_hint))
    }

    private fun renderHomePairs() {
        homePairsList.removeAllViews()
        val pairs = pairManager.getSavedPairs()
        if (pairs.isEmpty()) {
            homePairsList.addView(
                TextView(this).apply {
                    setText(R.string.no_pairs_yet)
                    setTextColor(colorOf(R.color.text_tertiary))
                    textSize = 13f
                }
            )
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
        paneAt(1).load(pair.urlPane1)
        paneAt(2).load(pair.urlPane2)
        split.restore()
        showSplitWorkspace(showHint = false)
        showHint(pair.title)
    }

    // ---------------------------------------------------------------- panes

    private fun paneAt(id: Int): WebPane = panes[(id - 1).coerceIn(0, panes.lastIndex)]

    private fun setActivePane(id: Int) {
        activePane = id
        panes.forEach { it.setActive(it.id == id) }
    }

    private fun setPaneCount(count: Int) {
        if (count == 3 && split.paneCount != 3) {
            // Three panes read as "one full half, plus two stacked", which needs
            // the primary split to run left/right.
            split.setVertical(false)
            prefs.verticalSplit = false
        }
        split.setPaneCount(count)
        prefs.paneCount = count
        loadPanesIfNeeded()
        updateLayoutButtons()
        showHint(
            getString(
                if (count == 3) R.string.layout_switched_three else R.string.layout_switched_two
            )
        )
    }

    private fun updateLayoutButtons() {
        btnLayoutTwo.isSelected = split.paneCount == 2
        btnLayoutThree.isSelected = split.paneCount == 3
    }

    private fun rotate() {
        split.toggleDirection()
        prefs.verticalSplit = split.isVertical
        showHint(
            getString(if (split.isVertical) R.string.hint_stacked else R.string.hint_side_by_side)
        )
    }

    /** Exchanges what two panes are showing, keeping each pane's own chrome. */
    private fun swapPanes(a: Int, b: Int) {
        if (a == b || a > split.paneCount || b > split.paneCount) return
        val paneA = paneAt(a)
        val paneB = paneAt(b)

        val urlA = paneA.currentUrl
        val urlB = paneB.currentUrl
        val desktopA = paneA.isDesktopMode
        val desktopB = paneB.isDesktopMode

        paneA.setDesktopMode(desktopB, reload = false)
        paneB.setDesktopMode(desktopA, reload = false)
        paneA.load(urlB)
        paneB.load(urlA)

        prefs.setPaneDesktop(a, desktopB)
        prefs.setPaneDesktop(b, desktopA)
        showHint(getString(R.string.panes_swapped))
    }

    /** Human name for a slot, which depends on the current layout. */
    private fun positionName(pane: Int): String = getString(
        when {
            split.paneCount == 2 && split.isVertical ->
                if (pane == 1) R.string.pos_top else R.string.pos_bottom

            split.paneCount == 2 ->
                if (pane == 1) R.string.pos_left else R.string.pos_right

            // 3-pane, portrait (pane 1 = top half; pane 2 = bottom-left; pane 3 = bottom-right)
            split.isVertical -> when (pane) {
                1 -> R.string.pos_top_half
                2 -> R.string.pos_bottom_left
                else -> R.string.pos_bottom_right
            }

            // 3-pane, landscape (pane 1 = left half; pane 2 = top-right; pane 3 = bottom-right)
            else -> when (pane) {
                1 -> R.string.pos_left_half
                2 -> R.string.pos_top_right
                else -> R.string.pos_bottom_right
            }
        }
    )

    /** The panel's three-dot menu. */
    private fun showPaneMenu(pane: WebPane) {
        val actions = mutableListOf<Action>()
        val maximized = split.maximizedPane == pane.id

        actions += Action(
            R.drawable.ic_maximize,
            getString(if (maximized) R.string.menu_restore else R.string.menu_maximize)
        ) { split.toggleMaximize(pane.id) }

        // Move this pane's content to any other slot in the current layout.
        (1..split.paneCount).filter { it != pane.id }.forEach { target ->
            actions += Action(
                R.drawable.ic_swap,
                getString(R.string.menu_move_to, positionName(target))
            ) {
                swapPanes(pane.id, target)
                setActivePane(target)
                showHint(getString(R.string.pane_moved, positionName(target)))
            }
        }

        actions += if (split.paneCount == 2) {
            Action(R.drawable.ic_grid_three, getString(R.string.menu_add_pane)) { setPaneCount(3) }
        } else {
            Action(R.drawable.ic_close, getString(R.string.menu_close_pane)) { closePane(pane.id) }
        }

        actions += Action(
            if (pane.isDesktopMode) R.drawable.ic_phone else R.drawable.ic_desktop,
            getString(R.string.menu_desktop_site)
        ) {
            pane.setDesktopMode(!pane.isDesktopMode, reload = true)
            prefs.setPaneDesktop(pane.id, pane.isDesktopMode)
        }
        actions += Action(R.drawable.ic_copy, getString(R.string.menu_copy_url)) {
            copyToClipboard(pane.currentUrl)
        }
        actions += Action(R.drawable.ic_share, getString(R.string.menu_share)) {
            shareUrl(pane.currentUrl)
        }
        actions += Action(R.drawable.ic_open_in_new, getString(R.string.menu_open_browser)) {
            openExternally(pane.currentUrl)
        }

        showActionSheet(getString(paneTitle(pane.id)), actions)
    }

    private fun paneTitle(id: Int) = when (id) {
        1 -> R.string.pane_1
        2 -> R.string.pane_2
        else -> R.string.pane_3
    }

    /**
     * Closing a pane in a three-pane layout shuffles the survivors up so the
     * remaining two keep the slots the user expects.
     */
    private fun closePane(id: Int) {
        if (split.paneCount != 3) return
        when (id) {
            1 -> {
                swapPanes(1, 2)
                swapPanes(2, 3)
            }

            2 -> swapPanes(2, 3)
        }
        setPaneCount(2)
        setActivePane(if (id == 1) 1 else id.coerceAtMost(2))
        showHint(getString(R.string.pane_closed))
    }

    private fun showSplitOptions() {
        val actions = mutableListOf(
            Action(R.drawable.ic_swap, getString(R.string.quick_swap)) { swapPanes(1, 2) },
            Action(R.drawable.ic_grid_split, getString(R.string.quick_even)) {
                split.applyRatio(0.5f, persist = true)
                split.applySecondaryRatio(0.5f, persist = true)
            },
            Action(R.drawable.ic_maximize, getString(R.string.quick_max_1)) {
                split.toggleMaximize(activePane)
            },
            Action(R.drawable.ic_rotate, getString(R.string.quick_rotate)) { rotate() }
        )
        actions += if (split.paneCount == 2) {
            Action(R.drawable.ic_grid_three, getString(R.string.quick_three_panes)) {
                setPaneCount(3)
            }
        } else {
            Action(R.drawable.ic_grid_split, getString(R.string.quick_two_panes)) {
                setPaneCount(2)
            }
        }
        actions += Action(R.drawable.ic_star, getString(R.string.quick_save_pair)) {
            promptSavePair()
        }
        showActionSheet(getString(R.string.quick_menu_title), actions)
    }

    // ------------------------------------------------------------ app pairs

    private fun promptSavePair() {
        val input = EditText(this).apply {
            setHint(R.string.save_pair_hint)
            setText(
                getString(
                    R.string.pair_urls,
                    UrlUtils.host(paneAt(1).currentUrl),
                    UrlUtils.host(paneAt(2).currentUrl)
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
                    pairManager.savePair(name, paneAt(1).currentUrl, paneAt(2).currentUrl)
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
            bothInstalled && NativeSplitLauncher.canLaunchAdjacent(this) ->
                NativeSplitLauncher.launchPair(this, first.packageName!!, second.packageName!!)

            bothInstalled -> confirmSystemSplit(first, second)

            else -> {
                paneAt(1).load(first.url)
                paneAt(2).load(second.url)
                showSplitWorkspace(showHint = false)
                split.restore()
            }
        }
    }

    /**
     * Two installed apps, but the system will not accept an adjacent launch from
     * a full-screen app.  Show a glass overlay that walks the user through the
     * three steps they need to take to enter Android split-screen themselves.
     */
    private fun confirmSystemSplit(first: AppEntry, second: AppEntry) {
        showNativeGuide(first, second)
    }

    private fun showNativeGuide(first: AppEntry, second: AppEntry) {
        tvGuideStep1.text = getString(R.string.native_guide_step1, first.label)
        tvGuideStep3.text = getString(R.string.native_guide_step3, second.label)

        nativeGuideOverlay.visibility = View.VISIBLE
        nativeGuideOverlay.alpha = 0f
        nativeGuideOverlay.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        nativeGuideOverlay.findViewById<Button>(R.id.btn_guide_cancel).setOnClickListener {
            hideNativeGuide()
        }

        nativeGuideOverlay.findViewById<Button>(R.id.btn_guide_open).setOnClickListener {
            hideNativeGuide()
            NativeSplitLauncher.launchPair(this, first.packageName!!, second.packageName!!)
            showHint(getString(R.string.split_launch_hint))
        }

        // Tap outside the card dismisses the overlay
        nativeGuideOverlay.setOnClickListener { hideNativeGuide() }
    }

    private fun hideNativeGuide() {
        nativeGuideOverlay.animate()
            .alpha(0f)
            .setDuration(160)
            .withEndAction { nativeGuideOverlay.visibility = View.GONE }
            .start()
    }

    // -------------------------------------------------------------- helpers

    private data class Action(val iconRes: Int, val label: String, val run: () -> Unit)

    /** Glass action sheet used for the divider menu, panel menu and pair menu. */
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
        hintToast.animate().cancel()
        hintToast.text = message
        hintToast.visibility = View.VISIBLE
        hintToast.alpha = 0f
        hintToast.animate()
            .alpha(1f)
            .setDuration(140)
            .withEndAction {
                hintToast.postDelayed({
                    hintToast.animate()
                        .alpha(0f)
                        .setDuration(220)
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
                KeyEvent.KEYCODE_3 -> {
                    if (split.paneCount == 3) setActivePane(3)
                    return true
                }
                KeyEvent.KEYCODE_E -> { swapPanes(1, 2); return true }
                KeyEvent.KEYCODE_R -> { paneAt(activePane).reload(); return true }
                KeyEvent.KEYCODE_D -> { rotate(); return true }
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
        // Panes are only torn down with the activity, so media keeps playing
        // while the user works in another one.
        panes.forEach { it.destroy() }
        super.onDestroy()
    }

    private companion object {
        const val HINT_VISIBLE_MS = 2000L
    }
}
