package com.gtboost.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class TempService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var peringatanTerakhir = 0L

    private val loop = object : Runnable {
        override fun run() {
            cek()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("pantau", "Pantau suhu", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel("panas", "Peringatan panas", NotificationManager.IMPORTANCE_HIGH)
        )
        val n = buatNotif("Memantau suhu...")
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
        handler.post(loop)
    }

    override fun onDestroy() {
        handler.removeCallbacks(loop)
        super.onDestroy()
    }

    private fun buatNotif(teks: String): Notification =
        NotificationCompat.Builder(this, "pantau")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("GTBoost")
            .setContentText(teks)
            .setOngoing(true)
            .build()

    private fun cek() {
        val b = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val suhu = (b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
        val nm = getSystemService(NotificationManager::class.java)

        nm.notify(1, buatNotif("Suhu baterai: $suhu °C"))

        val sekarang = System.currentTimeMillis()
        if (suhu >= 42 && sekarang - peringatanTerakhir > 120000) {
            peringatanTerakhir = sekarang
            val n = NotificationCompat.Builder(this, "panas")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("HP mulai panas")
                .setContentText("Suhu $suhu °C. Turunkan grafis atau istirahatkan HP.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            nm.notify(2, n)
        }
    }
}
