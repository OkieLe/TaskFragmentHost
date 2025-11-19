package io.github.ole.taskfrag

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val SCREEN_WIDTH = 1080
        private const val SCREEN_HEIGHT = 2400
        private const val ANIMATE_HIDE_DURATION = 150L
        private const val ANIMATE_SHOW_DURATION = 180L
    }
    private val taskOverlayController by lazy { TaskOverlayController.get(this) }
    private lateinit var taskFragmentController: TaskFragmentController

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
        taskFragmentController = TaskFragmentController(
            this,
            { _ ->
                startSideActivity()
            },
            {
            },
            {},
            mainExecutor
        ).apply { createTaskFragment() }
        taskOverlayController.start()
        taskOverlayController.setBackHandler { handleBackPressed() }
        taskOverlayController.setScrollHandler {  }
    }

    private fun dragTaskFragment(distancePx: Int) {
        beforeShowTaskFragment()
        Log.i(TAG, "dragTaskFragment $distancePx")
        offsetTaskFragment(distancePx)
    }

    private fun offsetTaskFragment(right: Int) {
        val rightRounded = right.coerceAtMost(SCREEN_WIDTH).coerceAtLeast(0)
        taskFragmentController.moveTaskFragment(
            Rect(rightRounded - SCREEN_WIDTH, 0, rightRounded, SCREEN_HEIGHT)
        )
    }

    private fun endDragTaskFragment(endDistancePx: Int) {
        if (endDistancePx < SCREEN_WIDTH / 2) {
            animateHideTaskFragment(endDistancePx)
        } else {
            animateShowTaskFragment(endDistancePx)
        }
    }

    private fun animateHideTaskFragment(startX: Int) {
        val animator = ValueAnimator.ofInt(startX, 0)
            .apply {
                duration = ANIMATE_HIDE_DURATION
                interpolator = AccelerateInterpolator()
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

    private fun animateShowTaskFragment(startX: Int) {
        val animator = ValueAnimator.ofInt(startX, SCREEN_WIDTH)
            .apply {
                duration = ANIMATE_SHOW_DURATION
                interpolator = DecelerateInterpolator()
            }
        animator.addUpdateListener {
            offsetTaskFragment(it.animatedValue as Int)
        }
        animator.start()
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

    private fun handleBackPressed(): Boolean {
        return true
    }

    private fun startSideActivity() {
        taskFragmentController.startActivityInTaskFragment(
            Intent(this, SideActivity::class.java))
    }

    override fun onDestroy() {
        taskOverlayController.stop()
        super.onDestroy()
    }
}
