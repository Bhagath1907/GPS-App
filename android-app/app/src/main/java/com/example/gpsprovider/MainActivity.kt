package com.example.gpsprovider

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var gpsService: GpsService? = null
    private var isBound = false
    
    private lateinit var accuracyText: TextView
    private lateinit var accuracyProgress: ProgressBar
    private lateinit var transmitButton: Button
    private lateinit var statusText: TextView

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as GpsService.LocalBinder
            gpsService = binder.getService()
            isBound = true
            startUpdateLoop()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        accuracyText = findViewById(R.id.accuracyText)
        accuracyProgress = findViewById(R.id.accuracyProgress)
        transmitButton = findViewById(R.id.transmitButton)
        statusText = findViewById(R.id.statusText)

        checkPermissions()

        transmitButton.setOnClickListener {
            if (gpsService?.transmitBurst() == true) {
                Toast.makeText(this, "Transmitting burst...", Toast.LENGTH_SHORT).show()
            } else {
                val acc = gpsService?.getLastAccuracy() ?: -1f
                if (acc > 10) {
                    Toast.makeText(this, "Accuracy too low (${acc}m > 10m)", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Not connected or GPS not ready", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val intent = Intent(this, GpsService::class.java)
        startForegroundService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN
        )
        
        val missing = permissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }

    private fun startUpdateLoop() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    val acc = gpsService?.getLastAccuracy() ?: -1f
                    if (acc != -1f) {
                        accuracyText.text = "Current Accuracy: ${String.format("%.1f", acc)}m"
                        accuracyProgress.progress = (100 - (acc * 5).toInt()).coerceIn(0, 100)
                        
                        if (acc <= 10) {
                            accuracyText.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
                        } else {
                            accuracyText.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                        }
                    }
                }
            }
        }, 0, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
