package com.example.gpsprovider

import android.location.Location
import java.text.SimpleDateFormat
import java.util.*

object NmeaUtils {
    // Permanent Fix: Strict formatting for UCL (Decimal precision and field counts)
    fun generateGga(l: Location): String {
        val time = SimpleDateFormat("HHmmss.ss", Locale.US).format(Date(l.time))
        val lat = convertToNmea(l.latitude, true)
        val lon = convertToNmea(l.longitude, false)
        val alt = String.format(Locale.US, "%.1f", l.altitude)
        
        // Standard GGA: $GPGGA,Time,Lat,N,Lon,E,Quality,Sats,HDOP,Alt,M,Geoid,M,Age,Station*CS
        val raw = "GPGGA,$time,$lat,$lon,1,08,1.0,$alt,M,0.0,M,,"
        return "$${raw}*${calculateChecksum(raw)}"
    }

    fun generateRmc(l: Location): String {
        val time = SimpleDateFormat("HHmmss.ss", Locale.US).format(Date(l.time))
        val lat = convertToNmea(l.latitude, true)
        val lon = convertToNmea(l.longitude, false)
        val speed = String.format(Locale.US, "%.1f", l.speed * 1.94384)
        val bear = String.format(Locale.US, "%.1f", l.bearing)
        val date = SimpleDateFormat("ddMMyy", Locale.US).format(Date(l.time))
        
        // Standard RMC: $GPRMC,Time,A,Lat,N,Lon,E,Speed,Course,Date,MagVar,Dir*CS
        val raw = "GPRMC,$time,A,$lat,$lon,$speed,$bear,$date,,,"
        return "$${raw}*${calculateChecksum(raw)}"
    }

    private fun convertToNmea(coord: Double, isLat: Boolean): String {
        val abs = Math.abs(coord)
        val deg = Math.floor(abs).toInt()
        val min = (abs - deg) * 60.0
        val dir = if (isLat) (if (coord >= 0) "N" else "S") else (if (coord >= 0) "E" else "W")
        return String.format(Locale.US, if (isLat) "%02d%07.4f,%s" else "%03d%07.4f,%s", deg, min, dir)
    }

    private fun calculateChecksum(s: String): String {
        var cs = 0
        for (c in s) { cs = cs xor c.toInt() }
        return String.format("%02X", cs)
    }
}
