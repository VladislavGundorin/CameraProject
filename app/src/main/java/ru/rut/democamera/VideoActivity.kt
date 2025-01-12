package ru.rut.democamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import ru.rut.democamera.databinding.ActivityVideoBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "VideoActivity"
    }

    private lateinit var binding: ActivityVideoBinding
    private lateinit var cameraExecutor: ExecutorService

    private var isFrontCamera = false
    private lateinit var cameraProvider: ProcessCameraProvider
    private var recording: Recording? = null
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var isRecording = false
    private var recordingStartTime = 0L
    private val handler = Handler(Looper.getMainLooper())

    @RequiresApi(Build.VERSION_CODES.M)
    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Snackbar.make(
                    binding.root,
                    "Camera permission is needed to record video",
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                requestAudioPermission()
            }
        }

    @RequiresApi(Build.VERSION_CODES.M)
    private val audioPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Snackbar.make(
                    binding.root,
                    "Audio record permission is needed to record video with sound",
                    Snackbar.LENGTH_LONG
                ).show()
                startCamera()
            } else {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        isFrontCamera = intent.getBooleanExtra("isFrontCamera", false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraPermissionResult.launch(Manifest.permission.CAMERA)
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(this))
        }

        binding.captureVideoBtn.setOnClickListener {
            if (!isRecording) {
                startRecording()
            } else {
                stopRecording()
            }
        }

        binding.switchCameraBtn.setOnClickListener {
            isFrontCamera = !isFrontCamera
            startCamera()
        }

        binding.galleryBtn.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }

        binding.photoBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("isFrontCamera", isFrontCamera)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        cameraExecutor.shutdown()
        stopTimer()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestAudioPermission() {
        audioPermissionResult.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        cameraProvider.unbindAll()

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        val camSelector = if (isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector = camSelector
        binding.videoPreview.controller = cameraController

        try {
            cameraProvider.bindToLifecycle(this, camSelector, videoCapture)
        } catch (exc: Exception) {
            Toast.makeText(this, "Use case binding failed", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun startRecording() {
        val videoFile = File(externalMediaDirs[0], "VIDEO_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        val canRecordAudio =
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED

        val recordingSource = if (canRecordAudio) {
            videoCapture.output.prepareRecording(this, outputOptions)
                .withAudioEnabled()
        } else {
            videoCapture.output.prepareRecording(this, outputOptions)
        }

        isRecording = true
        val normalColor = ContextCompat.getColorStateList(this, R.color.purple_500)
        val redColor = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
        binding.captureVideoBtn.backgroundTintList = redColor

        recording = recordingSource.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show()
                    recordingStartTime = System.currentTimeMillis()
                    startTimer()
                }
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    stopTimer()
                    binding.captureVideoBtn.backgroundTintList = normalColor
                    if (!recordEvent.hasError()) {
                        val msg = "Video captured: ${videoFile.absolutePath}"
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, msg)
                    } else {
                        recording?.close()
                        recording = null
                        Log.e(TAG, "Video capture ends with error: ${recordEvent.error}")
                    }
                    recording = null
                }
            }
        }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
        isRecording = false
        stopTimer()
        val normalColor = ContextCompat.getColorStateList(this, R.color.purple_500)
        binding.captureVideoBtn.backgroundTintList = normalColor
    }

    private fun startTimer() {
        handler.post(timerRunnable)
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val elapsed = System.currentTimeMillis() - recordingStartTime
                val seconds = elapsed / 1000
                val minutes = seconds / 60
                val displaySec = seconds % 60
                val display = String.format("%02d:%02d", minutes, displaySec)
                binding.recordingTimeText.text = display
                handler.postDelayed(this, 500)
            }
        }
    }

    private fun stopTimer() {
        handler.removeCallbacks(timerRunnable)
        binding.recordingTimeText.text = "00:00"
    }
}
