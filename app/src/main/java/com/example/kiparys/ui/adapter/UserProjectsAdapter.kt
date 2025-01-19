package com.example.kiparys.ui.adapter

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.CircleCropTransformation
import com.example.kiparys.KiparysApplication
import com.example.kiparys.data.model.UserProject
import com.example.kiparys.R
import com.example.kiparys.databinding.ItemProjectBinding
import com.example.kiparys.util.StringUtil.formatUserProjectTimestamp
import com.example.kiparys.util.SystemUtil.triggerSingleVibration
import com.google.android.material.color.MaterialColors


class UserProjectsAdapter(
    private val currentUserId: String,
    private val onItemClickListener: (UserProject) -> Unit,
    private val onItemLongClickListener: (UserProject) -> Unit
) : ListAdapter<UserProject, UserProjectsAdapter.ProjectViewHolder>(differCallback) {

    companion object {
        private val differCallback = object : DiffUtil.ItemCallback<UserProject>() {
            override fun areItemsTheSame(oldItem: UserProject, newItem: UserProject): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: UserProject, newItem: UserProject): Boolean {
                return oldItem.name == newItem.name &&
                        oldItem.projectImageUrl == newItem.projectImageUrl &&
                        oldItem.lastMessage == newItem.lastMessage &&
                        oldItem.draft == newItem.draft &&
                        oldItem.timestamp == newItem.timestamp &&
                        oldItem.lastSeenTimestamp == newItem.lastSeenTimestamp &&
                        oldItem.unreadMessagesCount == newItem.unreadMessagesCount &&
                        oldItem.notifications == newItem.notifications &&
                        oldItem.pinned == newItem.pinned
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProjectViewHolder(private val binding: ItemProjectBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val imageLoader =
            (binding.root.context.applicationContext as KiparysApplication).imageLoader

        fun bind(project: UserProject) = with(binding) {
            val request = ImageRequest.Builder(binding.root.context)
                .placeholder(R.drawable.baseline_circle_24)
                .error(R.drawable.baseline_circle_24)
                .data(project.projectImageUrl)
                .precision(Precision.EXACT)
                .transformations(CircleCropTransformation())
                .target(binding.sivProjectImage)
                .build()
            imageLoader.enqueue(request)

            mtvProjectName.text = project.name

            val spannableMessage = SpannableStringBuilder()

            if (project.draft != null) {
                val draftLabel = root.context.getString(R.string.label_draft_message)
                val highContrastErrorColor =
                    MaterialColors.getColor(root, com.google.android.material.R.attr.colorError)
                spannableMessage.append(draftLabel)
                spannableMessage.setSpan(
                    ForegroundColorSpan(highContrastErrorColor),
                    0,
                    draftLabel.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannableMessage.append(" ${project.draft}")
                mtvLastMessage.text = spannableMessage
            } else if (project.lastMessage != null) {
                val lastMessage = project.lastMessage
                val colonIndex = lastMessage.indexOf(':')
                if (colonIndex != -1) {
                    val highContrastColor = MaterialColors.getColor(
                        root,
                        com.google.android.material.R.attr.colorSurfaceInverse
                    )

                    val senderId = project.senderId
                    if (senderId == currentUserId) {
                        val localizedSender = root.context.getString(R.string.label_you)
                        spannableMessage.append(localizedSender)
                            .append(lastMessage.substring(colonIndex))
                        spannableMessage.setSpan(
                            ForegroundColorSpan(highContrastColor),
                            0,
                            localizedSender.length + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    } else {
                        spannableMessage.append(lastMessage)
                        spannableMessage.setSpan(
                            ForegroundColorSpan(highContrastColor),
                            0,
                            colonIndex + 1,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                } else {
                    spannableMessage.append(lastMessage)
                }
                mtvLastMessage.text = spannableMessage
            } else {
                mtvLastMessage.text = ""
            }
            mtvLastMessage.text = spannableMessage

            val formattedTimestamp =
                project.timestamp?.let { formatUserProjectTimestamp(it) } ?: getString(
                    root.context,
                    R.string.label_unknown_time
                )
            mtvTimestamp.text = formattedTimestamp

            sivNotificationOffIndicator.visibility =
                if (project.notifications == false) View.VISIBLE else View.GONE

            if (project.pinned == true) {
                root.setBackgroundColor(
                    MaterialColors.getColor(
                        root,
                        com.google.android.material.R.attr.colorSurfaceContainer
                    )
                )
                sivPinnedIndicator.visibility = View.VISIBLE
            } else {
                root.setBackgroundColor(
                    MaterialColors.getColor(
                        root,
                        com.google.android.material.R.attr.colorSurface
                    )
                )
                sivPinnedIndicator.visibility = View.GONE
            }

            if (project.unreadMessagesCount != null) {
                binding.mtvUnreadCount.text =
                    root.context.getString(R.string.label_integer, project.unreadMessagesCount)
                binding.mcvUnreadCount.visibility = View.VISIBLE
            } else {
                binding.mcvUnreadCount.visibility = View.GONE
            }

            val isUnread = project.timestamp?.let { lastMessageTime ->
                project.lastSeenTimestamp?.let { lastSeenTime ->
                    lastMessageTime > lastSeenTime && project.unreadMessagesCount == null
                }
            } == true

            sivUnreadIndicator.visibility = if (isUnread) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onItemClickListener(project)
            }

            binding.root.setOnLongClickListener {
                triggerSingleVibration(root.context)
                onItemLongClickListener(project)
                true
            }
        }

    }

}
