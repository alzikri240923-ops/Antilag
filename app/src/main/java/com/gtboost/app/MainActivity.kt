package com.gtboost.app

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import rikka.shizuku.Shizuku
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private var shizukuBinding = false
    private var pendingHz: Float? = null
    private var pendingRelease = false

private var refreshService: IRefreshRateService? = null
private var shizukuTerhubung = false

private val requestCodeShizuku = 1001

private val shizukuPermissionListener =
    Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == requestCodeShizuku) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                hubungkanShizuku()
            } else {
                Toast.makeText(
                    this,
                    "Izin Shizuku ditolak",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            refreshService = IRefreshRateService.Stub.asInterface(binder)
            shizukuTerhubung = true
            Toast.makeText(
                this@MainActivity,
                "Shizuku terhubung",
                Toast.LENGTH_SHORT
            ).show()
            terapkanPermintaanTertunda()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            refreshService = null
            shizukuTerhubung = false
            shizukuBinding = false
        }
    }

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(this, RefreshRateService::class.java)
        )
            .daemon(false)
            .tag("GTBoostRefreshRate")
            .version(1)
            .processNameSuffix("refresh")
    }

    private fun hubungkanShizuku() {
        if (shizukuBinding) return
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizuku belum aktif", Toast.LENGTH_LONG).show()
            return
        }

        try {
            shizukuBinding = true
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (e: Exception) {
            shizukuBinding = false
            Toast.makeText(
                this,
                "Gagal menghubungkan Shizuku: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun mintaIzinShizuku() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Aktifkan Shizuku terlebih dahulu", Toast.LENGTH_LONG).show()
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            hubungkanShizuku()
        } else {
            Shizuku.requestPermission(requestCodeShizuku)
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private val updater = object : Runnable {
        override fun run() {
            perbarui()
            handler.postDelayed(this, 2000)
        }
    }

    private fun klik(id: Int, aksi: () -> Unit) {
        findViewById<Button>(id).setOnClickListener { aksi() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        klik(R.id.btnLayar) { bukaPengaturan(Settings.ACTION_DISPLAY_SETTINGS) }
        klik(R.id.btnBaterai) { bukaPengaturan(Settings.ACTION_BATTERY_SAVER_SETTINGS) }
        klik(R.id.btnDev) { bukaPengaturan(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) }

        klik(R.id.btn60) { kunciHz(60f) }
        klik(R.id.btn90) { kunciHz(90f) }
        klik(R.id.btn120) { kunciHz(120f) }
        klik(R.id.btn144) { kunciHz(144f) }
        klik(R.id.btnLepas) { lepasHz() }

        klik(R.id.btnBersih) { bersihkan() }
        klik(R.id.btnMulai) { mulaiPantau() }
        klik(R.id.btnStop) {
            stopService(Intent(this, TempService::class.java))
            Toast.makeText(this, "Pemantau suhu dihentikan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        if (shizukuBinding) {
            try {
                Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            } catch (_: Exception) {
            }
            shizukuBinding = false
        }
        super.onDestroy()
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

    private fun kunciHz(hz: Float) {
        pendingHz = hz
        pendingRelease = false

        if (refreshService != null) {
            terapkanPermintaanTertunda()
        } else {
            mintaIzinShizuku()
        }
    }

    private fun lepasHz() {
        pendingHz = null
        pendingRelease = true

        if (refreshService != null) {
            terapkanPermintaanTertunda()
        } else {
            mintaIzinShizuku()
        }
    }

    private fun terapkanPermintaanTertunda() {
        val service = refreshService ?: return

        try {
            val hasil = if (pendingRelease) {
                service.releaseRefreshRate()
            } else {
                val hz = pendingHz ?: return
                service.setRefreshRate(hz)
            }

            Toast.makeText(this, hasil, Toast.LENGTH_LONG).show()
            pendingHz = null
            pendingRelease = false
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Perintah Shizuku gagal: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun ramBebasMb(): Long {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.availMem / (1024 * 1024)
    }

    private fun bersihkan() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val awal = ramBebasMb()
        for (app in packageManager.getInstalledApplications(0)) {
            val sistem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!sistem && app.packageName != packageName) {
                am.killBackgroundProcesses(app.packageName)
            }
        }
        handler.postDelayed({
            val selisih = ramBebasMb() - awal
            Toast.makeText(this, "Selesai. RAM bertambah sekitar $selisih MB", Toast.LENGTH_LONG).show()
        }, 1500)
    }

    private fun mulaiPantau() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            Toast.makeText(this, "Izinkan notifikasi, lalu tekan Mulai lagi", Toast.LENGTH_LONG).show()
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, TempService::class.java))
        Toast.makeText(this, "Pemantau suhu aktif", Toast.LENGTH_SHORT).show()
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
