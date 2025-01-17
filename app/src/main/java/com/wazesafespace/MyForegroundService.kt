package com.wazesafespace

import com.wazesafespace.MainActivity
import com.wazesafespace.MapFragment
import com.wazesafespace.R

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson

class MyForegroundService : Service() {

    private lateinit var database: DatabaseReference

    companion object {
        var isServiceRunning = false
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        isServiceRunning = true

        database = FirebaseDatabase.getInstance().reference
        // Reference to the alerts node
        val alertsRef = database.child("alerts")

        // Listen for real-time updates
        alertsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val alertData = snapshot.value as? Map<String, Any>
                val alertsList = alertData?.get("alerts") as? List<Map<String, Any>>

                if (alertsList != null) {
                    val beerShevaAlert = alertsList.any { alert ->
                        val cities = alert["cities"] as? String
                        cities?.contains("באר שבע") == true
                    }

                    if (beerShevaAlert) {
                        Log.d(TAG, "Alert for Beer Sheva found")
                        sendBeerShevaAlertNotification()

                    } else {
                        Log.d(TAG, "No alert for Beer Sheva found, no route will be shown.")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching data: ", error.toException())
            }
        })

        createNotificationChannel()

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        val notification = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setContentTitle("Service Running")
            .setContentText("Your app is running in the background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)  //Change to a suitable icon
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        Log.d("MyForegroundService", "Notification created and service started")
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "CHANNEL_ID",
                "Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendBeerShevaAlertNotification() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "CHANNEL_ID"
        val gson = Gson()

        // An intent that will open when the user clicks the alert
        val intent = Intent(this, MapFragment::class.java).apply {
            putExtra("action", "guideUserByLocation")
            putExtra(
                "event", gson.toJson(
                    ShelterEvent(
                        type = "ShelterNotification",
                        currentLocation = false
                    )
                )
            )
        }
        // PendingIntent with FLAG_UPDATE_CURRENT to make sure the intent is being updated
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("התראה לבאר שבע")
            .setContentText("התראה בבאר שבע,לחץ כאן לקבלת הנחיות.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        Log.d("MyForegroundService", "Foreground service is destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}