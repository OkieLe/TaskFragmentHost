package io.github.ole.taskfrag.shared

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * FrameLayout that detects **horizontal drag gestures** while letting its child
 * (e.g. a plain ScrollView) handle vertical scrolling normally.
 */
class DragLayout @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(ctx, attrs, defStyleAttr) {

    interface OnHorizontalDragListener {
        /**
         * @param distancePx the total distance in pixels
         *  positive to right, negative to left
         */
        fun onHorizontalDrag(distancePx: Int)

        /**
         * End of the drag
         * @param endDistancePx the end distance in pixels
         */
        fun onHorizontalDragEnd(endDistancePx: Int)
    }

    private var listener: OnHorizontalDragListener? = null
    fun setOnHorizontalDragListener(l: OnHorizontalDragListener?) { listener = l }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var startX = 0f
    private var startY = 0f
    private var accumulatedX = 0
    private var dragging = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.rawX
                startY = ev.rawY
                accumulatedX = 0
                dragging = false
                // Give child view e.g. ScrollView chance to handle vertical scrolling
                parent.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - startX
                val dy = ev.rawY - startY
                if (!dragging) {
                    if (abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                        dragging = true
                        // Handle the events in this view
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    val dx = ev.rawX - startX
                    // update accumulatedX
                    accumulatedX = dx.toInt()
                    listener?.onHorizontalDrag(accumulatedX)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // reset the state
                listener?.onHorizontalDragEnd(accumulatedX)
                accumulatedX = 0
                dragging = false
            }
        }
        return dragging || super.onTouchEvent(ev)
    }
}
