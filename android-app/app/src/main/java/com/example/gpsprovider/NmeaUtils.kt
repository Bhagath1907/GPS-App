package com.example.gpsprovider

import android.location.Location
import java.text.SimpleDateFormat
import java.util.*

object NmeaUtils {

    fun generateGga(location: Location): String {
        val time = SimpleDateFormat("HHmmss.ss", Locale.US).format(Date(location.time))
        val lat = convertToNmeaFormat(location.latitude, true)
        val lon = convertToNmeaFormat(location.longitude, false)
        val quality = 1 
        val satellites = 08 // Fixed to 2 digits for compatibility
        val hdop = 1.0 
        // FIX: Round altitude to 1 decimal place
        val alt = String.format(Locale.US, "%.1f", location.altitude)
        
        val sentence = "GPGGA,$time,$lat,$lon,$quality,$satellites,$hdop,$alt,M,0.0,M,,"
        return "$${sentence}*${calculateChecksum(sentence)}"
    }

    fun generateRmc(location: Location): String {
        val time = SimpleDateFormat("HHmmss.ss", Locale.US).format(Date(location.time))
        val status = "A" 
        val lat = convertToNmeaFormat(location.latitude, true)
        val lon = convertToNmeaFormat(location.longitude, false)
        // FIX: Round speed to 1 decimal place
        val speed = String.format(Locale.US, "%.1f", location.speed * 1.94384)
        val bearing = String.format(Locale.US, "%.1f", location.bearing)
        val date = SimpleDateFormat("ddMMyy", Locale.US).format(Date(location.time))
        
        val sentence = "GPRMC,$time,$status,$lat,$lon,$speed,$bearing,$date,,,"
        return "$${sentence}*${calculateChecksum(sentence)}"
    }

    private fun convertToNmeaFormat(coordinate: Double, isLatitude: Boolean): String {
        val absCoord = Math.abs(coordinate)
        val degrees = Math.floor(absCoord).toInt()
        val minutes = (absCoord - degrees) * 60.0
        
        val direction = if (isLatitude) {
            if (coordinate >= 0) "N" else "S"
        } else {
            if (coordinate >= 0) "E" else "W"
        }
        
        val degreeFormat = if (isLatitude) "%02d" else "%03d"
        return String.format(Locale.US, "$degreeFormat%07.4f,%s", degrees, minutes, direction)
    }

    private fun calculateChecksum(sentence: String): String {
        var checksum = 0
        for (char in sentence) {
            checksum = checksum xor char.toInt()
        }
        return String.format("%02X", checksum)
    }
}
