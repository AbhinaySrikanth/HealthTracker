package com.example.healthtracker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.healthtracker.data.AppDatabase
import com.example.healthtracker.data.PhoneUsage
import com.example.healthtracker.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DailyResetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repo = Repository(AppDatabase.getDatabase(applicationContext))
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val today = fmt.format(Date())
            val existing = repo.getPhoneUsageDao().getForDay(today)
            if (existing == null) {
                repo.upsertPhoneUsage(PhoneUsage(day = today))
            }
            Result.success()
        } catch (t: Throwable) {
            Result.failure()
        }
    }
}
