package com.example.healthtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.healthtracker.data.AppDatabase
import com.example.healthtracker.data.Repository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var statusTv: TextView
    private lateinit var hrTv: TextView
    private lateinit var stepsTv: TextView
    private lateinit var phoneStepsTv: TextView
    private lateinit var camHrTv: TextView
    private lateinit var dailyStepsTv: TextView
    private lateinit var historyHrTv: TextView
    private lateinit var unlocksTv: TextView
    private lateinit var screenTimeTv: TextView
    private lateinit var loginSection: LinearLayout

    private lateinit var repository: Repository
    private val scope = CoroutineScope(Dispatchers.Main)

    private var cameraHr: CameraHeartRate? = null
    private lateinit var phoneStepManager: PhoneStepManager
    private lateinit var fitManager: FitSensorsManager

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    private val cameraPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI refs
        statusTv = findViewById(R.id.statusTv)
        hrTv = findViewById(R.id.hrTv)
        stepsTv = findViewById(R.id.stepsTv)
        phoneStepsTv = findViewById(R.id.phoneStepsTv)
        camHrTv = findViewById(R.id.camHrTv)
        dailyStepsTv = findViewById(R.id.dailyStepsTv)
        historyHrTv = findViewById(R.id.historyHrTv)
        unlocksTv = findViewById(R.id.unlocksTv)
        screenTimeTv = findViewById(R.id.screenTimeTv)
        loginSection = findViewById(R.id.loginSection)

        repository = Repository(AppDatabase.getDatabase(this))

        // Buttons
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val btnStartFit = findViewById<Button>(R.id.btnStart)
        val btnCameraHr = findViewById<Button>(R.id.btnStartCameraHr)
        val btnStopCam = findViewById<Button>(R.id.btnStopCameraHr)
        val btnStartPhoneSteps = findViewById<Button>(R.id.btnPhoneSteps)

        fitManager = FitSensorsManager(this)
        phoneStepManager = PhoneStepManager(this)

        btnSignIn.setOnClickListener { startGoogleLogin() }
        btnStartFit.setOnClickListener { startGoogleFitTracking() }
        btnStartPhoneSteps.setOnClickListener { startPhoneStepTracking() }

        btnCameraHr.setOnClickListener {
            ensureCameraPermission()
            startCameraHR()
        }

        btnStopCam.setOnClickListener { cameraHr?.stop() }

        ensureSensorsPermissions()
        updateUiSignedInState()
        refreshUsageUi()
    }

    private fun refreshUsageUi() {
        scope.launch {
            val unlocks = repository.getTodayUnlockCount()
            val screen = repository.getTodayScreenTime()
            unlocksTv.text = "Unlocks: $unlocks"
            screenTimeTv.text = "Screen time: ${screen}s"
        }
    }

    // ---------------- LOGIN ----------------
    private fun startGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.web_client_id))
            .requestScopes(
                Fitness.SCOPE_ACTIVITY_READ,
                Fitness.SCOPE_BODY_READ
            )
            .build()

        startActivityForResult(
            GoogleSignIn.getClient(this, gso).signInIntent,
            REQ_GOOGLE_LOGIN
        )
    }

    override fun onActivityResult(req: Int, result: Int, data: Intent?) {
        super.onActivityResult(req, result, data)

        if (req == REQ_GOOGLE_LOGIN) {
            try {
                val acc = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException::class.java)
                requestGoogleFitPermissions(acc)
            } catch (_: Exception) {
                statusTv.text = "Google Login Failed"
            }
        }

        if (req == REQ_FIT_PERMISSIONS) updateUiSignedInState()
    }

    private fun requestGoogleFitPermissions(acc: GoogleSignInAccount) {
        val opt = FitnessOptions.builder()
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .build()

        if (!GoogleSignIn.hasPermissions(acc, opt)) {
            GoogleSignIn.requestPermissions(this, REQ_FIT_PERMISSIONS, acc, opt)
        } else updateUiSignedInState()
    }

    private fun updateUiSignedInState() {
        val opt = FitnessOptions.builder()
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .build()

        val acc = GoogleSignIn.getAccountForExtension(this, opt)
        val signed = GoogleSignIn.hasPermissions(acc, opt)

        statusTv.text = if (signed) "Signed into Google Fit" else "Not signed into Google Fit"
        loginSection.visibility = if (signed) View.GONE else View.VISIBLE
    }

    // ---------------- FIT TRACKING ----------------
    private fun startGoogleFitTracking() {
        if (!fitManager.isSignedInWithFitness()) {
            statusTv.text = "Not signed into Google Fit"
            return
        }

        fitManager.startSensors(
            onHeartRate = { bpm ->
                scope.launch {
                    hrTv.text = "Live HR (Fit): ${bpm ?: "--"}"
                    bpm?.let { repository.saveHeartRate(it.toInt()) }
                }
            },
            onStepsDelta = { delta ->
                scope.launch {
                    stepsTv.text = "Steps (Δ): $delta"
                    repository.saveFitSteps(delta)
                }
            },
            onDailySteps = { total ->
                scope.launch {
                    dailyStepsTv.text = "Daily Fit Steps: $total"
                }
            },
            onLastHistoryHR = { last ->
                scope.launch {
                    historyHrTv.text = "Stored HR (Fit): ${last ?: "--"}"
                }
            }
        )
    }

    // ---------------- PHONE STEPS ----------------
    private fun startPhoneStepTracking() {
        phoneStepManager.startListening { steps ->
            runOnUiThread { phoneStepsTv.text = "Phone Steps: $steps" }
            scope.launch { repository.savePhoneSteps(steps) }
        }
    }

    // ---------------- CAMERA HR ----------------
    private fun ensureCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraHR() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            camHrTv.text = "Camera permission denied"
            return
        }

        if (cameraHr == null) cameraHr = CameraHeartRate(this)

        camHrTv.text = "Measuring..."

        cameraHr!!.start { bpm ->
            runOnUiThread { camHrTv.text = "Camera HR: $bpm" }
            scope.launch { repository.saveHeartRate(bpm) }
        }
    }

    // ---------------- PERMISSIONS ----------------
    private fun ensureSensorsPermissions() {
        val req = mutableListOf<String>()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
            != PackageManager.PERMISSION_GRANTED
        ) req.add(Manifest.permission.BODY_SENSORS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED
        ) req.add(Manifest.permission.ACTIVITY_RECOGNITION)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.PACKAGE_USAGE_STATS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // can't request in code; user must enable manually
        }

        if (req.isNotEmpty()) permLauncher.launch(req.toTypedArray())
    }

    override fun onDestroy() {
        super.onDestroy()
        fitManager.stopSensors()
        phoneStepManager.stop()
        cameraHr?.stop()
    }

    companion object {
        private const val REQ_GOOGLE_LOGIN = 2001
        private const val REQ_FIT_PERMISSIONS = 2002
    }
}
