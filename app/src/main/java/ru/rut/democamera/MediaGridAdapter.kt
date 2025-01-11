package ru.rut.democamera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.rut.democamera.databinding.ListItemMediaBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MediaGridAdapter(
    private var files: List<File>,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<MediaGridAdapter.ViewHolder>() {

    fun updateData(newFiles: List<File>) {
        this.files = newFiles
        notifyDataSetChanged()
    }

    class ViewHolder(
        private val binding: ListItemMediaBinding,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            binding.root.setOnClickListener(this)
        }

        fun bind(file: File) {
            val isVideo = file.extension.lowercase(Locale.ROOT) == "mp4"
            val typeText = if (isVideo) "Video" else "Photo"

            val date = Date(file.lastModified())
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val dateText = dateFormat.format(date)

            binding.mediaType.text = typeText
            binding.mediaDate.text = dateText

            Glide.with(binding.root)
                .load(file)
                .centerCrop()
                .into(binding.previewImage)
        }

        override fun onClick(v: View?) {
            onItemClick(adapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemMediaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding) { pos ->
            onItemClick(files[pos])
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size
}
