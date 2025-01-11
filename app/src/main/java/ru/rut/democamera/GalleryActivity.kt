package ru.rut.democamera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import ru.rut.democamera.databinding.ActivityGalleryBinding
import java.io.File
import java.util.*

class GalleryActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "GalleryActivity"
    }

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: MediaGridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = MediaGridAdapter(emptyList()) { file ->
            val intent = Intent(this, FullscreenPreviewActivity::class.java)
            intent.putExtra("file_path", file.absolutePath)
            startActivity(intent)
        }
        binding.recyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        loadMediaFiles()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    private fun loadMediaFiles() {
        val directory = File(externalMediaDirs[0].absolutePath)
        val files = directory.listFiles()?.filter {
            it.isFile && (it.extension.lowercase(Locale.ROOT) == "jpg" ||
                    it.extension.lowercase(Locale.ROOT) == "mp4")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
        adapter.updateData(files)
    }
}
