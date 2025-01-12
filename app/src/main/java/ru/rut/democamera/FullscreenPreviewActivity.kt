package ru.rut.democamera

import android.content.DialogInterface
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.MediaController
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import ru.rut.democamera.databinding.ActivityFullscreenPreviewBinding
import java.io.File
import java.util.*

class FullscreenPreviewActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "FullscreenPreviewAct"
    }

    private lateinit var binding: ActivityFullscreenPreviewBinding
    private var currentFile: File? = null
    private var isVideo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        binding = ActivityFullscreenPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filePath = intent.getStringExtra("file_path")
        if (filePath.isNullOrEmpty()) {
            finish()
            return
        }
        currentFile = File(filePath)
        if (!currentFile!!.exists()) {
            finish()
            return
        }

        isVideo = currentFile!!.extension.lowercase(Locale.ROOT) == "mp4"

        if (isVideo) {
            binding.imageView.visibility = View.GONE
            binding.videoView.visibility = View.VISIBLE
            binding.replayBtn.visibility = View.GONE

            val mediaController = MediaController(this)
            mediaController.setAnchorView(binding.videoView)
            binding.videoView.setMediaController(mediaController)

            binding.videoView.setVideoURI(Uri.fromFile(currentFile))
            binding.videoView.requestFocus()
            binding.videoView.start()
        } else {
            binding.videoView.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
            binding.replayBtn.visibility = View.GONE

            Glide.with(this)
                .load(currentFile)
                .into(binding.imageView)
        }

        binding.deleteBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete file?")
                .setMessage("Are you sure you want to delete this file?")
                .setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                    currentFile?.delete()
                    finish()
                }
                .setNegativeButton("No", null)
                .show()
        }

        binding.newContentBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.backToGalleryBtn.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        binding.videoView.stopPlayback()
    }
}
