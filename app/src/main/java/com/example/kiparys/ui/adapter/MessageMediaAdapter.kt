package com.example.kiparys.ui.adapter

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.util.TypedValueCompat.dpToPx
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.ImageRequest
import com.example.kiparys.KiparysApplication
import com.example.kiparys.data.model.MediaMetadata
import com.example.kiparys.databinding.ItemMediaBinding
import com.google.android.material.color.MaterialColors


class MessageMediaAdapter(
    private val onMediaClickListener: (MediaMetadata) -> Unit,
    private val isMultipleMedia: Boolean,
    private val isReceivedMessage: Boolean
) : ListAdapter<MediaMetadata, MessageMediaAdapter.MediaViewHolder>(differCallback) {

    companion object {
        private val differCallback = object : DiffUtil.ItemCallback<MediaMetadata>() {
            override fun areItemsTheSame(oldItem: MediaMetadata, newItem: MediaMetadata): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: MediaMetadata,
                newItem: MediaMetadata
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemMediaBinding.inflate(inflater, parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MediaViewHolder(private val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val imageLoader =
            (binding.root.context.applicationContext as KiparysApplication).imageLoader
        private val metrics = binding.root.context.resources.displayMetrics

        fun bind(media: MediaMetadata) = with(binding) {
            if (isMultipleMedia) {
                val params = binding.sivMedia.layoutParams
                val dimensions = if (isReceivedMessage) 111.85f else 136.85f
                params.width = dpToPx(dimensions, metrics).toInt()
                params.height = dpToPx(dimensions, metrics).toInt()
                binding.sivMedia.layoutParams = params
            } else {
                val params = binding.sivMedia.layoutParams
                val dimensions = if (isReceivedMessage) 168f else 208f
                params.width = 0
                params.height = dpToPx(dimensions, metrics).toInt()
                binding.sivMedia.layoutParams = params
            }

            val aspectRatio = media.aspectRatio ?: 1.0f
            val constraintSet = ConstraintSet()
            constraintSet.clone(binding.root)
            constraintSet.setDimensionRatio(binding.sivMedia.id, "$aspectRatio")
            constraintSet.applyTo(binding.root)

            cpiMediaLoad.visibility = if (media.loading == true) View.VISIBLE else View.GONE

            val request = ImageRequest.Builder(binding.root.context)
                .data(media.mediaUrl ?: media.tempMediaUrl)
                .placeholder(
                    ColorDrawable(
                        MaterialColors.getColor(
                            root,
                            com.google.android.material.R.attr.colorSurfaceContainerLow
                        )
                    )
                )
                .error(
                    ColorDrawable(
                        MaterialColors.getColor(
                            root,
                            com.google.android.material.R.attr.colorSurfaceContainerLow
                        )
                    )
                )
                .target(binding.sivMedia)
                .build()
            imageLoader.enqueue(request)

            binding.root.setOnClickListener {
                onMediaClickListener(media)
            }
        }
    }
}
