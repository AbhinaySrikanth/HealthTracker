package com.example.healthtracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class PhoneStepManager(private val context: Context) : SensorEventListener {

    private var sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var onStep: ((Int) -> Unit)? = null
    private var stepCount = 0

    private var stepDetector: Sensor? = null
    private var accelerometer: Sensor? = null

    fun startListening(callback: (Int) -> Unit) {
        this.onStep = callback

        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        when {
            stepDetector != null ->
                sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)

            accelerometer != null ->
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {

            Sensor.TYPE_STEP_DETECTOR -> {
                stepCount++
                onStep?.invoke(stepCount)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val accel = sqrt(
                    (event.values[0] * event.values[0] +
                            event.values[1] * event.values[1] +
                            event.values[2] * event.values[2]).toDouble()
                )

                if (accel > 12) { // crude threshold
                    stepCount++
                    onStep?.invoke(stepCount)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
