package com.example.kiparys.ui.adapter

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.RoundedCornersTransformation
import com.example.kiparys.KiparysApplication
import com.example.kiparys.data.model.ProjectIdea
import com.example.kiparys.databinding.ItemIdeaBinding
import com.example.kiparys.util.SystemUtil.triggerSingleVibration
import com.google.android.material.color.MaterialColors
import java.util.Locale


class ProjectIdeasAdapter(
    private val currentUserId: String,
    private val onIdeaLongClickListener: (ProjectIdea) -> Unit,
    private val onThumbUpClickListener: (ProjectIdea) -> Unit
) : ListAdapter<ProjectIdea, ProjectIdeasAdapter.IdeaViewHolder>(differCallback) {

    companion object {
        private val differCallback = object : DiffUtil.ItemCallback<ProjectIdea>() {
            override fun areItemsTheSame(oldItem: ProjectIdea, newItem: ProjectIdea): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ProjectIdea, newItem: ProjectIdea): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IdeaViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemIdeaBinding.inflate(inflater, parent, false)
        return IdeaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IdeaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IdeaViewHolder(private val binding: ItemIdeaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val imageLoader =
            (binding.root.context.applicationContext as KiparysApplication).imageLoader

        fun bind(idea: ProjectIdea) = with(binding) {
            mtvIdeaDescription.text = idea.description

            if (!idea.imageUrl.isNullOrEmpty()) {
                val request = ImageRequest.Builder(binding.root.context)
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
                    .data(idea.imageUrl)
                    .precision(Precision.EXACT)
                    .transformations(RoundedCornersTransformation())
                    .target(binding.sivIdea)
                    .build()
                imageLoader.enqueue(request)
                sivIdea.visibility = View.VISIBLE
            } else {
                sivIdea.visibility = View.GONE
            }

            val thumbUpCount = idea.votes?.count { it.value } ?: 0
            if (thumbUpCount > 0) {
                mtvThumbUpCount.visibility = View.VISIBLE
                mtvThumbUpCount.text = String.format(Locale.getDefault(), "%,d", thumbUpCount)
            } else {
                mtvThumbUpCount.visibility = View.GONE
            }

            val isLikedByUser = idea.votes?.get(currentUserId) == true
            mbThumbUp.isSelected = isLikedByUser

            mbThumbUp.setOnClickListener {
                triggerSingleVibration(root.context)
                onThumbUpClickListener(idea)
            }

            binding.root.setOnLongClickListener {
                triggerSingleVibration(root.context)
                onIdeaLongClickListener(idea)
                true
            }

        }
    }

}
