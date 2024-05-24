package com.example.kotlin_hw3

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class CustomBluetoothService : Service() {

    private lateinit var customNotificationChannelId: String
    private lateinit var customNotificationManager: NotificationManagerCompat


    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val customBluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return START_NOT_STICKY

        val isEnabled = customBluetoothAdapter.isEnabled
        val status = if (isEnabled) "Connected" else "Disconnected"

        // Broadcast the custom Bluetooth status
        val broadcastIntent = Intent(ACTION_CUSTOM_BLUETOOTH_STATUS)
        broadcastIntent.putExtra(EXTRA_STATUS, status)
        sendBroadcast(broadcastIntent)

        // Show the custom Bluetooth notification
        showCustomBluetoothNotification(status)

        return START_STICKY
    }

    // BroadcastReceiver to listen for Bluetooth state changes
    private val customBluetoothStateReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            val status = when (state) {
                BluetoothAdapter.STATE_ON -> "Connected"
                BluetoothAdapter.STATE_OFF -> "Disconnected"
                else -> "Unknown"
            }
            // Broadcast the custom Bluetooth status
            val broadcastIntent: Intent = Intent(ACTION_CUSTOM_BLUETOOTH_STATUS)
            broadcastIntent.putExtra(EXTRA_STATUS, status)
            sendBroadcast(broadcastIntent)
            // Show the custom Bluetooth notification
            showCustomBluetoothNotification(status)
        }
    }

    // Show a notification about the Bluetooth status
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showCustomBluetoothNotification(status: String) {
        createCustomNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, customNotificationChannelId)
            .setContentTitle("Custom Bluetooth Status")
            .setContentText("Bluetooth is $status")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        customNotificationManager.notify(1, notification)
    }

    // Create the notification channel for custom Bluetooth status
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createCustomNotificationChannel() {

        customNotificationManager = NotificationManagerCompat.from(this)
        customNotificationChannelId = "custom_bluetooth_channel"
        val channelName = "Custom Bluetooth Status"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(customNotificationChannelId, channelName, importance)
        customNotificationManager.createNotificationChannel(channel)
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(customBluetoothStateReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the custom Bluetooth BroadcastReceiver
        unregisterReceiver(customBluetoothStateReceiver)
    }

    companion object {
        // Constants for custom Bluetooth actions and extras
        const val ACTION_CUSTOM_BLUETOOTH_STATUS = "action.CUSTOM_BLUETOOTH_STATUS"
        const val EXTRA_STATUS = "extra.STATUS"
    }
}
