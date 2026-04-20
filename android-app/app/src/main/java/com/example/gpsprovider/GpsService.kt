package com.example.gpsprovider

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
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

    inner class LocalBinder : Binder() {
        fun getService(): GpsService = this@GpsService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification("Waiting for connection..."))
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationUpdates()
        startBluetoothServer()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "GPS Provider Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Bluetooth Provider")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                lastLocation = result.lastLocation
                updateNotification()
            }
        }, mainLooper)
    }

    private fun updateNotification() {
        val accuracy = lastLocation?.accuracy ?: -1f
        val status = if (bluetoothSocket?.isConnected == true) "Connected" else "Standby"
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, createNotification("Status: $status | Accuracy: ${accuracy}m"))
    }

   private fun startBluetoothServer() {
    Thread {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        try {
            serverSocket = adapter.listenUsingRfcommWithServiceRecord("GpsProvider", SPP_UUID)
            while (isRunning.get()) {
                Log.d(TAG, "Waiting for Bluetooth connection...")
                val socket = serverSocket?.accept() // The 'Waiter' waits here
                
                if (socket != null) {
                    bluetoothSocket = socket
                    Log.d(TAG, "Bluetooth connected!")
                    updateNotification()
                    
                    // NEW STABILITY LOGIC:
                    // Wait here and don't look for new customers until this one leaves
                    while (socket.isConnected && isRunning.get()) {
                        Thread.sleep(1000) 
                    }
                    Log.d(TAG, "Client disconnected, waiting for next...")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Socket error", e)
        }
    }.start()
}

    private val isStreaming = AtomicBoolean(false)

    fun startStreaming(): Boolean {
        val socket = bluetoothSocket
        if (socket == null || !socket.isConnected) return false
        
        isStreaming.set(true)
        
        // We use an Asynchronous thread for streaming.
        // Asynchronous (ay-sin-kruh-nuhs) means the phone does this in the background 
        // while the screen stays active and responsive to you!
        Thread {
            try {
                val outputStream = socket.outputStream
                while (isStreaming.get() && socket.isConnected) {
                    val location = lastLocation
                    
                    // Accuracy Check: We only transmit if the data is precise (<= 10m).
                    if (location != null && location.accuracy <= 10) {
                        val gga = NmeaUtils.generateGga(location)
                        val rmc = NmeaUtils.generateRmc(location)
                        
                        // Sending NMEA sentences with a Checksum.
                        // Checksum (chek-suhm) is like a secret code at the end of the message 
                        // that helps the PC check if the data got scrambled during the trip!
                        outputStream.write("$gga\r\n".toByteArray())
                        outputStream.write("$rmc\r\n".toByteArray())
                        outputStream.flush()
                    }
                    
                    // We sleep for 1000ms to reduce Latency and prevent data flooding.
                    // Latency (lay-ten-see) is the tiny delay or 'waiting time' between 
                    // when data is sent and when it arrives!
                    Thread.sleep(1000)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Streaming failed", e)
                stopStreaming()
            }
        }.start()
        
        return true
    }

    fun stopStreaming() {
        isStreaming.set(false)
        updateNotification()
    }

    fun isCurrentlyStreaming(): Boolean = isStreaming.get()

    fun getLastAccuracy(): Float = lastLocation?.accuracy ?: -1f

    override fun onDestroy() {
        isRunning.set(false)
        try {
            serverSocket?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {}
        super.onDestroy()
    }
}
