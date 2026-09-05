package com.splitview.pad

import android.content.Context

/**
 * Remembers the workspace between launches: what each pane was showing, how the
 * split was arranged and which mode the user was last in. The previous build
 * threw all of this away on every cold start.
 */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("split_pad_session", Context.MODE_PRIVATE)

    var pane1Url: String
        get() = sp.getString(KEY_URL_1, DEFAULT_URL_1) ?: DEFAULT_URL_1
        set(value) = sp.edit().putString(KEY_URL_1, value).apply()

    var pane2Url: String
        get() = sp.getString(KEY_URL_2, DEFAULT_URL_2) ?: DEFAULT_URL_2
        set(value) = sp.edit().putString(KEY_URL_2, value).apply()

    /** Fraction of the split taken by pane 1, between [MIN_RATIO] and [MAX_RATIO]. */
    var splitRatio: Float
        get() = sp.getFloat(KEY_RATIO, 0.5f).coerceIn(MIN_RATIO, MAX_RATIO)
        set(value) = sp.edit().putFloat(KEY_RATIO, value.coerceIn(MIN_RATIO, MAX_RATIO)).apply()

    /** true = panes stacked top/bottom, false = side by side. */
    var verticalSplit: Boolean
        get() = sp.getBoolean(KEY_VERTICAL, true)
        set(value) = sp.edit().putBoolean(KEY_VERTICAL, value).apply()

    var pane1Desktop: Boolean
        get() = sp.getBoolean(KEY_DESKTOP_1, false)
        set(value) = sp.edit().putBoolean(KEY_DESKTOP_1, value).apply()

    var pane2Desktop: Boolean
        get() = sp.getBoolean(KEY_DESKTOP_2, false)
        set(value) = sp.edit().putBoolean(KEY_DESKTOP_2, value).apply()

    /** true once the user has opened the split workspace at least once. */
    var resumeInSplit: Boolean
        get() = sp.getBoolean(KEY_RESUME_SPLIT, false)
        set(value) = sp.edit().putBoolean(KEY_RESUME_SPLIT, value).apply()

    var floatingUrl: String
        get() = sp.getString(KEY_FLOAT_URL, DEFAULT_URL_2) ?: DEFAULT_URL_2
        set(value) = sp.edit().putString(KEY_FLOAT_URL, value).apply()

    var floatingAlpha: Float
        get() = sp.getFloat(KEY_FLOAT_ALPHA, 1.0f).coerceIn(0.4f, 1.0f)
        set(value) = sp.edit().putFloat(KEY_FLOAT_ALPHA, value.coerceIn(0.4f, 1.0f)).apply()

    var floatingX: Int
        get() = sp.getInt(KEY_FLOAT_X, 48)
        set(value) = sp.edit().putInt(KEY_FLOAT_X, value).apply()

    var floatingY: Int
        get() = sp.getInt(KEY_FLOAT_Y, 140)
        set(value) = sp.edit().putInt(KEY_FLOAT_Y, value).apply()

    var floatingWidth: Int
        get() = sp.getInt(KEY_FLOAT_W, 0)
        set(value) = sp.edit().putInt(KEY_FLOAT_W, value).apply()

    var floatingHeight: Int
        get() = sp.getInt(KEY_FLOAT_H, 0)
        set(value) = sp.edit().putInt(KEY_FLOAT_H, value).apply()

    companion object {
        const val MIN_RATIO = 0.15f
        const val MAX_RATIO = 0.85f
        const val DEFAULT_URL_1 = "https://m.youtube.com"
        const val DEFAULT_URL_2 = "https://www.google.com"

        private const val KEY_URL_1 = "pane1_url"
        private const val KEY_URL_2 = "pane2_url"
        private const val KEY_RATIO = "split_ratio"
        private const val KEY_VERTICAL = "split_vertical"
        private const val KEY_DESKTOP_1 = "pane1_desktop"
        private const val KEY_DESKTOP_2 = "pane2_desktop"
        private const val KEY_RESUME_SPLIT = "resume_split"
        private const val KEY_FLOAT_URL = "floating_url"
        private const val KEY_FLOAT_ALPHA = "floating_alpha"
        private const val KEY_FLOAT_X = "floating_x"
        private const val KEY_FLOAT_Y = "floating_y"
        private const val KEY_FLOAT_W = "floating_w"
        private const val KEY_FLOAT_H = "floating_h"
    }
}
