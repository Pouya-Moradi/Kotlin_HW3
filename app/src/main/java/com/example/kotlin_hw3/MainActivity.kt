package com.example.kotlin_hw3

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.FileObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class MainActivity : ComponentActivity() {

    // Custom Bluetooth status state
    private var customBluetoothStatus by mutableStateOf("Disconnected")

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeInitialWork(this)
        val permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)

        // If the permission is not granted, request it.
        if (permissionState == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        } else {
            // Permission is already granted, create the notification channel
            createNotificationChannel()
        }

        // Set the content of the activity
        setContent {
            DisplayLogScreen(this)
//            DisplayCustomBluetoothStatus(customBluetoothStatus)
        }

        // BroadcastReceiver for custom Bluetooth status updates
        val customBluetoothStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Get the custom Bluetooth status from the intent
                val status = intent.getStringExtra(CustomBluetoothService.EXTRA_STATUS) ?: "Disconnected"
                // Update the custom Bluetooth status
                customBluetoothStatus = status
            }
        }

        // Register the BroadcastReceiver
        val filter = IntentFilter(CustomBluetoothService.ACTION_CUSTOM_BLUETOOTH_STATUS)
        registerReceiver(customBluetoothStatusReceiver, filter, RECEIVER_NOT_EXPORTED)

        // Start the Bluetooth service using lifecycleScope
        lifecycleScope.launch {
            startService(Intent(this@MainActivity, CustomBluetoothService::class.java))
        }
    }

    // BroadcastReceiver for custom Bluetooth status updates
    private val customBluetoothStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get the custom Bluetooth status from the intent
            val status = intent.getStringExtra(CustomBluetoothService.EXTRA_STATUS) ?: "Disconnected"
            // Update the custom Bluetooth status
            customBluetoothStatus = status
        }
    }

    // Create a notification channel for custom Bluetooth status notifications
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        // Create a notification channel
        val channelId = "custom_bluetooth_channel"
        val channelName = "Custom Bluetooth Status"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the custom Bluetooth status BroadcastReceiver
        unregisterReceiver(customBluetoothStatusReceiver)
    }
}

// Function to read logs from a file
fun readLogsFromFile(context: Context): List<String> {
    val logFile = File(context.filesDir, "status_logs.txt")
    val logs = mutableListOf<String>()

    if (logFile.exists()) {
        logFile.useLines { lines ->
            lines.forEach { line ->
                val jsonObject = JSONObject(line)
                val timestamp = jsonObject.getString("timestamp")
                val bluetoothEnabled = jsonObject.getBoolean("custom_bluetooth_enabled")
                val airplaneModeOn = jsonObject.getBoolean("airplane_mode_on")

                val logMessage =
                    "time: ${timestamp}\nBluetooth is ${if (bluetoothEnabled) "Enabled" else "Disabled"},\nAirplane mode is" + " ${
                        if
                                (airplaneModeOn) "On" else "Off"
                    }"
                logs.add(logMessage)
            }
        }
    }

    return logs.reversed()
}

// Class to observe changes in the log file
class LogFileObserver(
    private val context: Context,
    private val callback: () -> Unit
) : FileObserver(context.filesDir.path + "/status_logs.txt", CREATE or MODIFY) {

    override fun onEvent(event: Int, path: String?) {
        if (event == CREATE || event == MODIFY) {
            // Call the callback function when the log file is created or modified
            callback()
        }
    }
}

@Composable
fun DisplayLogScreen(context: Context) {
    // State for storing logs
    val logs = remember { mutableStateOf(readLogsFromFile(context)) }

    // Create a file observer to watch for changes in the log file
    val observer = remember {
        LogFileObserver(context) {
            logs.value = readLogsFromFile(context)
        }
    }

    // Start watching for changes when the composable is first composed
    DisposableEffect(context) {
        observer.startWatching()
        // Stop watching for changes when the composable is disposed
        onDispose {
            observer.stopWatching()
        }
    }

    // Display logs in a lazy column
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        items(logs.value) { log ->
            Text(text = log)
            Divider()
        }
    }
}

// Composable function to display custom Bluetooth status
@Composable
fun DisplayCustomBluetoothStatus(status: String) {
    Column(
        modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Custom Bluetooth Status: $status")
    }
}

// Preview function for DisplayCustomBluetoothStatus composable
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DisplayCustomBluetoothStatus("Disconnected")
}
