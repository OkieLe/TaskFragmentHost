package io.github.ole.taskfrag.side

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.ole.taskfrag.shared.DragLayout
import io.github.ole.taskfrag.shared.TaskHostController

class SideActivity : ComponentActivity() {
    companion object {
        private const val TAG = "SideActivity"
    }
    private val taskHostController by lazy { TaskHostController.get(this) }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed")
            if (taskHostController.onBackPressed().not()) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_side)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gesture_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        findViewById<Button>(R.id.start_external).setOnClickListener { startSettings() }
        findViewById<Button>(R.id.start_another).setOnClickListener { startAnother() }
        taskHostController.start()
        findViewById<DragLayout>(R.id.gesture_layout).setOnHorizontalDragListener(
            object : DragLayout.OnHorizontalDragListener {
                override fun onHorizontalDrag(distancePx: Int) {
                    Log.d(TAG, "onHorizontalDrag: $distancePx")
                    taskHostController.onScrolled(distancePx, true)
                }

                override fun onHorizontalDragEnd(endDistancePx: Int) {
                    Log.d(TAG, "onHorizontalDragEnd: $endDistancePx")
                    taskHostController.onScrolled(endDistancePx, false)
                }
            }
        )
    }

    private fun startSettings() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.setClassName("com.android.settings", "com.android.settings.Settings")
        startActivity(intent)
    }

    private fun startAnother() {
        val intent = Intent(this, OverSideActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        taskHostController.stop()
        super.onDestroy()
    }
}
