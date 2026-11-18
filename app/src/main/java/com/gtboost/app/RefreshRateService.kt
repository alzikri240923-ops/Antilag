package com.gtboost.app

import android.os.RemoteException

class RefreshRateService : IRefreshRateService.Stub() {

    private fun jalankan(perintah: String): String {
        return try {
            val proses = ProcessBuilder("sh", "-c", perintah)
                .redirectErrorStream(true)
                .start()

            val hasil = proses.inputStream.bufferedReader().use { it.readText() }
            val kode = proses.waitFor()

            if (kode == 0) {
                "Berhasil: $hasil"
            } else {
                "Gagal (kode $kode): $hasil"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun setRefreshRate(hz: Float): String {
        if (hz !in listOf(60f, 90f, 120f, 144f)) {
            return "Refresh rate tidak valid"
        }

        return jalankan(
            "settings put system min_refresh_rate $hz && " +
            "settings put system peak_refresh_rate $hz"
        )
    }

    override fun releaseRefreshRate(): String {
        return jalankan(
            "settings delete system min_refresh_rate && " +
            "settings delete system peak_refresh_rate"
        )
    }

    override fun destroy() {
        System.exit(0)
    }
}
