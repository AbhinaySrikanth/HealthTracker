package com.example.healthtracker.data

import java.text.SimpleDateFormat
import java.util.*

class Repository(private val db: AppDatabase) {

    private val heartDao = db.heartRateDao()
    private val stepDao = db.stepDao()
    private val unlockDao = db.unlockDao()
    private val screenDao = db.screenTimeDao()

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // ---------------- HEART RATE ----------------
    suspend fun saveHeartRate(bpm: Int) {
        heartDao.insert(HeartRateEntry(bpm = bpm))
    }

    suspend fun getRecentHeartRates() = heartDao.getRecent()

    // ---------------- STEPS ----------------
    suspend fun saveFitSteps(steps: Int) {
        stepDao.insertFitSteps(StepEntry(steps = steps))
    }

    suspend fun savePhoneSteps(steps: Int) {
        stepDao.insertPhoneSteps(PhoneStepEntry(steps = steps))
    }

    // ---------------- UNLOCK COUNTS ----------------
    suspend fun incrementUnlocksForToday() {
        val d = today()
        val entry = unlockDao.getForDate(d)

        if (entry == null) {
            unlockDao.insert(UnlockEntry(date = d, unlocks = 1))
        } else {
            unlockDao.increment(d)
        }
    }

    suspend fun getTodayUnlockCount(): Int {
        return unlockDao.getForDate(today())?.unlocks ?: 0
    }

    // ---------------- SCREEN TIME ----------------
    suspend fun addScreenTime(seconds: Long) {
        val d = today()
        val entry = screenDao.getForDate(d)

        if (entry == null) {
            screenDao.insert(ScreenTimeEntry(date = d, seconds = seconds))
        } else {
            screenDao.updateTime(d, seconds)
        }
    }

    suspend fun getTodayScreenTime(): Long {
        return screenDao.getForDate(today())?.seconds ?: 0
    }
}
