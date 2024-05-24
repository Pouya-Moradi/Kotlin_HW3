package com.example.kotlin_hw3

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CustomStatusCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    // Function to reschedule the work
    private fun rescheduleWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val statusCheckRequest = OneTimeWorkRequestBuilder<CustomStatusCheckWorker>()
            .setConstraints(constraints)
            .setInitialDelay(2, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(statusCheckRequest)
    }

    override fun doWork(): Result {
        // Check Bluetooth status
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val isBluetoothEnabled = bluetoothAdapter?.isEnabled ?: false

        // Check airplane mode status
        val isAirplaneModeOn = Settings.Global.getInt(
            applicationContext.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0

        // Log Bluetooth and airplane mode status
        Log.i("custom_worker_bluetooth", "Bluetooth is ${if (isBluetoothEnabled) "Enabled" else "Disabled"}")
        Log.i("custom_worker_airplane", "Airplane mode is ${if (isAirplaneModeOn) "On" else "Off"}")

        // Reschedule the work
        rescheduleWork()

        // Write status to log file
        writeLogToFile(isBluetoothEnabled, isAirplaneModeOn)

        return Result.success()
    }

    // Function to write status to log file
    private fun writeLogToFile(isBluetoothEnabled: Boolean, isAirplaneModeOn: Boolean) {
        val logFile = File(applicationContext.filesDir, "status_logs.txt")
        val jsonObject = JSONObject()
        jsonObject.put("timestamp", convertMillisToDate(System.currentTimeMillis()))
        jsonObject.put("custom_bluetooth_enabled", isBluetoothEnabled)
        jsonObject.put("airplane_mode_on", isAirplaneModeOn)
        val jsonString = jsonObject.toString() + "\n"
        logFile.appendText(jsonString)
    }

    // Function to convert milliseconds to date string
    private fun convertMillisToDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

// Function to schedule initial work
fun initializeInitialWork(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .build()

    val statusCheckRequest = OneTimeWorkRequestBuilder<CustomStatusCheckWorker>()
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueue(statusCheckRequest)
}
