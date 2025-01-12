package ru.rut.democamera

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.material.snackbar.Snackbar
import ru.rut.democamera.databinding.ActivityMainBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProviderFuture: ProcessCameraProvider

    private var isFrontCamera = false

    private var imageCapture: ImageCapture? = null
    private lateinit var imageCaptureExecutor: ExecutorService

    @RequiresApi(Build.VERSION_CODES.M)
    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                setupCamera()
            } else {
                Snackbar.make(
                    binding.root,
                    "The camera permission is necessary",
                    Snackbar.LENGTH_INDEFINITE
                ).show()
            }
        }

    private fun setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this).get()
        startCamera()
    }

    private fun startCamera() {
        val cameraProvider = cameraProviderFuture
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.preview.surfaceProvider)
        }

        val cameraSelector = if (isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            cameraProvider.unbindAll()
            imageCapture = ImageCapture.Builder().build()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch (e: Exception) {
            Log.d(TAG, "Use case binding failed: $e")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isFrontCamera = intent.getBooleanExtra("isFrontCamera", false)
        imageCaptureExecutor = Executors.newSingleThreadExecutor()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraPermissionResult.launch(android.Manifest.permission.CAMERA)
        } else {
            setupCamera()
        }

        binding.imgCaptureBtn.setOnClickListener {
            takePhoto()
            animateFlash()
        }

        binding.switchBtn.setOnClickListener {
            isFrontCamera = !isFrontCamera
            startCamera()
        }

        binding.galleryBtn.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }

        binding.videoBtn.setOnClickListener {
            val intent = Intent(this, VideoActivity::class.java)
            intent.putExtra("isFrontCamera", isFrontCamera)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        imageCaptureExecutor.shutdown()
    }

    private fun takePhoto() {
        if (imageCapture == null) return

        val fileName = "JPEG_${System.currentTimeMillis()}.jpg"
        val file = File(externalMediaDirs[0], fileName)
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture?.takePicture(
            outputFileOptions,
            imageCaptureExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.i(TAG, "The image has been saved in ${file.toUri()}")
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        binding.root.context,
                        "Error taking photo",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.d(TAG, "Error taking photo: $exception")
                }
            }
        )
    }

    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }
}
