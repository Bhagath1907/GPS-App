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
            while (isRunning.get()) {
                try {
                    if (serverSocket == null) {
                        serverSocket = adapter.listenUsingRfcommWithServiceRecord("GpsProvider", SPP_UUID)
                    }
                    val socket = serverSocket?.accept()
                    if (socket != null) {
                        try { bluetoothSocket?.close() } catch (e: Exception) {}
                        bluetoothSocket = socket
                        updateNotification()
                        
                        // Handle client disconnection in a separate thread so server can keep accepting
                        Thread {
                            val buffer = ByteArray(1024)
                            try {
                                val inputStream = socket.inputStream
                                while (socket.isConnected && isRunning.get()) {
                                    if (inputStream.read(buffer) == -1) break // Connection closed by peer
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Client disconnected", e)
                            }
                            
                            try { socket.close() } catch (e: Exception) {}
                            if (bluetoothSocket == socket) {
                                bluetoothSocket = null
                                stopStreaming() // Stop streaming if client disconnects
                                updateNotification()
                            }
                        }.start()
                    }
                } catch (e: Exception) { 
                    Log.e(TAG, "Server Error", e)
                    try { serverSocket?.close() } catch (ignored: Exception) {}
                    serverSocket = null
                    // Wait a bit before retrying to avoid tight loop on failure
                    try { Thread.sleep(2000) } catch (ignored: Exception) {}
                }
            }
        }.start()
    }

    fun startStreaming(): Boolean {
        if (isStreaming.get()) return true // Already running
        isStreaming.set(true)
        updateNotification()

        Thread {
            while (isStreaming.get()) {
                val socket = bluetoothSocket
                if (socket != null && socket.isConnected) {
                    try {
                        val out = socket.outputStream
                        val loc = lastLocation
                        // Precise timing (1Hz) for UCL compatibility
                        if (loc != null && loc.accuracy <= 15) {
                            val gga = NmeaUtils.generateGga(loc)
                            val gsa = NmeaUtils.generateGsa()
                            val gsv = NmeaUtils.generateGsv()
                            val rmc = NmeaUtils.generateRmc(loc)
                            out.write("$gga\r\n$gsa\r\n$gsv\r\n$rmc\r\n".toByteArray())
                            out.flush()
                        }
                    } catch (e: Exception) { 
                        try { socket.close() } catch (ignored: Exception) {}
                        if (bluetoothSocket == socket) {
                            bluetoothSocket = null
                            updateNotification()
                        }
                    }
                }
                try { Thread.sleep(1000) } catch (ignored: Exception) {}
            }
        }.start()
        return true
    }

    fun stopStreaming() { isStreaming.set(false); updateNotification() }
    fun isCurrentlyStreaming(): Boolean = isStreaming.get()
    fun isClientConnected(): Boolean = bluetoothSocket?.isConnected == true
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
        val status = if (isStreaming.get()) {
            if (isClientConnected()) "TRANSMITTING" else "WAITING FOR PC"
        } else "STANDBY"
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
