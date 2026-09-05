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
 * The split itself: ratio, direction, the draggable divider and full-screening
 * one half.
 *
 * Gestures on the divider follow the conventions people already know from
 * One UI / HyperOS:
 *   - drag            → resize, with a live percentage readout
 *   - drag to the end → that half goes full screen
 *   - double tap      → swap the two panes
 *   - single tap      → the split options menu
 *   - long press      → snap back to an even 50:50
 */
@SuppressLint("ClickableViewAccessibility")
class SplitLayoutController(
    context: Context,
    private val splitContainer: LinearLayout,
    private val pane1: View,
    private val pane2: View,
    private val divider: View,
    private val ratioTooltip: TextView,
    private val onSwapRequested: () -> Unit,
    private val onDividerTapped: () -> Unit,
    private val onRatioSettled: (Float) -> Unit,
    private val onMaximizedChanged: (Int) -> Unit
) {

    /** 0 = both panes visible, 1 or 2 = that pane is full screen. */
    var maximizedPane: Int = 0
        private set

    var ratio: Float = 0.5f
        private set

    var isVertical: Boolean = true
        private set

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val dividerThicknessPx = (22 * context.resources.displayMetrics.density).toInt()

    private var dragging = false
    private var downPos = 0f
    private var startRatio = 0.5f

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onDividerTapped()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                onSwapRequested()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (maximizedPane != 0) restore() else applyRatio(0.5f, persist = true)
            }
        }
    )

    init {
        divider.setOnTouchListener { _, event -> handleDividerTouch(event) }
    }

    private fun handleDividerTouch(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        val pos = if (isVertical) event.rawY else event.rawX

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downPos = pos
                startRatio = ratio
                dragging = false
                divider.isSelected = true
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val delta = pos - downPos
                if (!dragging && kotlin.math.abs(delta) > touchSlop) {
                    dragging = true
                    // Dragging out of full screen puts the other pane back first.
                    if (maximizedPane != 0) restore()
                    startRatio = ratio
                    downPos = pos
                    showTooltip(true)
                }
                if (dragging) {
                    val total = (if (isVertical) splitContainer.height else splitContainer.width)
                        .toFloat() - dividerThicknessPx
                    if (total > 0f) {
                        applyRatio(startRatio + (pos - downPos) / total, persist = false)
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                divider.isSelected = false
                showTooltip(false)
                if (dragging) {
                    // Flicked all the way to an edge: full-screen that half.
                    when {
                        ratio <= EDGE_SNAP -> maximize(2)
                        ratio >= 1f - EDGE_SNAP -> maximize(1)
                        else -> onRatioSettled(ratio)
                    }
                }
                dragging = false
                return true
            }
        }
        return false
    }

    fun applyRatio(newRatio: Float, persist: Boolean) {
        if (maximizedPane != 0) restore()
        ratio = newRatio.coerceIn(Prefs.MIN_RATIO, Prefs.MAX_RATIO)
        setWeights(ratio, 1f - ratio)
        ratioTooltip.text = "${Math.round(ratio * 100)} : ${100 - Math.round(ratio * 100)}"
        if (persist) onRatioSettled(ratio)
    }

    fun maximize(pane: Int) {
        if (pane != 1 && pane != 2) return
        // A pane that was full-screened by dragging to the edge should come back
        // to an even split rather than to the 15% sliver it was dragged to.
        if (ratio <= Prefs.MIN_RATIO + 0.02f || ratio >= Prefs.MAX_RATIO - 0.02f) {
            ratio = 0.5f
            onRatioSettled(ratio)
        }
        maximizedPane = pane
        pane1.visibility = if (pane == 1) View.VISIBLE else View.GONE
        pane2.visibility = if (pane == 2) View.VISIBLE else View.GONE
        setWeights(if (pane == 1) 1f else 0f, if (pane == 2) 1f else 0f)
        onMaximizedChanged(pane)
    }

    fun restore() {
        if (maximizedPane == 0) return
        maximizedPane = 0
        pane1.visibility = View.VISIBLE
        pane2.visibility = View.VISIBLE
        setWeights(ratio, 1f - ratio)
        onMaximizedChanged(0)
    }

    fun toggleMaximize(pane: Int) {
        if (maximizedPane == pane) restore() else maximize(pane)
    }

    fun setVertical(vertical: Boolean) {
        isVertical = vertical
        splitContainer.orientation =
            if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL

        applyAxis(pane1)
        applyAxis(pane2)

        divider.layoutParams = divider.layoutParams.apply {
            if (vertical) {
                width = LinearLayout.LayoutParams.MATCH_PARENT
                height = dividerThicknessPx
            } else {
                width = dividerThicknessPx
                height = LinearLayout.LayoutParams.MATCH_PARENT
            }
        }
        splitContainer.requestLayout()
    }

    fun toggleDirection() = setVertical(!isVertical)

    private fun applyAxis(pane: View) {
        val lp = pane.layoutParams as LinearLayout.LayoutParams
        if (isVertical) {
            lp.width = LinearLayout.LayoutParams.MATCH_PARENT
            lp.height = 0
        } else {
            lp.width = 0
            lp.height = LinearLayout.LayoutParams.MATCH_PARENT
        }
        pane.layoutParams = lp
    }

    private fun setWeights(w1: Float, w2: Float) {
        (pane1.layoutParams as LinearLayout.LayoutParams).let {
            it.weight = w1
            pane1.layoutParams = it
        }
        (pane2.layoutParams as LinearLayout.LayoutParams).let {
            it.weight = w2
            pane2.layoutParams = it
        }
        splitContainer.requestLayout()
    }

    private fun showTooltip(show: Boolean) {
        ratioTooltip.visibility = if (show) View.VISIBLE else View.GONE
    }

    companion object {
        /** How close to an edge a drag has to end to full-screen a pane. */
        private const val EDGE_SNAP = 0.18f
    }
}
