package com.gtboost.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private val updater = object : Runnable {
        override fun run() {
            perbarui()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnLayar).setOnClickListener {
            bukaPengaturan(Settings.ACTION_DISPLAY_SETTINGS)
        }
        findViewById<Button>(R.id.btnBaterai).setOnClickListener {
            bukaPengaturan(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        }
        findViewById<Button>(R.id.btnDev).setOnClickListener {
            bukaPengaturan(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updater)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updater)
    }

    private fun bukaPengaturan(aksi: String) {
        try {
            startActivity(Intent(aksi))
        } catch (e: Exception) {
            Toast.makeText(this, "Pengaturan tidak bisa dibuka di HP ini", Toast.LENGTH_SHORT).show()
        }
    }

    private fun perbarui() {
        val baterai = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val suhuRaw = baterai?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val suhu = suhuRaw / 10.0
        val level = baterai?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val skala = baterai?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val persen = if (skala > 0) level * 100 / skala else 0

        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val totalMb = info.totalMem / (1024 * 1024)
        val bebasMb = info.availMem / (1024 * 1024)

        val hz = display?.refreshRate ?: 0f

        findViewById<TextView>(R.id.tvSuhu).text = "Suhu baterai: $suhu °C"
        findViewById<TextView>(R.id.tvBaterai).text = "Baterai: $persen%"
        findViewById<TextView>(R.id.tvRam).text = "RAM bebas: $bebasMb MB dari $totalMb MB"
        findViewById<TextView>(R.id.tvLayar).text = "Layar saat ini: ${hz.toInt()} Hz"

        val peringatan = when {
            suhu >= 45 -> "PANAS BANGET! Istirahatkan HP, turunkan grafis."
            suhu >= 40 -> "Mulai panas. Pertimbangkan turunkan grafis atau lepas casing."
            else -> ""
        }
        findViewById<TextView>(R.id.tvPeringatan).text = peringatan
    }
}
