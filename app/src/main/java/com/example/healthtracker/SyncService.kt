package com.example.healthtracker

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

object SyncService {
    private val client = OkHttpClient()

    suspend fun pushSample(context: Context, payloadJson: String, endpointUrl: String) {
        withContext(Dispatchers.IO) {
            val body: RequestBody = payloadJson.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request: Request = Request.Builder()
                .url(endpointUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    // log or handle error
                }
            }
        }
    }
}
