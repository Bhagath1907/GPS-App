private fun startUpdateLoop() {
    Timer().scheduleAtFixedRate(object : TimerTask() {
        override fun run() {
            runOnUiThread {
                val service = gpsService ?: return@runOnUiThread
                
                // 1. Update Accuracy Display
                val acc = service.getLastAccuracy()
                if (acc != -1f) {
                    accuracyText.text = "Current Accuracy: ${String.format("%.1f", acc)}m"
                    accuracyProgress.progress = (100 - (acc * 5).toInt()).coerceIn(0, 100)
                    accuracyText.setTextColor(ContextCompat.getColor(this@MainActivity, 
                        if (acc <= 10) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
                }

                // 2. NEW: Sync Button State with Service
                if (service.isCurrentlyStreaming()) {
                    transmitButton.text = "STOP TRANSMISSION"
                    transmitButton.setBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
                } else {
                    transmitButton.text = "START TRANSMISSION"
                    transmitButton.setBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_dark))
                }

                // 3. Update Status Text
                val status = if (service.isCurrentlyStreaming()) "Status: STREAMING" else "Status: Standby"
                statusText.text = status
            }
        }
    }, 0, 1000)
}
