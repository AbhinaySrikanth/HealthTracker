package com.example.healthtracker

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.concurrent.TimeUnit

object UsageStatsHelper {

    // ----------------------------------------------------------------------
    // TOTAL SCREEN TIME TODAY
    // ----------------------------------------------------------------------
    fun getTotalForegroundTimeToday(context: Context): Long {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val end = System.currentTimeMillis()
        val start = getStartOfDay()

        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            start,
            end
        ) ?: return 0L

        var total = 0L
        stats.forEach { total += it.totalTimeInForeground }

        return total
    }

    // ----------------------------------------------------------------------
    // UNLOCK COUNT (from UnlockReceiver)
    // ----------------------------------------------------------------------
    fun getUnlockCount(receiver: UnlockReceiver): Int {
        return receiver.dailyUnlockCount
    }

    // ----------------------------------------------------------------------
    // FORMAT TIME
    // ----------------------------------------------------------------------
    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "0m"

        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
        val hours = minutes / 60
        val mins = minutes % 60

        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    // ----------------------------------------------------------------------
    // START OF DAY TIMESTAMP
    // ----------------------------------------------------------------------
    private fun getStartOfDay(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
