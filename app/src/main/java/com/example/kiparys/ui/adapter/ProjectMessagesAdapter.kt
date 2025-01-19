package com.example.kiparys.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat.getString
import androidx.core.util.TypedValueCompat.dpToPx
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.CircleCropTransformation
import com.example.kiparys.KiparysApplication
import com.example.kiparys.R
import com.example.kiparys.data.model.MediaMetadata
import com.example.kiparys.data.model.ProjectMessage
import com.example.kiparys.data.model.ReplyInfo
import com.example.kiparys.data.repository.ProjectRepository
import com.example.kiparys.databinding.ItemMessageDateBinding
import com.example.kiparys.databinding.ItemMessageReceivedBinding
import com.example.kiparys.databinding.ItemMessageSentBinding
import com.example.kiparys.util.MimeTypeUtil.getExtensionFromMimeType
import com.example.kiparys.util.StringUtil.formatMessageTimestamp
import com.example.kiparys.util.SystemUtil.triggerSingleVibration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale


class ProjectMessagesAdapter(
    private val currentUserId: String,
    private val projectRepository: ProjectRepository,
    private val onProfileImageClick: (String) -> Unit,
    private val onMessageClickListener: (ProjectMessage) -> Unit,
    private val onReplyClickListener: (ReplyInfo) -> Unit,
    private val onMediaClick: (MediaMetadata) -> Unit,
    private val onFileClickListener: (File, String) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_RECEIVED = 1
        private const val VIEW_TYPE_SENT = 2
        private const val VIEW_TYPE_DATE = 3
    }

    public override fun getItem(position: Int): Any? {
        return super.getItem(position)
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item) {
            is ProjectMessage -> {
                if (item.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
            }

            is String -> VIEW_TYPE_DATE
            else -> throw IllegalArgumentException("Invalid item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(inflater, parent, false)
                SentMessageViewHolder(binding)
            }

            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(inflater, parent, false)
                ReceivedMessageViewHolder(binding, onProfileImageClick)
            }

            VIEW_TYPE_DATE -> {
                val binding = ItemMessageDateBinding.inflate(inflater, parent, false)
                DateViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> if (currentItem is ProjectMessage) holder.bind(currentItem)
            is ReceivedMessageViewHolder -> if (currentItem is ProjectMessage) holder.bind(
                currentItem
            )

            is DateViewHolder -> if (currentItem is String) holder.bind(currentItem)
        }
    }

    inner class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val imageLoader =
            (binding.root.context.applicationContext as KiparysApplication).imageLoader
        private val metrics = binding.root.context.resources.displayMetrics

        fun bind(message: ProjectMessage) = with(binding) {
            message.content?.let { content ->
                mtvSentMessageText.text = content
                mtvSentMessageText.visibility = View.VISIBLE
            } ?: run {
                mtvSentMessageText.visibility = View.GONE
            }

            sivUnreadMessage.visibility =
                if (message.showTime && message.seen == null) View.VISIBLE else View.GONE
            sivReadMessage.visibility =
                if (message.showTime && message.seen == true) View.VISIBLE else View.GONE

            mtvSentMessageText.visibility =
                if (!message.content.isNullOrEmpty()) View.VISIBLE else View.GONE
            sentMessageTime.text = formatMessageTimestamp(itemView.context, message.timestamp)
            sentMessageTime.visibility = if (message.showTime) View.VISIBLE else View.GONE
            mtvEdited.visibility = if (message.edited == true) View.VISIBLE else View.GONE
            sivPinnedIndicator.visibility = if (message.pinned == true) View.VISIBLE else View.GONE

            val layoutParams = clItemMessageSent.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.topMargin =
                if (message.showTime) dpToPx(4f, metrics).toInt() else dpToPx(2f, metrics).toInt()
            clItemMessageSent.layoutParams = layoutParams

            if (!message.media.isNullOrEmpty()) {
                rvMedia.visibility = View.VISIBLE
                val constraintSet = ConstraintSet()
                constraintSet.clone(binding.root)
                constraintSet.constrainPercentWidth(R.id.mcvSentMessageText, 0.89f)
                constraintSet.applyTo(binding.root)

                val mediaAdapter = MessageMediaAdapter(
                    onMediaClickListener = { mediaMetadata -> onMediaClick(mediaMetadata) },
                    isMultipleMedia = message.media.size > 1,
                    isReceivedMessage = false
                )
                rvMedia.apply {
                    adapter = mediaAdapter
                    layoutManager = StaggeredGridLayoutManager(
                        if (message.media.size == 1) 1 else 2,
                        StaggeredGridLayoutManager.VERTICAL
                    )
                    setHasFixedSize(true)
                }
                val mediaList = message.media.entries.map { entry ->
                    val media = entry.value
                    media.id = entry.key
                    media
                }.sortedBy { it.id ?: "" }
                mediaAdapter.submitList(mediaList)
            } else {
                rvMedia.visibility = View.GONE
                val constraintSet = ConstraintSet()
                constraintSet.clone(binding.root)
                constraintSet.constrainPercentWidth(R.id.mcvSentMessageText, 0.84f)
                constraintSet.applyTo(binding.root)
            }

            if (message.file != null) {
                mcvFile.visibility = View.VISIBLE
                cpiFileLoad.visibility =
                    if (message.file.loading == true) View.VISIBLE else View.GONE
                sivFileImage.visibility =
                    if (message.file.loading == true) View.INVISIBLE else View.VISIBLE
                mtvFileName.text = message.file.fileName
                mtvFileSize.text = root.context.getString(
                    R.string.file_placeholder_size,
                    message.file.size?.div(1024.0)?.div(1024.0)?.let {
                        String.format(Locale.getDefault(), "%.2f", it)
                    })
                mtvFileType.text = message.file.mimeType?.let {
                    getExtensionFromMimeType(it)
                }?.uppercase(Locale.getDefault())

                if (message.file.uploaded != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = projectRepository.downloadAndCacheFile(
                            context = itemView.context,
                            url = message.file.fileUrl.toString(),
                            mimeType = message.file.mimeType.toString(),
                            timestamp = message.file.uploaded
                        )

                        withContext(Dispatchers.Main) {
                            cpiFileLoad.visibility = View.VISIBLE
                            result.onSuccess { file ->
                                cpiFileLoad.visibility = View.GONE
                                mcvFile.setOnClickListener {
                                    onFileClickListener(file, message.file.mimeType.toString())
                                }
                            }.onFailure { error ->
                                cpiFileLoad.visibility = View.GONE
                                Toast.makeText(
                                    itemView.context, "Error downloading file: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            } else {
                mcvFile.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                triggerSingleVibration(root.context)
                onMessageClickListener(message)
            }

            message.replyTo?.let { replyInfo ->
                mcvReplyMessage.visibility = View.VISIBLE
                mtvReplyUserName.text = replyInfo.replySenderName

                mtvReplyMessageText.text = when {
                    !message.replyTo.replyContent.isNullOrEmpty() -> replyInfo.replyContent
                    message.replyTo.file == true -> getString(
                        binding.root.context,
                        R.string.last_message_file
                    )

                    message.replyTo.album == true -> getString(
                        binding.root.context,
                        R.string.last_message_album
                    )

                    message.replyTo.image == true -> getString(
                        binding.root.context,
                        R.string.last_message_image
                    )

                    else -> ""
                }

                val request = ImageRequest.Builder(binding.root.context)
                    .placeholder(R.drawable.baseline_circle_24)
                    .error(R.drawable.baseline_circle_24)
                    .data(replyInfo.replySenderImageUrl)
                    .precision(Precision.EXACT)
                    .transformations(CircleCropTransformation())
                    .target(binding.sivReplyUserProfileImage)
                    .build()
                imageLoader.enqueue(request)
                mcvReplyMessage.setOnClickListener {
                    onReplyClickListener(replyInfo)
                }
            } ?: run {
                mcvReplyMessage.visibility = View.GONE
            }
        }
    }

    inner class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding,
        private val onProfileImageClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val imageLoader =
            (binding.root.context.applicationContext as KiparysApplication).imageLoader
        private val metrics = binding.root.context.resources.displayMetrics

        fun bind(message: ProjectMessage) = with(binding) {
            message.content?.let { content ->
                mtvReceivedMessageText.text = content
                mtvReceivedMessageText.visibility = View.VISIBLE
            } ?: run {
                mtvReceivedMessageText.visibility = View.GONE
            }
            mtvUserName.text = message.senderName
            mtvUserName.visibility = if (message.showName) View.VISIBLE else View.GONE
            mtvReceivedMessageTime.text =
                formatMessageTimestamp(itemView.context, message.timestamp)
            mtvReceivedMessageTime.visibility = if (message.showTime) View.VISIBLE else View.GONE
            sivUserProfileImage.visibility = if (message.showAvatar) View.VISIBLE else View.GONE
            mtvEdited.visibility = if (message.edited == true) View.VISIBLE else View.GONE
            sivPinnedIndicator.visibility = if (message.pinned == true) View.VISIBLE else View.GONE

            val layoutParams = clItemMessageReceived.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.topMargin =
                if (message.showName) dpToPx(4f, metrics).toInt() else dpToPx(2f, metrics).toInt()
            clItemMessageReceived.layoutParams = layoutParams

            if (!message.media.isNullOrEmpty()) {
                rvMedia.visibility = View.VISIBLE
                val mediaAdapter = MessageMediaAdapter(
                    onMediaClickListener = { mediaMetadata -> onMediaClick(mediaMetadata) },
                    isMultipleMedia = message.media.size > 1,
                    isReceivedMessage = true
                )
                rvMedia.apply {
                    adapter = mediaAdapter
                    layoutManager = StaggeredGridLayoutManager(
                        if (message.media.size == 1) 1 else 2,
                        StaggeredGridLayoutManager.VERTICAL
                    )
                    setHasFixedSize(true)
                }
                val mediaList = message.media.entries.map { entry ->
                    val media = entry.value
                    media.id = entry.key
                    media
                }.sortedBy { it.id ?: "" }
                mediaAdapter.submitList(mediaList)
            } else {
                rvMedia.visibility = View.GONE
            }

            if (message.file != null) {
                mcvFile.visibility = View.VISIBLE
                cpiFileLoad.visibility =
                    if (message.file.loading == true) View.VISIBLE else View.GONE
                sivFileImage.visibility =
                    if (message.file.loading == true) View.INVISIBLE else View.VISIBLE
                mtvFileName.text = message.file.fileName
                mtvFileSize.text = root.context.getString(
                    R.string.file_placeholder_size,
                    message.file.size?.div(1024.0)?.div(1024.0)?.let {
                        String.format(Locale.getDefault(), "%.2f", it)
                    })
                mtvFileType.text = message.file.mimeType?.let {
                    getExtensionFromMimeType(it)
                }?.uppercase(Locale.getDefault())

                if (message.file.uploaded != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = projectRepository.downloadAndCacheFile(
                            context = itemView.context,
                            url = message.file.fileUrl.toString(),
                            mimeType = message.file.mimeType.toString(),
                            timestamp = message.file.uploaded
                        )

                        withContext(Dispatchers.Main) {
                            cpiFileLoad.visibility = View.VISIBLE
                            result.onSuccess { file ->
                                cpiFileLoad.visibility = View.GONE
                                mcvFile.setOnClickListener {
                                    onFileClickListener(file, message.file.mimeType.toString())
                                }
                            }.onFailure { error ->
                                cpiFileLoad.visibility = View.GONE
                                Toast.makeText(
                                    itemView.context, "Error downloading file: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            } else {
                mcvFile.visibility = View.GONE
            }

            val request = ImageRequest.Builder(binding.root.context)
                .placeholder(R.drawable.baseline_circle_24)
                .error(R.drawable.baseline_circle_24)
                .data(message.senderImageUrl)
                .precision(Precision.EXACT)
                .transformations(CircleCropTransformation())
                .target(binding.sivUserProfileImage)
                .build()
            imageLoader.enqueue(request)

            sivUserProfileImage.setOnClickListener {
                message.senderId?.let { senderId ->
                    onProfileImageClick(senderId)
                }
            }

            binding.root.setOnClickListener {
                triggerSingleVibration(root.context)
                onMessageClickListener(message)
            }

            message.replyTo?.let { replyInfo ->
                mcvReplyMessage.visibility = View.VISIBLE
                mtvReplyUserName.text = replyInfo.replySenderName

                mtvReplyMessageText.text = when {
                    !message.replyTo.replyContent.isNullOrEmpty() -> replyInfo.replyContent
                    message.replyTo.file == true -> getString(
                        binding.root.context,
                        R.string.last_message_file
                    )

                    message.replyTo.album == true -> getString(
                        binding.root.context,
                        R.string.last_message_album
                    )

                    message.replyTo.image == true -> getString(
                        binding.root.context,
                        R.string.last_message_image
                    )

                    else -> ""
                }

                val request = ImageRequest.Builder(binding.root.context)
                    .placeholder(R.drawable.baseline_circle_24)
                    .error(R.drawable.baseline_circle_24)
                    .data(replyInfo.replySenderImageUrl)
                    .precision(Precision.EXACT)
                    .transformations(CircleCropTransformation())
                    .target(binding.sivReplyUserProfileImage)
                    .build()
                imageLoader.enqueue(request)
                mcvReplyMessage.setOnClickListener {
                    onReplyClickListener(replyInfo)
                }
            } ?: run {
                mcvReplyMessage.visibility = View.GONE
            }
        }
    }

    inner class DateViewHolder(
        private val binding: ItemMessageDateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(date: String) {
            binding.mtvDate.text = date
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is ProjectMessage && newItem is ProjectMessage -> oldItem.id == newItem.id
                oldItem is String && newItem is String -> oldItem == newItem
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is ProjectMessage && newItem is ProjectMessage -> {
                    val isMediaSame = oldItem.media?.let { oldMedia ->
                        newItem.media?.let { newMedia ->
                            oldMedia.size == newMedia.size &&
                                    oldMedia.all { (key, oldValue) ->
                                        val newValue = newMedia[key]
                                        newValue != null &&
                                                oldValue.mediaUrl == newValue.mediaUrl &&
                                                oldValue.tempMediaUrl == newValue.tempMediaUrl &&
                                                oldValue.mimeType == newValue.mimeType &&
                                                oldValue.uploaded == newValue.uploaded &&
                                                oldValue.loading == newValue.loading
                                    }
                        }
                    } ?: (oldItem.media == null && newItem.media == null)


                    val isSeenSame = if (oldItem.showTime == true && newItem.showTime == true) {
                        oldItem.seen == newItem.seen
                    } else true

                    isMediaSame &&
                            isSeenSame &&
                            oldItem.senderName == newItem.senderName &&
                            oldItem.senderImageUrl == newItem.senderImageUrl &&
                            oldItem.content == newItem.content &&
                            oldItem.pinned == newItem.pinned &&
                            oldItem.edited == newItem.edited &&
                            oldItem.replyTo == newItem.replyTo &&
                            oldItem.file == newItem.file &&
                            oldItem.showName == newItem.showName &&
                            oldItem.showTime == newItem.showTime &&
                            oldItem.showAvatar == newItem.showAvatar
                }

                oldItem is String && newItem is String -> oldItem == newItem
                else -> false
            }
        }
    }

}
