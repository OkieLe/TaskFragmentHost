package io.github.ole.taskfrag

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.window.TaskFragmentInfo
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.ole.taskfrag.shared.DragLayout
import io.github.ole.taskfrag.shared.TaskOverlayController

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val SCREEN_WIDTH = 1080
        private const val SCREEN_HEIGHT = 2400
        private const val ANIMATE_HIDE_DURATION = 150L
        private const val ANIMATE_BACK_DURATION = 300L
        private const val ANIMATE_SHOW_DURATION = 180L
    }
    private val taskOverlayController by lazy { TaskOverlayController.get(this) }
    private lateinit var taskFragmentController: TaskFragmentController

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed")
            moveTaskToBack(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<DragLayout>(R.id.main).setOnHorizontalDragListener(
            object : DragLayout.OnHorizontalDragListener {
                override fun onHorizontalDrag(distancePx: Int) {
                    if (distancePx > 0) {
                        dragTaskFragment(distancePx)
                    }
                }

                override fun onHorizontalDragEnd(endDistancePx: Int) {
                    endDragTaskFragment(endDistancePx)
                }
            }
        )
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        taskFragmentController = TaskFragmentController(
            this,
            { fragmentInfo ->
                logTaskFragmentState(fragmentInfo)
                handleTaskVisibilityChange()
            },
            { fragmentInfo ->
                logTaskFragmentState(fragmentInfo)
                handleTaskVisibilityChange()
            },
            {
                Log.i(TAG, "onTaskFragmentGone")
                handleTaskVisibilityChange()
            },
            mainExecutor
        ).apply { createTaskFragment() }
        taskOverlayController.start()
        taskOverlayController.setInputHandler(object : TaskOverlayController.InputHandler {
            override fun onBackPressed(): Boolean {
                return handleBackFromOverlay()
            }

            override fun onScrolled(scrollX: Int, scrolling: Boolean) {
                if (scrolling && scrollX < 0) {
                    dragTaskFragment(SCREEN_WIDTH + scrollX)
                } else if (!scrolling) {
                    Log.d(TAG, "Overlay scroll end $scrollX")
                    endDragTaskFragment(SCREEN_WIDTH + scrollX)
                }
            }
        })
    }

    private fun handleBackFromOverlay(): Boolean {
        Log.d(TAG, "handleBackFromOverlay ${taskFragmentController.lastActivityRemaining()}")
        return taskFragmentController.lastActivityRemaining().also {
            if (it) {
                mainExecutor.execute {
                    animateHideTaskFragment(SCREEN_WIDTH, ANIMATE_BACK_DURATION)
                }
            }
        }
    }

    private fun dragTaskFragment(distancePx: Int) {
        beforeShowTaskFragment()
        Log.i(TAG, "dragTaskFragment $distancePx")
        mainExecutor.execute {
            offsetTaskFragment(distancePx)
        }
    }

    private fun offsetTaskFragment(right: Int) {
        val rightRounded = right.coerceAtMost(SCREEN_WIDTH).coerceAtLeast(0)
        taskFragmentController.moveTaskFragment(
            Rect(rightRounded - SCREEN_WIDTH, 0, rightRounded, SCREEN_HEIGHT)
        )
    }

    private fun endDragTaskFragment(endDistancePx: Int) {
        mainExecutor.execute {
            if (endDistancePx < SCREEN_WIDTH / 2) {
                animateHideTaskFragment(endDistancePx)
            } else {
                animateShowTaskFragment(endDistancePx)
            }
        }
    }

    private fun animateHideTaskFragment(startX: Int, duration: Long = ANIMATE_HIDE_DURATION) {
        val animator = ValueAnimator.ofInt(startX, 0)
            .also {
                it.duration = duration
                it.interpolator = AccelerateInterpolator()
            }
        animator.addUpdateListener {
            offsetTaskFragment(it.animatedValue as Int)
        }
        animator.addListener(
            onEnd = {
                afterHideTaskFragment()
            }
        )
        animator.start()
    }

    private fun animateShowTaskFragment(startX: Int, duration: Long = ANIMATE_SHOW_DURATION) {
        val animator = ValueAnimator.ofInt(startX, SCREEN_WIDTH)
            .also {
                it.duration = duration
                it.interpolator = DecelerateInterpolator()
            }
        animator.addUpdateListener {
            offsetTaskFragment(it.animatedValue as Int)
        }
        animator.start()
    }

    private fun startSideActivity() {
        taskFragmentController.startActivityInTaskFragment(
            Intent(Intent.ACTION_MAIN).apply {
                setClassName("io.github.ole.taskfrag.side",
                    "io.github.ole.taskfrag.side.SideActivity")
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
    }

    private fun beforeShowTaskFragment() {
        if (!taskFragmentController.isFragmentOnTop()) {
            taskFragmentController.reorderToTop()
        }
        if (taskFragmentController.isFragmentEmpty()) {
            startSideActivity()
        }
    }

    private fun afterHideTaskFragment() {
        taskFragmentController.reorderToBottom()
    }

    private fun logTaskFragmentState(info: TaskFragmentInfo) {
        info.activities
        Log.d(TAG, "TaskFragment: vis=${info.isVisible}," +
                " top=${info.isTopNonFinishingChild}, empty=${info.isEmpty}," +
                " runCnt=${info.runningActivityCount}")
    }

    private fun handleTaskVisibilityChange() {
        val visible = taskFragmentController.isFragmentOnTop() && !taskFragmentController.isFragmentEmpty()
        taskOverlayController.setInputInterceptable(visible)
    }

    override fun onDestroy() {
        taskOverlayController.stop()
        onBackPressedCallback.remove()
        taskFragmentController.destroy()
        super.onDestroy()
    }
}
