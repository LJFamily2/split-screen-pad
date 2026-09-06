package com.splitview.pad

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The split itself: how many panes, their ratios, the direction, the draggable
 * dividers and full-screening one pane.
 *
 * Two shapes are supported:
 *
 *   2 panes   pane 1 | pane 2
 *   3 panes   pane 1 | (pane 2 over pane 3)
 *
 * The secondary group always runs along the opposite axis to the primary split,
 * so three panes read as "one full half, plus two stacked in the other half".
 *
 * Divider gestures follow the conventions people know from One UI / HyperOS:
 *   - drag            → resize, with a live percentage readout
 *   - drag to the end → that pane goes full screen
 *   - tap             → the split options menu, opened on lift with no delay
 *   - long press      → snap back to an even split
 */
@SuppressLint("ClickableViewAccessibility")
class SplitLayoutController(
    private val context: Context,
    private val splitContainer: LinearLayout,
    private val secondaryGroup: LinearLayout,
    private val panes: List<View>,
    private val primaryDivider: View,
    private val secondaryDivider: View,
    private val primaryTooltip: TextView,
    private val secondaryTooltip: TextView,
    private val onDividerTapped: () -> Unit,
    private val onRatioSettled: (primary: Float, secondary: Float) -> Unit,
    private val onMaximizedChanged: (Int) -> Unit
) {

    /** 0 = every pane visible, otherwise the 1-based pane that is full screen. */
    var maximizedPane: Int = 0
        private set

    /** Fraction taken by pane 1 of the whole workspace. */
    var ratio: Float = 0.5f
        private set

    /** Fraction taken by pane 2 of the secondary half, when three panes are shown. */
    var secondaryRatio: Float = 0.5f
        private set

    var isVertical: Boolean = true
        private set

    var paneCount: Int = 2
        private set

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val dividerThicknessPx = (22 * context.resources.displayMetrics.density).toInt()

    private val pane1 get() = panes[0]
    private val pane2 get() = panes[1]
    private val pane3 get() = panes[2]

    init {
        attach(primaryDivider, primary = true)
        attach(secondaryDivider, primary = false)
    }

    // ------------------------------------------------------------- gestures

    private fun attach(divider: View, primary: Boolean) {
        val detector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    // Fires on lift, so the options sheet appears immediately
                    // rather than after a double-tap window has elapsed.
                    onDividerTapped()
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    if (maximizedPane != 0) restore()
                    else if (primary) applyRatio(0.5f, persist = true)
                    else applySecondaryRatio(0.5f, persist = true)
                }
            }
        )

        var dragging = false
        var downPos = 0f
        var startRatio = 0.5f

        divider.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            // The primary divider follows the main axis; the secondary one runs
            // across it, so each reads a different coordinate.
            val alongVerticalAxis = if (primary) isVertical else !isVertical
            val pos = if (alongVerticalAxis) event.rawY else event.rawX

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downPos = pos
                    startRatio = if (primary) ratio else secondaryRatio
                    dragging = false
                    divider.isSelected = true
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val delta = pos - downPos
                    if (!dragging && kotlin.math.abs(delta) > touchSlop) {
                        dragging = true
                        if (maximizedPane != 0) restore()
                        startRatio = if (primary) ratio else secondaryRatio
                        downPos = pos
                        showTooltip(primary, true)
                    }
                    if (dragging) {
                        val host = if (primary) splitContainer else secondaryGroup
                        val total = (if (alongVerticalAxis) host.height else host.width)
                            .toFloat() - dividerThicknessPx
                        if (total > 0f) {
                            val next = startRatio + (pos - downPos) / total
                            if (primary) applyRatio(next, persist = false)
                            else applySecondaryRatio(next, persist = false)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    divider.isSelected = false
                    showTooltip(primary, false)
                    if (dragging) {
                        if (primary) settlePrimary() else settleSecondary()
                    }
                    dragging = false
                    true
                }

                else -> false
            }
        }
    }

    private fun settlePrimary() {
        when {
            // Flicked all the way to an edge: full-screen the pane that survived.
            // With three panes the other half holds two panes, so there is no
            // single pane to promote — the drag just settles at the minimum.
            ratio <= Prefs.MIN_RATIO + EDGE_SNAP && paneCount == 2 -> maximize(2)
            ratio >= Prefs.MAX_RATIO - EDGE_SNAP -> maximize(1)
            else -> persistRatios()
        }
    }

    private fun settleSecondary() {
        when {
            secondaryRatio <= Prefs.MIN_RATIO + EDGE_SNAP -> maximize(3)
            secondaryRatio >= Prefs.MAX_RATIO - EDGE_SNAP -> maximize(2)
            else -> persistRatios()
        }
    }

    // -------------------------------------------------------------- layout

    fun applyRatio(newRatio: Float, persist: Boolean) {
        if (maximizedPane != 0) restore()
        ratio = newRatio.coerceIn(Prefs.MIN_RATIO, Prefs.MAX_RATIO)
        layoutWeights()
        primaryTooltip.text = percentLabel(ratio)
        if (persist) persistRatios()
    }

    fun applySecondaryRatio(newRatio: Float, persist: Boolean) {
        if (maximizedPane != 0) restore()
        secondaryRatio = newRatio.coerceIn(Prefs.MIN_RATIO, Prefs.MAX_RATIO)
        layoutWeights()
        secondaryTooltip.text = percentLabel(secondaryRatio)
        if (persist) persistRatios()
    }

    fun setPaneCount(count: Int) {
        paneCount = count.coerceIn(2, 3)
        if (maximizedPane > paneCount) maximizedPane = 0
        applyAxes()
        layoutWeights()
        updateVisibility()
    }

    fun setVertical(vertical: Boolean) {
        isVertical = vertical
        applyAxes()
        layoutWeights()
    }

    fun toggleDirection() = setVertical(!isVertical)

    fun maximize(pane: Int) {
        if (pane !in 1..paneCount) return
        // A pane full-screened by dragging to the edge should come back to an
        // even split rather than to the sliver it was dragged to.
        if (ratio <= Prefs.MIN_RATIO + 0.02f || ratio >= Prefs.MAX_RATIO - 0.02f) ratio = 0.5f
        if (secondaryRatio <= Prefs.MIN_RATIO + 0.02f ||
            secondaryRatio >= Prefs.MAX_RATIO - 0.02f
        ) {
            secondaryRatio = 0.5f
        }
        persistRatios()
        maximizedPane = pane
        updateVisibility()
        layoutWeights()
        onMaximizedChanged(pane)
    }

    fun restore() {
        if (maximizedPane == 0) return
        maximizedPane = 0
        updateVisibility()
        layoutWeights()
        onMaximizedChanged(0)
    }

    fun toggleMaximize(pane: Int) {
        if (maximizedPane == pane) restore() else maximize(pane)
    }

    // ------------------------------------------------------------ internals

    private fun applyAxes() {
        splitContainer.orientation =
            if (isVertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        // Two stacked panes read best across the primary axis.
        secondaryGroup.orientation =
            if (isVertical) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

        axis(pane1, isVertical)
        axis(secondaryGroup, isVertical)
        axis(pane2, !isVertical)
        axis(pane3, !isVertical)

        thickness(primaryDivider, isVertical)
        thickness(secondaryDivider, !isVertical)
        splitContainer.requestLayout()
    }

    /** Lays a child out along [vertical]: zero height and weighted, or zero width. */
    private fun axis(view: View, vertical: Boolean) {
        val lp = view.layoutParams as LinearLayout.LayoutParams
        if (vertical) {
            lp.width = LinearLayout.LayoutParams.MATCH_PARENT
            lp.height = 0
        } else {
            lp.width = 0
            lp.height = LinearLayout.LayoutParams.MATCH_PARENT
        }
        view.layoutParams = lp
    }

    private fun thickness(divider: View, vertical: Boolean) {
        divider.layoutParams = divider.layoutParams.apply {
            if (vertical) {
                width = LinearLayout.LayoutParams.MATCH_PARENT
                height = dividerThicknessPx
            } else {
                width = dividerThicknessPx
                height = LinearLayout.LayoutParams.MATCH_PARENT
            }
        }
    }

    private fun layoutWeights() {
        when (maximizedPane) {
            1 -> {
                weight(pane1, 1f)
                weight(secondaryGroup, 0f)
            }

            2 -> {
                weight(pane1, 0f)
                weight(secondaryGroup, 1f)
                weight(pane2, 1f)
                weight(pane3, 0f)
            }

            3 -> {
                weight(pane1, 0f)
                weight(secondaryGroup, 1f)
                weight(pane2, 0f)
                weight(pane3, 1f)
            }

            else -> {
                weight(pane1, ratio)
                weight(secondaryGroup, 1f - ratio)
                if (paneCount == 3) {
                    weight(pane2, secondaryRatio)
                    weight(pane3, 1f - secondaryRatio)
                } else {
                    weight(pane2, 1f)
                    weight(pane3, 0f)
                }
            }
        }
        splitContainer.requestLayout()
    }

    private fun weight(view: View, value: Float) {
        val lp = view.layoutParams as LinearLayout.LayoutParams
        if (lp.weight != value) {
            lp.weight = value
            view.layoutParams = lp
        }
    }

    private fun updateVisibility() {
        val threePanes = paneCount == 3
        pane1.visibility = visibilityFor(1)
        pane2.visibility = visibilityFor(2)
        pane3.visibility = if (threePanes) visibilityFor(3) else View.GONE

        primaryDivider.visibility = View.VISIBLE
        secondaryDivider.visibility = if (threePanes) View.VISIBLE else View.GONE
        secondaryGroup.visibility = if (maximizedPane == 1) View.GONE else View.VISIBLE
    }

    private fun visibilityFor(pane: Int) =
        if (maximizedPane == 0 || maximizedPane == pane) View.VISIBLE else View.GONE

    private fun persistRatios() = onRatioSettled(ratio, secondaryRatio)

    private fun showTooltip(primary: Boolean, show: Boolean) {
        val tooltip = if (primary) primaryTooltip else secondaryTooltip
        tooltip.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun percentLabel(value: Float): String {
        val left = Math.round(value * 100)
        return "$left : ${100 - left}"
    }

    private companion object {
        /** How close to an edge a drag has to end to full-screen a pane. */
        const val EDGE_SNAP = 0.03f
    }
}
