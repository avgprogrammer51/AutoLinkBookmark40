package com.example.autolinkbookmark

import android.content.Intent
import android.content.ComponentName
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        
        startButton.setOnClickListener { startExtraction() }
        stopButton.setOnClickListener { stopExtraction() }
        
        ShortcutDetectionService.callback = { message ->
            runOnUiThread {
                statusText.text = message
            }
        }
        
        ShortcutDetectionService.progressCallback = { current, total, _ ->
            runOnUiThread {
                progressBar.max = total
                progressBar.progress = current
            }
        }
    }
    
    private fun startExtraction() {
        if (!ShortcutDetectionService.isRunning) {
            showAccessibilityDialog()
            return
        }
        startButton.isEnabled = false
        ShortcutDetectionService().startExtraction()
    }
    
    private fun stopExtraction() {
        ShortcutDetectionService().stopExtraction()
        startButton.isEnabled = true
    }
    
    private fun showAccessibilityDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Enable Accessibility")
            .setMessage("Settings > Accessibility > AutoLinkBookmark > Enable")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .show()
    }
}
