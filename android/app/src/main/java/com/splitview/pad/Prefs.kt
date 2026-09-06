package com.splitview.pad

import android.content.Context

/**
 * Remembers the workspace between launches: what each pane was showing, how the
 * split was arranged and which mode the user was last in.
 */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("split_pad_session", Context.MODE_PRIVATE)

    fun paneUrl(pane: Int): String =
        sp.getString(keyUrl(pane), defaultUrl(pane)) ?: defaultUrl(pane)

    fun setPaneUrl(pane: Int, url: String) = sp.edit().putString(keyUrl(pane), url).apply()

    fun paneDesktop(pane: Int): Boolean = sp.getBoolean(keyDesktop(pane), false)

    fun setPaneDesktop(pane: Int, desktop: Boolean) =
        sp.edit().putBoolean(keyDesktop(pane), desktop).apply()

    /** Fraction of the split taken by pane 1, between [MIN_RATIO] and [MAX_RATIO]. */
    var splitRatio: Float
        get() = sp.getFloat(KEY_RATIO, 0.5f).coerceIn(MIN_RATIO, MAX_RATIO)
        set(value) = sp.edit().putFloat(KEY_RATIO, value.coerceIn(MIN_RATIO, MAX_RATIO)).apply()

    /** Fraction of the second half taken by pane 2, when three panes are shown. */
    var secondaryRatio: Float
        get() = sp.getFloat(KEY_RATIO_2, 0.5f).coerceIn(MIN_RATIO, MAX_RATIO)
        set(value) = sp.edit().putFloat(KEY_RATIO_2, value.coerceIn(MIN_RATIO, MAX_RATIO)).apply()

    /** true = the primary split runs top/bottom, false = side by side. */
    var verticalSplit: Boolean
        get() = sp.getBoolean(KEY_VERTICAL, true)
        set(value) = sp.edit().putBoolean(KEY_VERTICAL, value).apply()

    /** 2 or 3 — how many panes the workspace is showing. */
    var paneCount: Int
        get() = sp.getInt(KEY_PANE_COUNT, 2).coerceIn(2, 3)
        set(value) = sp.edit().putInt(KEY_PANE_COUNT, value.coerceIn(2, 3)).apply()

    /** true once the user has opened the split workspace at least once. */
    var resumeInSplit: Boolean
        get() = sp.getBoolean(KEY_RESUME_SPLIT, false)
        set(value) = sp.edit().putBoolean(KEY_RESUME_SPLIT, value).apply()

    private fun keyUrl(pane: Int) = "pane${pane}_url"

    private fun keyDesktop(pane: Int) = "pane${pane}_desktop"

    private fun defaultUrl(pane: Int) = when (pane) {
        1 -> DEFAULT_URL_1
        2 -> DEFAULT_URL_2
        else -> DEFAULT_URL_3
    }

    companion object {
        const val MIN_RATIO = 0.15f
        const val MAX_RATIO = 0.85f
        const val DEFAULT_URL_1 = "https://m.youtube.com"
        const val DEFAULT_URL_2 = "https://www.google.com"
        const val DEFAULT_URL_3 = "https://www.wikipedia.org"

        private const val KEY_RATIO = "split_ratio"
        private const val KEY_RATIO_2 = "split_ratio_secondary"
        private const val KEY_VERTICAL = "split_vertical"
        private const val KEY_PANE_COUNT = "pane_count"
        private const val KEY_RESUME_SPLIT = "resume_split"
    }
}
