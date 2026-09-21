package com.mrmoeini.androidhealthcheck

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.RandomAccessFile
import java.text.DecimalFormat

/**
 * Shows a live view of:
 *  - System-wide CPU usage (from /proc/stat, when the platform allows reading it)
 *  - This app's own CPU usage (via Process.getElapsedCpuTime(), always available)
 *  - Device and app memory usage (via ActivityManager + Runtime)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var cpuText: TextView
    private lateinit var appCpuText: TextView
    private lateinit var memText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val refreshIntervalMs = 2000L

    private var lastCpuIdle = 0L
    private var lastCpuTotal = 0L
    private var lastAppCpuTimeMs = 0L
    private var lastAppSampleTime = 0L

    private val df = DecimalFormat("#0.0")

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateSystemCpu()
            updateAppCpu()
            updateMemory()
            handler.postDelayed(this, refreshIntervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cpuText = findViewById(R.id.cpuText)
        appCpuText = findViewById(R.id.appCpuText)
        memText = findViewById(R.id.memText)

        lastAppCpuTimeMs = Process.getElapsedCpuTime()
        lastAppSampleTime = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    /**
     * Note: many Android 8+ devices block third-party apps from reading /proc/stat
     * for security reasons (SELinux). On those devices this will show "unavailable"
     * and the App CPU / Memory readings below are the reliable numbers to use.
     */
    private fun updateSystemCpu() {
        try {
            RandomAccessFile("/proc/stat", "r").use { reader ->
                val load = reader.readLine()
                val toks = load.trim().split("\\s+".toRegex())

                val user = toks[1].toLong()
                val nice = toks[2].toLong()
                val system = toks[3].toLong()
                val idle = toks[4].toLong()
                val iowait = toks.getOrElse(5) { "0" }.toLong()
                val irq = toks.getOrElse(6) { "0" }.toLong()
                val softirq = toks.getOrElse(7) { "0" }.toLong()

                val total = user + nice + system + idle + iowait + irq + softirq

                if (lastCpuTotal != 0L) {
                    val totalDelta = total - lastCpuTotal
                    val idleDelta = idle - lastCpuIdle
                    val usage = if (totalDelta > 0) {
                        100.0 * (totalDelta - idleDelta) / totalDelta
                    } else 0.0
                    cpuText.text = "System CPU: ${df.format(usage)}%"
                } else {
                    cpuText.text = "System CPU: measuring..."
                }

                lastCpuTotal = total
                lastCpuIdle = idle
            }
        } catch (e: Exception) {
            cpuText.text = "System CPU: unavailable on this device"
        }
    }

    private fun updateAppCpu() {
        try {
            val now = System.currentTimeMillis()
            val cpuTimeNow = Process.getElapsedCpuTime()
            val wallDelta = now - lastAppSampleTime
            val cpuDelta = cpuTimeNow - lastAppCpuTimeMs

            if (wallDelta > 0) {
                val usage = (100.0 * cpuDelta / wallDelta).coerceIn(0.0, 100.0)
                appCpuText.text = "App CPU: ${df.format(usage)}%"
            }

            lastAppSampleTime = now
            lastAppCpuTimeMs = cpuTimeNow
        } catch (e: Exception) {
            appCpuText.text = "App CPU: unavailable"
        }
    }

    private fun updateMemory() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)

            val totalMb = memInfo.totalMem / (1024 * 1024)
            val availMb = memInfo.availMem / (1024 * 1024)
            val usedMb = totalMb - availMb

            val runtime = Runtime.getRuntime()
            val appHeapUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

            memText.text = "Device Memory: ${usedMb}MB / ${totalMb}MB\nApp Heap: ${appHeapUsedMb}MB"
        } catch (e: Exception) {
            memText.text = "Memory: unavailable"
        }
    }
}
