package com.example.healthtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.healthtracker.data.AppDatabase
import com.example.healthtracker.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScreenReceiver : BroadcastReceiver() {

    companion object {
        private const val PREFS = "screen_time_prefs"
        private const val KEY_LAST_ON = "last_on_ts"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val repo = Repository(AppDatabase.getDatabase(context))

        when (intent?.action) {
            Intent.ACTION_SCREEN_ON -> {
                prefs.edit().putLong(KEY_LAST_ON, System.currentTimeMillis()).apply()
            }
            Intent.ACTION_SCREEN_OFF -> {
                val last = prefs.getLong(KEY_LAST_ON, 0L)
                if (last > 0L) {
                    val session = System.currentTimeMillis() - last
                    CoroutineScope(Dispatchers.IO).launch {
                        repo.addScreenTimeMillisForToday(session)
                    }
                    prefs.edit().remove(KEY_LAST_ON).apply()
                }
            }
        }
    }
}
