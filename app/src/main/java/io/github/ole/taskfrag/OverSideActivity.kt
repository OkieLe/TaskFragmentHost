package io.github.ole.taskfrag

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class OverSideActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_over_side)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gesture_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<Button>(R.id.start_external).setOnClickListener { startSettings() }
        findViewById<Button>(R.id.close_this).setOnClickListener { finish() }
    }

    private fun startSettings() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.setClassName("com.android.settings", "com.android.settings.Settings")
        startActivity(intent)
    }
}
