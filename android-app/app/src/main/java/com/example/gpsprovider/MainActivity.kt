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
import java.util.Timer
import java.util.TimerTask

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
            val service = gpsService
            if (service == null) {
                Toast.makeText(this, "Service not ready", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (service.isCurrentlyStreaming()) {
                service.stopStreaming()
                Toast.makeText(this, "Streaming Stopped", Toast.LENGTH_SHORT).show()
            } else {
                if (service.startStreaming()) {
                    Toast.makeText(this, "Streaming Started", Toast.LENGTH_SHORT).show()
                } else {
                    val acc = service.getLastAccuracy()
                    if (acc > 15) {
                        Toast.makeText(this, "Accuracy too low (${acc}m > 15m)", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Check Bluetooth Connection", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Defer service start until permissions are granted
    }

    private fun checkPermissions() {
        val permissionsList = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN
        )
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val missing = permissionsList.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        } else {
            startGpsService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            startGpsService()
        }
    }

    private fun startGpsService() {
        try {
            val intent = Intent(this, GpsService::class.java)
            startForegroundService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to start service: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startUpdateLoop() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    val service = gpsService ?: return@runOnUiThread
                    
                    val acc = service.getLastAccuracy()
                    if (acc != -1f) {
                        accuracyText.text = "Current Accuracy: ${String.format("%.1f", acc)}m"
                        accuracyProgress.progress = (100 - (acc * 5).toInt()).coerceIn(0, 100)
                        accuracyText.setTextColor(ContextCompat.getColor(this@MainActivity, 
                            if (acc <= 10) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
                    }

                    if (service.isCurrentlyStreaming()) {
                        transmitButton.text = "STOP TRANSMISSION"
                        transmitButton.setBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                        statusText.text = "Status: STREAMING"
                    } else {
                        transmitButton.text = "START TRANSMISSION"
                        transmitButton.setBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
                        statusText.text = "Status: Standby"
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
