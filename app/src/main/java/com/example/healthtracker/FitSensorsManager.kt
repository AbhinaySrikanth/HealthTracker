package com.example.healthtracker

import android.app.Activity
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import java.util.concurrent.TimeUnit

class FitSensorsManager(private val activity: Activity) {

    private var heartRateListener: OnDataPointListener? = null
    private var stepListener: OnDataPointListener? = null

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)
        .build()

    private val account
        get() = GoogleSignIn.getAccountForExtension(activity, fitnessOptions)

    fun isSignedInWithFitness(): Boolean =
        GoogleSignIn.hasPermissions(account, fitnessOptions)

    // Start Streaming Sensors + Daily Total + HR History
    fun startSensors(
        onHeartRate: (Double?) -> Unit,
        onStepsDelta: (Int) -> Unit,
        onDailySteps: (Int) -> Unit,
        onLastHistoryHR: (Int?) -> Unit
    ) {
        registerHeartRateListener(onHeartRate)
        registerStepDeltaListener(onStepsDelta)
        readDailyTotalSteps(onDailySteps)
        readLatestHeartRateHistory(onLastHistoryHR)
        subscribeToRecordingAPI()
    }

    // -------------------------------------------------------------
    // LIVE HEART RATE STREAM (Phone + WearOS ONLY, Fastrack = 0)
    // -------------------------------------------------------------
    private fun registerHeartRateListener(onHeartRate: (Double?) -> Unit) {
        val listener = OnDataPointListener { dp ->
            try {
                val bpm = dp.getValue(dp.dataType.fields[0]).asFloat()
                onHeartRate(bpm.toDouble())
            } catch (e: Exception) {
                Log.e("FitSensors", "HR parse error ${e.message}")
            }
        }
        heartRateListener = listener

        val request = SensorRequest.Builder()
            .setDataType(DataType.TYPE_HEART_RATE_BPM)
            .setSamplingRate(3, TimeUnit.SECONDS)
            .build()

        Fitness.getSensorsClient(activity, account)
            .add(request, listener)
            .addOnSuccessListener { Log.i("FitSensors", "HR listener OK") }
            .addOnFailureListener { Log.e("FitSensors", "HR listener fail ${it.message}") }
    }

    // -------------------------------------------------------------
    // LIVE STEP DELTA STREAM
    // -------------------------------------------------------------
    private fun registerStepDeltaListener(onSteps: (Int) -> Unit) {
        val listener = OnDataPointListener { dp ->
            try {
                val delta = dp.getValue(dp.dataType.fields[0]).asInt()
                onSteps(delta)
            } catch (_: Exception) {}
        }

        stepListener = listener

        val request = SensorRequest.Builder()
            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .setSamplingRate(5, TimeUnit.SECONDS)
            .build()

        Fitness.getSensorsClient(activity, account)
            .add(request, listener)
            .addOnSuccessListener { Log.i("FitSensors", "Step delta listener OK") }
            .addOnFailureListener { Log.e("FitSensors", "Step delta listener fail ${it.message}") }
    }

    // -------------------------------------------------------------
    // DAILY TOTAL STEPS
    // -------------------------------------------------------------
    fun readDailyTotalSteps(onDaily: (Int) -> Unit) {
        Fitness.getHistoryClient(activity, account)
            .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener { result ->
                val total =
                    if (result.dataPoints.isNotEmpty())
                        result.dataPoints[0].getValue(Field.FIELD_STEPS).asInt()
                    else 0

                onDaily(total)
                Log.i("FitSensors", "Daily steps = $total")
            }
            .addOnFailureListener { e ->
                Log.e("FitSensors", "Daily total fail ${e.message}")
            }
    }

    // -------------------------------------------------------------
    // **THE IMPORTANT PART**
    // LATEST HEART RATE HISTORY (Works with Fastrack, Fitbit, etc.)
    // -------------------------------------------------------------
    fun readLatestHeartRateHistory(onHistoryHR: (Int?) -> Unit) {
        val end = System.currentTimeMillis()
        val start = end - (24 * 60 * 60 * 1000L)

        val req = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .setTimeRange(start, end, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(activity, account)
            .readData(req)
            .addOnSuccessListener { res ->
                var last: Int? = null

                res.dataSets.forEach { set ->
                    set.dataPoints.forEach { dp ->
                        val bpm = dp.getValue(dp.dataType.fields[0]).asFloat().toInt()
                        last = bpm
                    }
                }

                onHistoryHR(last)
                Log.i("FitSensors", "History HR = $last")
            }
            .addOnFailureListener {
                Log.e("FitSensors", "HR history read fail ${it.message}")
                onHistoryHR(null)
            }
    }

    // -------------------------------------------------------------
    // BACKGROUND RECORDING
    // -------------------------------------------------------------
    private fun subscribeToRecordingAPI() {
        Fitness.getRecordingClient(activity, account)
            .subscribe(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener {
                Log.i("FitSensors", "Recording subscribed")
            }
            .addOnFailureListener {
                Log.e("FitSensors", "Recording subscribe fail ${it.message}")
            }
    }

    fun stopSensors() {
        heartRateListener?.let {
            Fitness.getSensorsClient(activity, account).remove(it)
        }
        stepListener?.let {
            Fitness.getSensorsClient(activity, account).remove(it)
        }
    }
}
