package com.example.kiparys.ui.projectdetails

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import com.example.kiparys.Constants.ADD_IDEA
import com.example.kiparys.Constants.CAMERA
import com.example.kiparys.Constants.DATE_PICKER
import com.example.kiparys.Constants.FILES
import com.example.kiparys.Constants.MAX_FILE_SIZE
import com.example.kiparys.Constants.MAX_IMAGE_SIZE
import com.example.kiparys.Constants.PHOTO
import com.example.kiparys.Constants.TIME_PICKER
import com.example.kiparys.KiparysApplication
import com.example.kiparys.R
import com.example.kiparys.data.model.ProjectMessage
import com.example.kiparys.data.repository.ProjectRepository
import com.example.kiparys.databinding.BottomSheetCreateIdeaBinding
import com.example.kiparys.databinding.BottomSheetCreateTaskBinding
import com.example.kiparys.databinding.BottomSheetOptionsBinding
import com.example.kiparys.databinding.FragmentProjectChatBinding
import com.example.kiparys.ui.adapter.AttachToMessageOptionsAdapter
import com.example.kiparys.ui.adapter.ProjectMessageOptionsAdapter
import com.example.kiparys.ui.adapter.ProjectMessagesAdapter
import com.example.kiparys.ui.externalprofile.ExternalProfileDialogFragment
import com.example.kiparys.ui.main.MainViewModel
import com.example.kiparys.ui.projectgallery.ProjectGalleryDialogFragment
import com.example.kiparys.util.ErrorUtil
import com.example.kiparys.util.FilePathUtil.getFileNameFromUri
import com.example.kiparys.util.FilePathUtil.isDocumentType
import com.example.kiparys.util.ImageUtil.getBitmapFromUri
import com.example.kiparys.util.ImageUtil.getFileSizeFromUri
import com.example.kiparys.util.StringUtil.getMessageViewCountString
import com.example.kiparys.util.SystemUtil.isKeyboardVisible
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.getValue
import kotlin.text.isNullOrEmpty


class ProjectChatFragment : Fragment() {

    private var _binding: FragmentProjectChatBinding? = null
    private val fragmentProjectChatBinding get() = _binding!!
    private lateinit var currentPhotoPath: String
    private lateinit var photoURI: Uri
    private lateinit var captureImageLauncher: ActivityResultLauncher<Intent>
    private var isTypingWatcherEnabled = true
    private var attachOptionsDialog: BottomSheetDialog? = null
    private var createTaskDialog: BottomSheetDialog? = null
    private var createIdeaDialog: BottomSheetDialog? = null
    private var deleteMessageDialog: AlertDialog? = null
    private var messageViewCountDialog: AlertDialog? = null
    private var datePickerDialog: MaterialDatePicker<Long>? = null
    private var projectMessageOptionsDialog: BottomSheetDialog? = null
    private lateinit var projectMessagesAdapter: ProjectMessagesAdapter
    private val projectDetailsViewModel: ProjectDetailsViewModel by viewModels({ requireParentFragment() })
    private val mainViewModel: MainViewModel by activityViewModels()
    private val projectRepository: ProjectRepository by lazy { ProjectRepository() }
    private var typingJob: Job? = null
    private var hideDateJob: Job? = null
    private var previousAttachmentImages: MutableList<Attachment> = mutableListOf()
    private var previousAttachmentFile: Attachment? = null
    var memberSearchDialog: MemberSearchDialogFragment? = null
    var projectGalleryDialog: ProjectGalleryDialogFragment? = null
    var userProfileDialog: ExternalProfileDialogFragment? = null
    private var isGalleryOpen = false
    private var isFilePickerOpen = false
    private val fileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            isFilePickerOpen = false
            uri?.let {
                val fileSizeInBytes = getFileSizeFromUri(requireContext(), uri)
                val mimeType = requireContext().contentResolver.getType(uri)
                mimeType?.let { mimeType ->
                    if (mimeType.startsWith("image")) {
                        if (projectDetailsViewModel.projectMainScreenUiState.value.attachmentFile != null)
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.warning_different_type_attachment),
                                Toast.LENGTH_LONG
                            ).show()
                        else {
                            if (fileSizeInBytes > MAX_IMAGE_SIZE) {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.warning_image_size),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                val imageBitmap = getBitmapFromUri(requireContext(), uri)
                                projectDetailsViewModel.addAttachmentImageBitmap(
                                    uri = uri,
                                    imageBitmap = imageBitmap,
                                    mimeType = mimeType
                                )
                            }
                        }
                    } else if (isDocumentType(mimeType)) {
                        if (projectDetailsViewModel.projectMainScreenUiState.value.attachmentImages.isNotEmpty())
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.warning_different_type_attachment),
                                Toast.LENGTH_LONG
                            ).show()
                        else {
                            if (fileSizeInBytes > MAX_FILE_SIZE) {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.warning_file_size),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                val fileName = getFileNameFromUri(requireContext(), uri)
                                projectDetailsViewModel.addAttachmentFile(
                                    uri = uri,
                                    fileSize = fileSizeInBytes,
                                    fileName = fileName,
                                    mimeType = mimeType
                                )
                            }
                        }
                    }
                }
            }
        }
    private val galleryLauncherSingle =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            isGalleryOpen = false
            uri?.let {
                val fileSizeInBytes = getFileSizeFromUri(requireContext(), uri)
                val imageBitmap = getBitmapFromUri(requireContext(), uri)
                if (imageBitmap != null) {
                    if (fileSizeInBytes > MAX_IMAGE_SIZE) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.warning_image_size), Toast.LENGTH_LONG
                        ).show()
                    } else {
                        projectDetailsViewModel.setIdeaImageBitmap(imageBitmap)
                    }
                }
            }
        }
    private val galleryLauncherMultiply =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
            isGalleryOpen = false
            uris?.let { uriList ->
                if (projectDetailsViewModel.projectMainScreenUiState.value.attachmentFile != null)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.warning_different_type_attachment),
                        Toast.LENGTH_LONG
                    ).show()
                else {
                    uriList.forEach { uri ->
                        val fileSizeInBytes = getFileSizeFromUri(requireContext(), uri)
                        val imageBitmap = getBitmapFromUri(requireContext(), uri)
                        val mimeType = requireContext().contentResolver.getType(uri)
                        val currentAttachments =
                            projectDetailsViewModel.projectMainScreenUiState.value.attachmentImages

                        if (currentAttachments.size >= 20) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.warning_max_attachments),
                                Toast.LENGTH_LONG
                            ).show()
                            return@forEach
                        }

                        if (imageBitmap != null) {
                            if (fileSizeInBytes > MAX_IMAGE_SIZE) {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.warning_image_size),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                projectDetailsViewModel.addAttachmentImageBitmap(
                                    uri = uri,
                                    imageBitmap = imageBitmap,
                                    mimeType = mimeType
                                )
                            }
                        }
                    }
                }
            }
        }
    private val imm by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectChatBinding.inflate(inflater, container, false)
        return fragmentProjectChatBinding.root
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File =
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        captureImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    if (projectDetailsViewModel.projectMainScreenUiState.value.attachmentFile != null)
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.warning_different_type_attachment),
                            Toast.LENGTH_LONG
                        ).show()
                    else {
                        val imageBitmap = BitmapFactory.decodeFile(currentPhotoPath)
                        val imageFile = File(currentPhotoPath)
                        val fileSizeInBytes = imageFile.length()
                        val mimeType = requireContext().contentResolver.getType(photoURI)
                        if (fileSizeInBytes > MAX_IMAGE_SIZE) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.warning_image_size),
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            val currentAttachments =
                                projectDetailsViewModel.projectMainScreenUiState.value.attachmentImages
                            if (currentAttachments.size >= 20) {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.warning_max_attachments),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                if (projectDetailsViewModel.projectMainScreenUiState.value.currentOptionTag == ADD_IDEA) {
                                    projectDetailsViewModel.setIdeaImageBitmap(imageBitmap)
                                } else if (projectDetailsViewModel.projectMainScreenUiState.value.currentOptionTag == CAMERA) {
                                    projectDetailsViewModel.addAttachmentImageBitmap(
                                        uri = photoURI,
                                        imageBitmap = imageBitmap,
                                        mimeType = mimeType
                                    )
                                }
                            }
                        }
                    }
                }
            }

        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                super.onPause(owner)
                if (isKeyboardVisible(requireActivity())) {
                    imm.hideSoftInputFromWindow(fragmentProjectChatBinding.root.windowToken, 0)
                }
                val layoutManager =
                    fragmentProjectChatBinding.rvProjectMessages.layoutManager as? LinearLayoutManager
                val lastVisibleItemPosition = layoutManager?.findLastVisibleItemPosition() ?: 0
                val totalItemCount = layoutManager?.itemCount ?: 0
                val chatPosition = if (lastVisibleItemPosition == totalItemCount - 1) {
                    -2
                } else {
                    layoutManager?.findFirstVisibleItemPosition() ?: 0
                }
                projectDetailsViewModel.updateTypingState(false)
                projectDetailsViewModel.updateProjectTyping(false)
                projectDetailsViewModel.saveChatScrollPosition(chatPosition)
                projectDetailsViewModel.saveDraftChatInput(
                    fragmentProjectChatBinding.etMessageText.text.toString().trim()
                )
            }

            override fun onDestroy(owner: LifecycleOwner) {
                typingJob?.cancel()
                typingJob = null
                hideDateJob?.cancel()
                hideDateJob = null
                fragmentProjectChatBinding.rvProjectMessages.adapter = null
                super.onDestroy(owner)
            }
        })

        fragmentProjectChatBinding.mbAttach.setOnClickListener {
            showAttachOptionsBottomSheetDialog()
        }

        fragmentProjectChatBinding.mcvDate.background.alpha = 165

        fragmentProjectChatBinding.etMessageText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val messageInput = s.toString().trim()
                val hasAttachment = projectDetailsViewModel.projectMainScreenUiState.value.run {
                    attachmentImages.isNotEmpty() || attachmentFile != null
                }

                fragmentProjectChatBinding.mbSend.isEnabled =
                    messageInput.isNotEmpty() || hasAttachment
                fragmentProjectChatBinding.mbEdit.isEnabled =
                    messageInput.isNotEmpty() || hasAttachment

                if (!isTypingWatcherEnabled) return

                if (messageInput.isNotBlank() && projectDetailsViewModel.projectMainScreenUiState.value.isTyping == false) {
                    projectDetailsViewModel.updateTypingState(true)
                    projectDetailsViewModel.updateProjectTyping(true)
                }

                typingJob?.cancel()
                typingJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(2000)
                    projectDetailsViewModel.updateTypingState(false)
                    projectDetailsViewModel.updateProjectTyping(false)
                }

            }

            override fun afterTextChanged(s: Editable?) {}
        })

        fragmentProjectChatBinding.mbSend.setOnClickListener {
            val messageInput = fragmentProjectChatBinding.etMessageText.text.toString().trim()
            if (messageInput.isNotEmpty()
                || projectDetailsViewModel.projectMainScreenUiState.value.attachmentImages.isNotEmpty()
                || projectDetailsViewModel.projectMainScreenUiState.value.attachmentFile != null
            ) {
                val messageToTeam = when {
                    projectDetailsViewModel.projectMainScreenUiState.value.attachmentImages.isNotEmpty() ->
                        getString(
                            R.string.notification_message_send_images,
                            projectDetailsViewModel.projectMainScreenUiState.value.attachmentImages.size.toString()
                        )

                    projectDetailsViewModel.projectMainScreenUiState.value.attachmentFile != null ->
                        getString(R.string.notification_message_send_file)

                    else -> ""
                }
                projectDetailsViewModel.saveMessage(messageInput, messageToTeam)
                projectDetailsViewModel.updateProjectTyping(false)
                fragmentProjectChatBinding.etMessageText.text = null
                fragmentProjectChatBinding.rvProjectMessages.scrollToPosition(projectMessagesAdapter.itemCount - 1)
            }
        }

        fragmentProjectChatBinding.mbEdit.setOnClickListener {
            val messageInput = fragmentProjectChatBinding.etMessageText.text.toString().trim()
            if (messageInput.isNotEmpty()
                || projectDetailsViewModel.projectMainScreenUiState.value.attachmentImages.isNotEmpty()
                || projectDetailsViewModel.projectMainScreenUiState.value.attachmentFile != null
            ) {
                projectDetailsViewModel.updateMessage(
                    messageInput, getString(R.string.last_message_album),
                    getString(R.string.last_message_file),
                    getString(R.string.last_message_image)
                )
                projectDetailsViewModel.updateProjectTyping(false)
                fragmentProjectChatBinding.etMessageText.text = null
            }
        }

        fragmentProjectChatBinding.mbCloseEdit.setOnClickListener {
            fragmentProjectChatBinding.etMessageText.text = null
            projectDetailsViewModel.resetMessageStates()
        }

        fragmentProjectChatBinding.mbCloseReply.setOnClickListener {
            projectDetailsViewModel.closeReplyMessage()
        }

        projectMessagesAdapter = ProjectMessagesAdapter(
            currentUserId = mainViewModel.userId.value.toString(),
            projectRepository = projectRepository,
            onProfileImageClick = { userId ->
                projectDetailsViewModel.showMemberProfileDialog(userId)
            },
            onMessageClickListener = { showProjectMessageOptionsDialog(it) },
            onReplyClickListener = { replyInfo ->
                val messageToScroll =
                    projectDetailsViewModel.projectMessagesUiState.value.projectMessages.find { message ->
                        message is ProjectMessage && message.id == replyInfo.replyMessageId
                    } as? ProjectMessage
                messageToScroll?.let { message ->
                    val position = projectMessagesAdapter.currentList.indexOf(message)
                    if (position != -1) {
                        fragmentProjectChatBinding.rvProjectMessages.scrollToPosition(position)
                    }
                }
            },
            onMediaClick = { mediaMetadata ->
                projectDetailsViewModel.showGalleryDialog(mediaMetadata)
            },
            onFileClickListener = { file, mimeType ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider", file
                        ),
                        mimeType
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                requireView().context.startActivity(Intent.createChooser(intent, "Open file with"))
            }
        ).apply {
            registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    val layoutManager =
                        fragmentProjectChatBinding.rvProjectMessages.layoutManager as? LinearLayoutManager
                    layoutManager?.let {
                        var lastVisiblePosition = it.findLastCompletelyVisibleItemPosition()
                        var itemCountAfterFiltering = itemCount

                        if (lastVisiblePosition == -1) {
                            var nonStringItemsCount = 0
                            for (i in positionStart until positionStart + itemCount) {
                                val item = projectMessagesAdapter.getItem(i)
                                if (item !is String) {
                                    nonStringItemsCount++
                                }
                            }
                            lastVisiblePosition = projectMessagesAdapter.itemCount - itemCount
                            itemCountAfterFiltering = itemCount - nonStringItemsCount
                        }

                        val isAtBottom =
                            lastVisiblePosition == projectMessagesAdapter.itemCount - itemCountAfterFiltering - 1

                        if (isAtBottom) fragmentProjectChatBinding.rvProjectMessages.scrollToPosition(
                            projectMessagesAdapter.itemCount - 1
                        )
                        else projectDetailsViewModel.isAtBottomStateUpdate(false)
                    }
                }
            })
            stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        val badgeDrawable = BadgeDrawable.create(requireContext())
        fragmentProjectChatBinding.fabJumpDown.doOnPreDraw {
            @ExperimentalBadgeUtils
            BadgeUtils.attachBadgeDrawable(badgeDrawable, fragmentProjectChatBinding.fabJumpDown)
        }
        fragmentProjectChatBinding.rvProjectMessages.apply {
            adapter = projectMessagesAdapter
            layoutManager = LinearLayoutManager(context).apply {
                reverseLayout = false
                stackFromEnd = true
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    projectDetailsViewModel.updateLastSeenTimestamp()
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        projectDetailsViewModel.updateChatScrollState(RecyclerView.SCROLL_STATE_IDLE)
                        hideDateJob?.cancel()
                        hideDateJob = viewLifecycleOwner.lifecycleScope.launch {
                            delay(500)
                            fragmentProjectChatBinding.clCurrentRangeDate.visibility = View.GONE
                        }
                    } else {
                        hideDateJob?.cancel()
                        projectDetailsViewModel.updateChatScrollState(-1)
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val unreadCount =
                        projectDetailsViewModel.unreadMessagesCountUiState.value?.getOrNull() ?: 0
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                    val isAtBottom = !recyclerView.canScrollVertically(1)

                    if (dy > 0 && !isAtBottom && lastVisibleItemPosition < totalItemCount - 1) {
                        fragmentProjectChatBinding.fabJumpDown.doOnPreDraw {
                            badgeDrawable.isVisible = unreadCount > 0
                        }
                        fragmentProjectChatBinding.fabJumpDown.show()
                    } else if (dy < 0 || isAtBottom) {
                        fragmentProjectChatBinding.fabJumpDown.doOnPreDraw {
                            badgeDrawable.isVisible = false
                        }
                        fragmentProjectChatBinding.fabJumpDown.hide()
                    }

                    if (isAtBottom &&
                        projectDetailsViewModel.projectMainScreenUiState.value.initScrollingCompleted == true
                    ) {
                        projectDetailsViewModel.isAtBottomStateUpdate(true)
                        projectDetailsViewModel.updateLastSeenTimestamp()
                    }

                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    val messages =
                        projectDetailsViewModel.projectMessagesUiState.value.projectMessages

                    if (firstVisibleItemPosition >= 0
                        && firstVisibleItemPosition < messages.size
                        && projectDetailsViewModel.projectMainScreenUiState.value.chatScrollState != 0
                    ) {
                        val message = messages[firstVisibleItemPosition]
                        if (message is ProjectMessage) {
                            val timestampRange = message.timestamp
                            if (timestampRange != null) {
                                fragmentProjectChatBinding.clCurrentRangeDate.visibility =
                                    View.VISIBLE
                                fragmentProjectChatBinding.mtvDate.text =
                                    SimpleDateFormat("d MMMM", Locale.getDefault()).format(
                                        Date(timestampRange)
                                    )
                            } else {
                                fragmentProjectChatBinding.clCurrentRangeDate.visibility = View.GONE
                            }
                        } else {
                            fragmentProjectChatBinding.clCurrentRangeDate.visibility = View.GONE
                        }
                    } else {
                        fragmentProjectChatBinding.clCurrentRangeDate.visibility = View.GONE
                    }
                }
            })
        }

        fragmentProjectChatBinding.fabJumpDown.setOnClickListener {
            fragmentProjectChatBinding.rvProjectMessages.scrollToPosition(
                projectMessagesAdapter.itemCount - 1
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    mainViewModel.userPresenceInProjectChatState.collect { uiState -> }
                }
                launch {
                    if (projectDetailsViewModel.projectMainScreenUiState.value.isLoadChatState == true) {
                        fragmentProjectChatBinding.rvProjectMessages.doOnPreDraw {
                            fragmentProjectChatBinding.etMessageText.requestFocus()
                            fragmentProjectChatBinding.etMessageText.doOnPreDraw {
                                imm.hideSoftInputFromWindow(
                                    fragmentProjectChatBinding.etMessageText.windowToken,
                                    0
                                )
                            }
                        }
                    }
                }
                launch {
                    projectDetailsViewModel.unreadMessagesCountUiState.collect { uiState ->
                        uiState?.onSuccess { count ->
                            badgeDrawable.maxNumber = 99
                            badgeDrawable.number = count
                            badgeDrawable.isVisible = count > 0
                            projectDetailsViewModel.updateLastSeenTimestamp()
                            if (projectDetailsViewModel.projectMainScreenUiState.value.isAtBottom == false
                                && count > 0 && projectDetailsViewModel.projectMainScreenUiState.value.isLoadChatState == true
                            )
                                fragmentProjectChatBinding.rvProjectMessages.doOnPreDraw {
                                    fragmentProjectChatBinding.fabJumpDown.show()
                                }
                        }
                    }
                }
                launch {
                    projectDetailsViewModel.projectMessagesUiState.collect { uiState ->
                        fragmentProjectChatBinding.lpiProjectMessages.visibility =
                            if (uiState.isLoading) View.VISIBLE else View.GONE
                        projectMessagesAdapter.submitList(uiState.projectMessages)
                        {
                            projectDetailsViewModel.projectMainScreenUiState.value.chatScrollPosition?.let { position ->
                                if (position == -2)
                                    projectDetailsViewModel.isAtBottomStateUpdate(true)
                                else
                                    projectDetailsViewModel.isAtBottomStateUpdate(false)
                                if (uiState.projectMessages.isNotEmpty()) {
                                    fragmentProjectChatBinding.rvProjectMessages.doOnPreDraw {
                                        val layoutManager =
                                            fragmentProjectChatBinding.rvProjectMessages.layoutManager as LinearLayoutManager
                                        layoutManager.scrollToPositionWithOffset(position, 0)
                                        fragmentProjectChatBinding.rvProjectMessages.doOnPreDraw {
                                            projectDetailsViewModel.initScrollingCompleted()
                                            fragmentProjectChatBinding.rvProjectMessages.adapter?.let { adapter ->
                                                val lastVisibleItemPosition =
                                                    layoutManager.findLastVisibleItemPosition()
                                                if (lastVisibleItemPosition < adapter.itemCount - 1) {
                                                    fragmentProjectChatBinding.fabJumpDown.doOnPreDraw {
                                                        val unreadCount =
                                                            projectDetailsViewModel.unreadMessagesCountUiState.value?.getOrNull()
                                                        badgeDrawable.isVisible =
                                                            unreadCount?.let { it > 0 } == true
                                                    }
                                                    fragmentProjectChatBinding.fabJumpDown.show()
                                                    projectDetailsViewModel.initFabShown()
                                                }
                                                fragmentProjectChatBinding.rvProjectMessages.doOnPreDraw {
                                                    fragmentProjectChatBinding.etMessageText.requestFocus()
                                                    projectDetailsViewModel.projectMainScreenUiState.value.chatDraft?.let { draft ->
                                                        fragmentProjectChatBinding.etMessageText.doOnPreDraw {
                                                            isTypingWatcherEnabled = false
                                                            fragmentProjectChatBinding.etMessageText.setText(
                                                                draft
                                                            )
                                                            fragmentProjectChatBinding.etMessageText.setSelection(
                                                                draft.length
                                                            )
                                                            isTypingWatcherEnabled = true
                                                        }
                                                        projectDetailsViewModel.setInputStateSuccess()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    projectDetailsViewModel.loadChatStateCompletedSuccess()
                                } else projectDetailsViewModel.isAtBottomStateUpdate(true)
                                projectDetailsViewModel.updateLastSeenTimestamp()
                            }
                        }

                        if (!uiState.isLoading) {
                            fragmentProjectChatBinding.mtvProjectChatPreviewTitle.visibility =
                                if (uiState.projectMessages.isNotEmpty()) View.GONE else View.VISIBLE
                            fragmentProjectChatBinding.mtvProjectChatPreviewBody.visibility =
                                if (uiState.projectMessages.isNotEmpty()) View.GONE else View.VISIBLE

                            fragmentProjectChatBinding.clPinnedMessages.visibility =
                                if (uiState.pinnedMessages.isNotEmpty()) View.VISIBLE else View.GONE

                            if (projectDetailsViewModel.projectMainScreenUiState.value.setInputState == true ||
                                uiState.projectMessages.isEmpty()
                            ) {
                                fragmentProjectChatBinding.rvProjectMessages.doOnPreDraw {
                                    fragmentProjectChatBinding.etMessageText.requestFocus()
                                }
                            }

                            val chipGroup = fragmentProjectChatBinding.cgPinnedMessages
                            chipGroup.removeAllViews()

                            for (message in uiState.pinnedMessages) {
                                val chip = layoutInflater.inflate(
                                    R.layout.chip_pinned_message,
                                    chipGroup,
                                    false
                                ) as Chip

                                if (message.media != null) {
                                    chip.text =
                                        if (message.media.values.size == 1) getString(R.string.label_pinned_photo) else getString(
                                            R.string.label_pinned_album
                                        )

                                    val imageLoader =
                                        (requireContext().applicationContext as KiparysApplication).imageLoader
                                    val request = ImageRequest.Builder(requireContext())
                                        .data(message.media.values.first().mediaUrl)
                                        .transformations(RoundedCornersTransformation(264f))
                                        .placeholder(
                                            ColorDrawable(
                                                MaterialColors.getColor(
                                                    fragmentProjectChatBinding.root,
                                                    com.google.android.material.R.attr.colorSurfaceContainerLow
                                                )
                                            )
                                        )
                                        .error(
                                            ColorDrawable(
                                                MaterialColors.getColor(
                                                    fragmentProjectChatBinding.root,
                                                    com.google.android.material.R.attr.colorSurfaceContainerLow
                                                )
                                            )
                                        )
                                        .target { drawable -> chip.chipIcon = drawable }
                                        .build()

                                    imageLoader.enqueue(request)
                                } else if (message.file != null) {
                                    chip.text = message.file.fileName
                                } else chip.text = message.content

                                chip.setOnClickListener {
                                    val position =
                                        projectMessagesAdapter.currentList.indexOf(message)
                                    if (position != -1) {
                                        fragmentProjectChatBinding.rvProjectMessages.scrollToPosition(
                                            position
                                        )
                                    }
                                }
                                chip.setOnCloseIconClickListener {
                                    projectDetailsViewModel.setSelectedMessage(message)
                                    projectDetailsViewModel.unpinMessage()
                                }
                                chipGroup.addView(chip)
                            }
                        }
                    }
                }

                launch {
                    projectDetailsViewModel.projectMainScreenUiState.collect { uiState ->
                        uiState.error?.let { error ->
                            attachOptionsDialog?.dismiss()
                            deleteMessageDialog?.dismiss()
                            requireView().postDelayed({
                                Snackbar.make(
                                    requireView(),
                                    ErrorUtil.getErrorMessage(requireContext(), error),
                                    Snackbar.LENGTH_LONG
                                )
                                    .setAnchorView(fragmentProjectChatBinding.clMessageInput)
                                    .show()
                            }, 250)
                            projectDetailsViewModel.errorMessageShown()
                        }

                        fragmentProjectChatBinding.mbSend.isEnabled =
                            uiState.attachmentImages.isNotEmpty()
                                    || fragmentProjectChatBinding.etMessageText.text?.isNotEmpty() == true
                                    || projectDetailsViewModel.projectMainScreenUiState.value.attachmentFile != null
                        fragmentProjectChatBinding.mbEdit.isEnabled =
                            uiState.attachmentImages.isNotEmpty()
                                    || fragmentProjectChatBinding.etMessageText.text?.isNotEmpty() == true
                                    || projectDetailsViewModel.projectMainScreenUiState.value.attachmentFile != null
                        fragmentProjectChatBinding.mbSend.visibility =
                            if (uiState.isMessageEditing) View.INVISIBLE else View.VISIBLE
                        fragmentProjectChatBinding.mbEdit.visibility =
                            if (uiState.isMessageEditing) View.VISIBLE else View.INVISIBLE
                        fragmentProjectChatBinding.clEditMessage.visibility =
                            if (uiState.isMessageEditing) View.VISIBLE else View.GONE
                        fragmentProjectChatBinding.clReplyMessage.visibility =
                            if (uiState.isReplaying) View.VISIBLE else View.GONE

                        val chipGroup = fragmentProjectChatBinding.cgAttachments
                        fragmentProjectChatBinding.clAttachments.visibility =
                            if (uiState.attachmentImages.isNotEmpty() || uiState.attachmentFile != null) View.VISIBLE else View.GONE

                        if (uiState.attachmentFile != null) {
                            val file = uiState.attachmentFile

                            val existingFileChip = chipGroup.findViewWithTag<Chip>(file)
                            if (existingFileChip != null) {
                                chipGroup.removeView(existingFileChip)
                            } else {
                                for (i in 0 until chipGroup.childCount) {
                                    val child = chipGroup.getChildAt(i)
                                    if (child is Chip && child.tag is Attachment) {
                                        chipGroup.removeView(child)
                                        break
                                    }
                                }
                            }

                            val chip =
                                layoutInflater.inflate(R.layout.chip_file, chipGroup, false) as Chip
                            chip.isCloseIconVisible = true

                            val fileSizeInMB = file.size?.div(1024.0)?.div(1024.0)

                            val fileText =
                                getString(R.string.file_placeholder, file.name, fileSizeInMB)

                            val spannableText = SpannableStringBuilder(fileText).apply {
                                val startIndex = fileText.indexOf(
                                    String.format(
                                        Locale.getDefault(),
                                        "%.2f MB",
                                        fileSizeInMB
                                    )
                                )
                                val endIndex = startIndex + String.format(
                                    Locale.getDefault(),
                                    "%.2f MB",
                                    fileSizeInMB
                                ).length
                                setSpan(
                                    StyleSpan(Typeface.BOLD),
                                    startIndex,
                                    endIndex,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }

                            chip.text = spannableText
                            chip.tag = file
                            chip.setOnCloseIconClickListener {
                                projectDetailsViewModel.removeAttachmentFile()
                                chipGroup.removeView(chip)
                            }
                            chipGroup.addView(chip)
                        } else {
                            chipGroup.removeView(
                                chipGroup.findViewWithTag<Chip>(
                                    previousAttachmentFile
                                )
                            )
                            previousAttachmentFile = null
                        }

                        val newImages = uiState.attachmentImages.filterNot {
                            previousAttachmentImages.contains(it)
                        }
                        for (image in newImages) {
                            val chip = layoutInflater.inflate(
                                R.layout.chip_image,
                                chipGroup,
                                false
                            ) as Chip
                            chip.isCloseIconVisible = true
                            chip.tag = image

                            chip.setOnCloseIconClickListener {
                                projectDetailsViewModel.removeAttachmentImageSelection(image)
                            }

                            val imageLoader =
                                (requireContext().applicationContext as KiparysApplication).imageLoader
                            val request = ImageRequest.Builder(requireContext())
                                .data(image.bitmap ?: image.url)
                                .transformations(RoundedCornersTransformation(264f))
                                .placeholder(
                                    ColorDrawable(
                                        MaterialColors.getColor(
                                            fragmentProjectChatBinding.root,
                                            com.google.android.material.R.attr.colorSurfaceContainerLow
                                        )
                                    )
                                )
                                .error(
                                    ColorDrawable(
                                        MaterialColors.getColor(
                                            fragmentProjectChatBinding.root,
                                            com.google.android.material.R.attr.colorSurfaceContainerLow
                                        )
                                    )
                                )
                                .target { drawable -> chip.chipIcon = drawable }
                                .build()

                            imageLoader.enqueue(request)

                            chipGroup.addView(chip)
                        }

                        val removedImages = previousAttachmentImages.filterNot {
                            uiState.attachmentImages.contains(it)
                        }
                        for (image in removedImages) {
                            val chipToRemove = chipGroup.findViewWithTag<Chip>(image)
                            chipGroup.removeView(chipToRemove)
                        }

                        previousAttachmentImages.clear()
                        previousAttachmentImages.addAll(uiState.attachmentImages)
                        previousAttachmentFile = uiState.attachmentFile

                        if (uiState.saveIdeaSuccess) {
                            createIdeaDialog?.dismiss()
                            projectDetailsViewModel.saveIdeaSuccessMessageShown()
                        }

                        if (uiState.showMemberProfileDialogSuccess) {
                            ExternalProfileDialogFragment.show(childFragmentManager)
                            projectDetailsViewModel.memberProfileDialogShown()
                        }

                        if (uiState.showGalleryDialogSuccess) {
                            ProjectGalleryDialogFragment.show(childFragmentManager)
                            projectDetailsViewModel.galleryDialogShown()
                        }

                        if (uiState.saveTaskSuccess) {
                            createTaskDialog?.dismiss()
                            projectDetailsViewModel.saveTaskSuccessMessageShown()
                        }

                        if (uiState.showCreateIdeaDialog) {
                            showCreateIdeaBottomSheetDialog()
                            projectDetailsViewModel.createIdeaDialogShown()
                        }

                        if (uiState.showCreateTaskDialog) {
                            showCreateTaskBottomSheetDialog()
                            projectDetailsViewModel.createTaskDialogShown()
                        }

                        if (uiState.isReplaying && uiState.setupDataToReplyMessage) {
                            fragmentProjectChatBinding.mtvReplyMessageTitle.text = getString(
                                R.string.label_reply_to_message,
                                uiState.selectedProjectMessage?.senderName
                            )

                            val replyMessageBody = when {
                                !uiState.selectedProjectMessage?.content.isNullOrEmpty() -> uiState.selectedProjectMessage.content
                                uiState.selectedProjectMessage?.file != null -> getString(R.string.last_message_file)
                                !uiState.selectedProjectMessage?.media.isNullOrEmpty() -> {
                                    val mediaCount = uiState.selectedProjectMessage.media.size
                                    if (mediaCount == 1) getString(R.string.last_message_image)
                                    else getString(R.string.last_message_album)
                                }

                                else -> ""
                            }

                            fragmentProjectChatBinding.mtvReplyMessageBody.text = replyMessageBody

                            fragmentProjectChatBinding.etMessageText.requestFocus()
                            fragmentProjectChatBinding.etMessageText.postDelayed({
                                imm.showSoftInput(
                                    fragmentProjectChatBinding.etMessageText,
                                    InputMethodManager.SHOW_IMPLICIT
                                )
                            }, 100)
                            projectDetailsViewModel.replyFormShown()
                        }

                        if (uiState.isMessageCopied) {
                            val clipboard =
                                requireContext().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(
                                "message",
                                uiState.selectedProjectMessage?.content ?: ""
                            )
                            clipboard.setPrimaryClip(clip)
                            projectDetailsViewModel.copyMessageDone()
                        }

                        if (uiState.isMessageEditing && uiState.updateInputForEdit) {
                            isTypingWatcherEnabled = false
                            fragmentProjectChatBinding.etMessageText.setText(uiState.selectedProjectMessage?.content)
                            fragmentProjectChatBinding.etMessageText.requestFocus()
                            fragmentProjectChatBinding.etMessageText.postDelayed({
                                imm.showSoftInput(
                                    fragmentProjectChatBinding.etMessageText,
                                    InputMethodManager.SHOW_IMPLICIT
                                )
                            }, 100)
                            fragmentProjectChatBinding.etMessageText.text?.let {
                                fragmentProjectChatBinding.etMessageText.setSelection(it.length)
                            }
                            isTypingWatcherEnabled = true
                            projectDetailsViewModel.editMessageFormShown()
                        }

                        if (uiState.showConfirmDeleteMessageDialog) {
                            deleteMessageDialog = MaterialAlertDialogBuilder(requireContext())
                                .setTitle(getString(R.string.dialog_title_delete_message))
                                .setMessage(getString(R.string.dialog_message_delete_message))
                                .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton(getString(R.string.action_delete)) { dialog, _ ->
                                    projectDetailsViewModel.confirmDeleteMessage(
                                        getString(R.string.last_message_album),
                                        getString(R.string.last_message_file),
                                        getString(R.string.last_message_image)
                                    )
                                    dialog.dismiss()
                                }
                                .setOnDismissListener {
                                    deleteMessageDialog = null
                                }
                                .create()
                            deleteMessageDialog?.show()
                            projectDetailsViewModel.confirmDeleteMessageDialogShown()
                        }

                        if (uiState.showMessageViewCountDialog) {
                            messageViewCountDialog = MaterialAlertDialogBuilder(requireContext())
                                .setTitle(getString(R.string.dialog_title_message_views))
                                .setMessage(uiState.selectedMessageViewCount?.let {
                                    getMessageViewCountString(
                                        it
                                    )
                                })
                                .setPositiveButton(getString(R.string.action_ok)) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setOnDismissListener {
                                    messageViewCountDialog = null
                                }
                                .create()
                            messageViewCountDialog?.show()
                            projectDetailsViewModel.messageViewCountDialogShown()
                        }

                    }
                }

            }
        }

    }

    private fun showCreateIdeaBottomSheetDialog() {
        if (createIdeaDialog?.isShowing == true) {
            return
        }

        createIdeaDialog = BottomSheetDialog(requireContext())
        createIdeaDialog?.behavior?.isFitToContents = true
        createIdeaDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        val bottomSheetCreateIdeaBinding = BottomSheetCreateIdeaBinding.inflate(layoutInflater)

        bottomSheetCreateIdeaBinding.etIdeaDescription.setText(
            projectDetailsViewModel.projectMainScreenUiState.value.selectedProjectMessage?.content)
        bottomSheetCreateIdeaBinding.etIdeaDescription.requestFocus()
        bottomSheetCreateIdeaBinding.etIdeaDescription.postDelayed({
            imm.showSoftInput(
                fragmentProjectChatBinding.etMessageText,
                InputMethodManager.SHOW_IMPLICIT
            )
        }, 100)
        bottomSheetCreateIdeaBinding.etIdeaDescription.text?.let {
            bottomSheetCreateIdeaBinding.etIdeaDescription.setSelection(it.length)
        }

        bottomSheetCreateIdeaBinding.etIdeaDescription.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                bottomSheetCreateIdeaBinding.mbSaveIdea.isEnabled = !s.isNullOrEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        bottomSheetCreateIdeaBinding.mbAttachPhoto.setOnClickListener {
            bottomSheetCreateIdeaBinding.root.clearFocus()
            if (isKeyboardVisible(requireActivity())) {
                imm.hideSoftInputFromWindow(bottomSheetCreateIdeaBinding.root.windowToken, 0)
                bottomSheetCreateIdeaBinding.root.post { checkCameraPermission() }
            } else {
                checkCameraPermission()
            }
        }

        bottomSheetCreateIdeaBinding.mbAttachPicture.setOnClickListener {
            bottomSheetCreateIdeaBinding.root.clearFocus()
            if (!isGalleryOpen) {
                isGalleryOpen = true
                bottomSheetCreateIdeaBinding.root.clearFocus()
                if (isKeyboardVisible(requireActivity())) {
                    imm.hideSoftInputFromWindow(bottomSheetCreateIdeaBinding.root.windowToken, 0)
                    bottomSheetCreateIdeaBinding.root.postDelayed(
                        { galleryLauncherSingle.launch("image/*") },
                        300
                    )
                } else {
                    galleryLauncherSingle.launch("image/*")
                }
            }
        }

        bottomSheetCreateIdeaBinding.mbSaveIdea.setOnClickListener {
            val ideaDescriptionInput = bottomSheetCreateIdeaBinding.etIdeaDescription.text
                .toString().trim().replace("\\s+".toRegex(), " ")
            val messageToMember = getString(
                R.string.notification_message_new_idea,
                projectDetailsViewModel.getCurrentUserDisplayName(),
                ideaDescriptionInput
            )
            projectDetailsViewModel.saveIdea(
                ideaDescription = ideaDescriptionInput,
                messageToMember = messageToMember
            )
        }

        var previousImageBitmap: Bitmap? = null

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                projectDetailsViewModel.projectMainScreenUiState.collect { uiState ->
                    bottomSheetCreateIdeaBinding.mbSaveIdea.isEnabled = !uiState.isSaveIdea &&
                            !bottomSheetCreateIdeaBinding.etIdeaDescription.text.isNullOrEmpty()
                    bottomSheetCreateIdeaBinding.cpiIdeaSave.visibility =
                        if (uiState.isSaveIdea) View.VISIBLE else View.GONE
                    bottomSheetCreateIdeaBinding.mbSaveIdea.text =
                        if (uiState.isSaveIdea) getString(R.string.prompt_empty_string) else getString(
                            R.string.action_save
                        )

                    if (uiState.ideaImageBitmap != previousImageBitmap) {
                        previousImageBitmap = uiState.ideaImageBitmap

                        val chipGroup = bottomSheetCreateIdeaBinding.cgImage
                        chipGroup.removeAllViews()

                        bottomSheetCreateIdeaBinding.hsvChipGroup.visibility =
                            if (uiState.ideaImageBitmap != null) View.VISIBLE else View.GONE

                        uiState.ideaImageBitmap?.let { bitmap ->
                            val chipImage = layoutInflater.inflate(
                                R.layout.chip_image,
                                chipGroup,
                                false
                            ) as Chip
                            val imageLoader =
                                (requireContext().applicationContext as KiparysApplication).imageLoader
                            val request = ImageRequest.Builder(requireContext())
                                .data(bitmap)
                                .transformations(RoundedCornersTransformation(2f))
                                .placeholder(
                                    ColorDrawable(
                                        MaterialColors.getColor(
                                            fragmentProjectChatBinding.root,
                                            com.google.android.material.R.attr.colorSurfaceContainerLow
                                        )
                                    )
                                )
                                .error(
                                    ColorDrawable(
                                        MaterialColors.getColor(
                                            fragmentProjectChatBinding.root,
                                            com.google.android.material.R.attr.colorSurfaceContainerLow
                                        )
                                    )
                                )
                                .target { drawable -> chipImage.chipIcon = drawable }
                                .build()
                            imageLoader.enqueue(request)

                            chipImage.setOnCloseIconClickListener {
                                projectDetailsViewModel.setIdeaImageBitmap(null)
                            }
                            chipGroup.addView(chipImage)
                        }
                    }
                }
            }
        }

        createIdeaDialog?.setOnDismissListener {
            bottomSheetCreateIdeaBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetCreateIdeaBinding.root)
            }
            projectDetailsViewModel.setIdeaImageBitmap(null)
            createIdeaDialog = null
        }

        createIdeaDialog?.setContentView(bottomSheetCreateIdeaBinding.root)
        createIdeaDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        if (isKeyboardVisible(requireActivity())) {
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
            requireView().postDelayed({
                createIdeaDialog?.show()
            }, 60)
        } else {
            createIdeaDialog?.show()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_camera_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_camera_permission_required),
                    Toast.LENGTH_LONG
                ).show()
            }

            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val photoFile: File = createImageFile()
        photoURI = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        }
        captureImageLauncher.launch(intent)
    }

    private fun showCreateTaskBottomSheetDialog() {
        if (createTaskDialog?.isShowing == true) {
            return
        }

        createTaskDialog = BottomSheetDialog(requireContext())
        createTaskDialog?.behavior?.isFitToContents = true
        createTaskDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        val bottomSheetCreateTaskBinding = BottomSheetCreateTaskBinding.inflate(layoutInflater)

        bottomSheetCreateTaskBinding.etTaskName.setText(projectDetailsViewModel.projectMainScreenUiState.value.selectedProjectMessage?.content)
        bottomSheetCreateTaskBinding.etTaskName.requestFocus()
        bottomSheetCreateTaskBinding.etTaskName.postDelayed({
            imm.showSoftInput(
                fragmentProjectChatBinding.etMessageText,
                InputMethodManager.SHOW_IMPLICIT
            )
        }, 100)
        bottomSheetCreateTaskBinding.etTaskName.text?.let {
            bottomSheetCreateTaskBinding.etTaskName.setSelection(it.length)
        }

        bottomSheetCreateTaskBinding.etTaskName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                bottomSheetCreateTaskBinding.mbSaveTask.isEnabled = !s.isNullOrEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        bottomSheetCreateTaskBinding.mbAddAssignedUser.setOnClickListener {
            bottomSheetCreateTaskBinding.root.clearFocus()
            if (isKeyboardVisible(requireActivity())) {
                imm.hideSoftInputFromWindow(bottomSheetCreateTaskBinding.root.windowToken, 0)
                bottomSheetCreateTaskBinding.root.postDelayed({
                    MemberSearchDialogFragment.show(
                        childFragmentManager
                    )
                }, 300)
            } else {
                MemberSearchDialogFragment.show(childFragmentManager)
            }
        }

        bottomSheetCreateTaskBinding.mbAddDue.setOnClickListener {
            bottomSheetCreateTaskBinding.root.clearFocus()
            if (isKeyboardVisible(requireActivity())) {
                imm.hideSoftInputFromWindow(bottomSheetCreateTaskBinding.root.windowToken, 0)
                bottomSheetCreateTaskBinding.root.postDelayed({ showDateTimePickerDialog() }, 300)
            } else {
                showDateTimePickerDialog()
            }
        }

        bottomSheetCreateTaskBinding.mbSaveTask.setOnClickListener {
            val taskNameInput = bottomSheetCreateTaskBinding.etTaskName.text.toString().trim()
                .replace("\\s+".toRegex(), " ")
            val taskDescriptionInput =
                bottomSheetCreateTaskBinding.etTaskDescription.text.toString().trim()
                    .replace("\\s+".toRegex(), " ")
            val messageToMember =
                if (projectDetailsViewModel.projectMainScreenUiState.value.taskDueTimestamp != null)
                    getString(
                        R.string.notification_message_to_assigned_member_with_due,
                        projectDetailsViewModel.getCurrentUserDisplayName(),
                        taskNameInput,
                        SimpleDateFormat("EEE, d MMM, HH:mm", Locale.getDefault())
                            .format(projectDetailsViewModel.projectMainScreenUiState.value.taskDueTimestamp)
                    )
                else
                    getString(
                        R.string.notification_message_to_assigned_member,
                        projectDetailsViewModel.getCurrentUserDisplayName(),
                        taskNameInput
                    )
            projectDetailsViewModel.saveTask(
                taskName = taskNameInput,
                taskDescription = if (taskDescriptionInput.isEmpty()) null else taskDescriptionInput,
                messageToMember = messageToMember
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                projectDetailsViewModel.projectMainScreenUiState.collect { uiState ->
                    bottomSheetCreateTaskBinding.mbSaveTask.isEnabled = !uiState.isSaveTask &&
                            !bottomSheetCreateTaskBinding.etTaskName.text.isNullOrEmpty()
                    bottomSheetCreateTaskBinding.cpiTaskSave.visibility =
                        if (uiState.isSaveTask) View.VISIBLE else View.GONE
                    bottomSheetCreateTaskBinding.mbSaveTask.text =
                        if (uiState.isSaveTask) getString(R.string.prompt_empty_string) else getString(
                            R.string.action_save
                        )

                    val chipGroup = bottomSheetCreateTaskBinding.cgDueAssigned
                    chipGroup.removeAllViews()

                    bottomSheetCreateTaskBinding.hsvChipGroup.visibility =
                        if (uiState.taskDueTimestamp != null || uiState.taskAssignedUser != null)
                            View.VISIBLE else View.GONE

                    if (uiState.taskDueTimestamp != null || uiState.taskAssignedUser != null) {
                        val chipDue =
                            layoutInflater.inflate(R.layout.chip_due, chipGroup, false) as Chip
                        if (uiState.taskDueTimestamp != null) {
                            chipDue.text =
                                SimpleDateFormat("EEE, d MMM, HH:mm", Locale.getDefault())
                                    .format(uiState.taskDueTimestamp)
                            chipDue.setOnClickListener {
                                showDateTimePickerDialog()
                            }
                            chipDue.setOnCloseIconClickListener {
                                projectDetailsViewModel.setTaskDueTimestamp(null)
                            }
                            chipGroup.addView(chipDue)
                        }

                        val chipUser =
                            layoutInflater.inflate(R.layout.chip_user, chipGroup, false) as Chip
                        if (uiState.taskAssignedUser != null) {
                            chipUser.text = uiState.taskAssignedUser.email
                            chipUser.setOnCloseIconClickListener {
                                projectDetailsViewModel.setAssignedUser(
                                    null
                                )
                            }
                            chipUser.setOnClickListener {
                                MemberSearchDialogFragment.show(
                                    childFragmentManager
                                )
                            }
                            val imageLoader =
                                (requireContext().applicationContext as KiparysApplication).imageLoader
                            val request = ImageRequest.Builder(requireContext())
                                .data(uiState.taskAssignedUser.profileImageUrl)
                                .precision(Precision.EXACT)
                                .transformations(CircleCropTransformation())
                                .placeholder(R.drawable.baseline_circle_24)
                                .error(R.drawable.baseline_circle_24)
                                .target { drawable ->
                                    chipUser.chipIcon = drawable
                                }
                                .build()
                            imageLoader.enqueue(request)
                            chipGroup.addView(chipUser)
                        }

                    }

                }
            }
        }


        createTaskDialog?.setOnDismissListener {
            bottomSheetCreateTaskBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetCreateTaskBinding.root)
            }
            projectDetailsViewModel.setTaskDueTimestamp(null)
            projectDetailsViewModel.setAssignedUser(null)
            createTaskDialog = null
        }

        createTaskDialog?.setContentView(bottomSheetCreateTaskBinding.root)
        createTaskDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        if (isKeyboardVisible(requireActivity())) {
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
            requireView().postDelayed({
                createTaskDialog?.show()
            }, 60)
        } else {
            createTaskDialog?.show()
        }
    }

    private fun showDateTimePickerDialog() {
        if (datePickerDialog != null && datePickerDialog?.isAdded == true) {
            return
        }

        val calendar = Calendar.getInstance()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.date_picker_title_task_due))
            .setSelection(calendar.timeInMillis)

        datePickerDialog = datePicker.build()
        datePickerDialog?.addOnPositiveButtonClickListener { dateSelection ->
            calendar.timeInMillis = dateSelection

            val timePickerDialog = MaterialTimePicker.Builder()
                .setTitleText(getString(R.string.time_picker_title_task_due))
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .build()

            timePickerDialog.addOnPositiveButtonClickListener {
                calendar.set(Calendar.HOUR_OF_DAY, timePickerDialog.hour)
                calendar.set(Calendar.MINUTE, timePickerDialog.minute)
                projectDetailsViewModel.setTaskDueTimestamp(calendar.timeInMillis)
            }

            timePickerDialog.show(parentFragmentManager, TIME_PICKER)

        }

        datePickerDialog?.addOnDismissListener {
            datePickerDialog = null
        }

        datePickerDialog?.show(parentFragmentManager, DATE_PICKER)

    }

    private fun showAttachOptionsBottomSheetDialog() {
        if (attachOptionsDialog?.isShowing == true) {
            return
        }

        attachOptionsDialog = BottomSheetDialog(requireContext())
        attachOptionsDialog?.behavior?.isFitToContents = true
        attachOptionsDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        val bottomSheetAttachOptionsBinding = BottomSheetOptionsBinding.inflate(layoutInflater)

        val adapter = AttachToMessageOptionsAdapter(requireContext()) { selectedOption ->
            projectDetailsViewModel.updateOptionTag(selectedOption.tag)
            if (projectDetailsViewModel.projectMainScreenUiState.value.isMessageEditing) {
                requireView().post {
                    Snackbar.make(
                        requireView(),
                        getString(R.string.error_attach_when_editing_message),
                        Snackbar.LENGTH_LONG
                    )
                        .setAnchorView(fragmentProjectChatBinding.clMessageInput)
                        .show()
                }
            } else {
                when (selectedOption.tag) {
                    PHOTO ->
                        if (!isGalleryOpen) {
                            isGalleryOpen = true
                            bottomSheetAttachOptionsBinding.root.clearFocus()
                            if (isKeyboardVisible(requireActivity())) {
                                imm.hideSoftInputFromWindow(
                                    bottomSheetAttachOptionsBinding.root.windowToken,
                                    0
                                )
                                bottomSheetAttachOptionsBinding.root.post {
                                    galleryLauncherMultiply.launch(
                                        "image/*"
                                    )
                                }
                            } else {
                                galleryLauncherMultiply.launch("image/*")
                            }
                        }

                    CAMERA -> {
                        bottomSheetAttachOptionsBinding.root.clearFocus()
                        if (isKeyboardVisible(requireActivity())) {
                            imm.hideSoftInputFromWindow(
                                bottomSheetAttachOptionsBinding.root.windowToken,
                                0
                            )
                            bottomSheetAttachOptionsBinding.root.post { checkCameraPermission() }
                        } else {
                            checkCameraPermission()
                        }
                    }

                    FILES -> {
                        if (!isFilePickerOpen) {
                            isFilePickerOpen = true
                            bottomSheetAttachOptionsBinding.root.clearFocus()
                            if (isKeyboardVisible(requireActivity())) {
                                imm.hideSoftInputFromWindow(
                                    bottomSheetAttachOptionsBinding.root.windowToken,
                                    0
                                )
                                bottomSheetAttachOptionsBinding.root.post { fileLauncher.launch("*/*") }
                            } else {
                                fileLauncher.launch("*/*")
                            }
                        }
                    }
                }
            }
            attachOptionsDialog?.dismiss()
        }
        bottomSheetAttachOptionsBinding.rvOptions.adapter = adapter
        bottomSheetAttachOptionsBinding.rvOptions.layoutManager = LinearLayoutManager(context)

        attachOptionsDialog?.setOnDismissListener {
            fragmentProjectChatBinding.root.clearFocus()
            bottomSheetAttachOptionsBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetAttachOptionsBinding.root)
            }
            attachOptionsDialog = null
        }

        attachOptionsDialog?.setContentView(bottomSheetAttachOptionsBinding.root)
        attachOptionsDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        if (isKeyboardVisible(requireActivity())) {
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
            requireView().postDelayed({
                attachOptionsDialog?.show()
            }, 60)
        } else {
            attachOptionsDialog?.show()
        }
    }


    private fun showProjectMessageOptionsDialog(message: ProjectMessage) {
        if (projectMessageOptionsDialog?.isShowing == true) {
            return
        }

        projectMessageOptionsDialog = BottomSheetDialog(requireContext())
        projectMessageOptionsDialog?.behavior?.isFitToContents = true
        projectMessageOptionsDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        val bottomSheetSelectTwoFactorBinding = BottomSheetOptionsBinding.inflate(layoutInflater)

        val adapter = mainViewModel.userId.value?.let {
            ProjectMessageOptionsAdapter(requireContext(), it, message) { selectedOption ->
                projectDetailsViewModel.handleProjectMessageOptionSelection(selectedOption, message)
                projectMessageOptionsDialog?.dismiss()
            }
        }
        bottomSheetSelectTwoFactorBinding.rvOptions.adapter = adapter
        bottomSheetSelectTwoFactorBinding.rvOptions.layoutManager = LinearLayoutManager(context)

        projectMessageOptionsDialog?.setOnDismissListener {
            bottomSheetSelectTwoFactorBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetSelectTwoFactorBinding.root)
            }
            projectMessageOptionsDialog = null
        }

        projectMessageOptionsDialog?.setContentView(bottomSheetSelectTwoFactorBinding.root)
        projectMessageOptionsDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        if (isKeyboardVisible(requireActivity())) {
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
            requireView().post { projectMessageOptionsDialog?.show() }
        } else {
            projectMessageOptionsDialog?.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
