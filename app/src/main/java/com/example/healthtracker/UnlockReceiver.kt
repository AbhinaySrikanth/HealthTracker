package com.example.healthtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.healthtracker.data.AppDatabase
import com.example.healthtracker.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_USER_PRESENT) {

            // Increment unlock count in DB
            CoroutineScope(Dispatchers.IO).launch {
                val repo = Repository(AppDatabase.getDatabase(context))
                repo.incrementUnlocksForToday()
            }
        }
    }
}
