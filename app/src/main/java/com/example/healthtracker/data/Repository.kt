package com.example.healthtracker.data

import com.example.healthtracker.models.MedicineDto
import com.example.healthtracker.models.ProgressDto
import com.example.healthtracker.network.ApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal safe Repository:
 * - api: the default ApiService (login currently implemented)
 * - apiFor(baseUrl): helper to create a Retrofit instance for a custom base URL (ngrok)
 *
 * Network-heavy methods are defensive and fall back to local/default data so the app won't crash
 * when backend endpoints are missing or return non-JSON payloads (ngrok HTML pages).
 */
@Singleton
class Repository @Inject constructor(
    val api: ApiService,
    private val db: AppDatabase
) {

    fun apiFor(baseUrl: String): ApiService {

        val moshi = Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(
                OkHttpClient.Builder()
                    .build()
            )
            .build()
            .create(ApiService::class.java)
    }




    suspend fun fetchTodayProgress(): ProgressDto {
        // Use a safe local fallback to avoid crashes.
        // When backend adds a matching endpoint, replace this logic with a network call using api or apiFor().
        return try {
            // If you later add an endpoint in ApiService named getTodayProgress, call it here.
            ProgressDto() // default empty progress
        } catch (e: Exception) {
            ProgressDto()
        }
    }

    suspend fun getLocalMedicines(): List<MedicineEntry> = try {
        db.medicineDao().getAll()
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * Defensive refresh - will do nothing if API endpoint is not present.
     * Uncomment / update when backend returns medicines endpoint.
     */
    suspend fun refreshMedicinesFromServer() {
        try {
            // If your backend provides /medicines and ApiService.getMedicines(), use it here.
            // val resp = api.getMedicines()
            // if (resp.isSuccessful) { ... }
        } catch (_: Exception) {
            // ignore - offline / endpoint not available
        }
    }

    suspend fun markMedicine(id: String, taken: Boolean) {
        try {
            db.medicineDao().markTaken(id, taken)
        } catch (_: Exception) { /* ignore db error */ }

        // best-effort notify server if method exists
        try {
            // api.markMedicine(id, mapOf("taken" to taken))
        } catch (_: Exception) { /* ignore */ }
    }

    suspend fun saveProgressSnapshot(percent: Int) {
        try {
            db.progressDao().insert(ProgressSnapshot(timestamp = System.currentTimeMillis(), percent = percent))
        } catch (_: Exception) { /* ignore */ }
    }
}
