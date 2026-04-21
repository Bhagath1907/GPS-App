package com.example.gpsprovider

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class GpsService : Service() {
    private val TAG = "GpsService"
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val CHANNEL_ID = "GpsProviderChannel"
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null
    private val isRunning = AtomicBoolean(true)
    private val isStreaming = AtomicBoolean(false)
    private var wakeLock: PowerManager.WakeLock? = null

    inner class LocalBinder : Binder() { fun getService(): GpsService = this@GpsService }
    private val binder = LocalBinder()
    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification("System Active"))
        
        // Permanent Fix: Prevent CPU from 'napping' which kills Bluetooth
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GpsProvider::Lock")
        wakeLock?.acquire()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
        startBluetoothServer()
    }

    private fun startBluetoothServer() {
        Thread {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            try {
                serverSocket = adapter.listenUsingRfcommWithServiceRecord("GpsProvider", SPP_UUID)
                while (isRunning.get()) {
                    val socket = serverSocket?.accept()
                    if (socket != null) {
                        bluetoothSocket = socket
                        updateNotification()
                        // Keep server alive but don't accept new ones while connected
                        while (socket.isConnected && isRunning.get()) { Thread.sleep(2000) }
                    }
                }
            } catch (e: IOException) { Log.e(TAG, "Server Error", e) }
        }.start()
    }

    fun startStreaming(): Boolean {
        val socket = bluetoothSocket
        if (socket == null || !socket.isConnected) return false
        if (isStreaming.get()) return true // Already running

        isStreaming.set(true)
        Thread {
            try {
                val out = socket.outputStream
                while (isStreaming.get() && socket.isConnected) {
                    val loc = lastLocation
                    // Permanent Fix: Precise timing (1Hz) for UCL compatibility
                    if (loc != null && loc.accuracy <= 10) {
                        val gga = NmeaUtils.generateGga(loc)
                        val rmc = NmeaUtils.generateRmc(loc)
                        out.write("$gga\r\n$rmc\r\n".toByteArray())
                        out.flush()
                    } else {
                        // Keep connection alive with a 'Null' NMEA comment 
                        // This keeps Windows drivers happy without confusing UCL
                        out.write("$--WAITING_FOR_ACCURACY*00\r\n".toByteArray())
                        out.flush()
                    }
                    Thread.sleep(1000) 
                }
            } catch (e: IOException) { stopStreaming() }
        }.start()
        return true
    }

    fun stopStreaming() { isStreaming.set(false); updateNotification() }
    fun isCurrentlyStreaming(): Boolean = isStreaming.get()
    fun getLastAccuracy(): Float = lastLocation?.accuracy ?: -1f

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "GPS Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aadhaar GPS Provider")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        fusedLocationClient.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) { 
                lastLocation = res.lastLocation
                updateNotification() 
            }
        }, mainLooper)
    }

    private fun updateNotification() {
        val acc = lastLocation?.accuracy ?: -1f
        val status = if (isStreaming.get()) "STREAMING" else "STANDBY"
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, createNotification("Status: $status | Accuracy: ${String.format("%.1f", acc)}m"))
    }

    override fun onDestroy() {
        isRunning.set(false)
        wakeLock?.release()
        try { serverSocket?.close(); bluetoothSocket?.close() } catch (e: Exception) {}
        super.onDestroy()
    }
}
