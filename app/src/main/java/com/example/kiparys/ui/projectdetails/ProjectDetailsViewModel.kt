package com.example.kiparys.ui.projectdetails

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiparys.Constants.ADD_IDEA
import com.example.kiparys.Constants.ADD_TASK
import com.example.kiparys.Constants.COMPLETED_TASK
import com.example.kiparys.Constants.COPY_MESSAGE
import com.example.kiparys.Constants.DELETE_IDEA
import com.example.kiparys.Constants.DELETE_MESSAGE
import com.example.kiparys.Constants.DELETE_TASK
import com.example.kiparys.Constants.EDIT_IDEA
import com.example.kiparys.Constants.EDIT_MESSAGE
import com.example.kiparys.Constants.EDIT_TASK
import com.example.kiparys.Constants.ERROR_PIN_MESSAGE_LIMIT_EXCEEDED
import com.example.kiparys.Constants.ERROR_PROJECT_NOT_FOUND
import com.example.kiparys.Constants.ERROR_USER_NOT_FOUND
import com.example.kiparys.Constants.IMAGE_MEDIUM_QUALITY
import com.example.kiparys.Constants.JPG_EXTENSION
import com.example.kiparys.Constants.LIKE_IDEA
import com.example.kiparys.Constants.PIN_MESSAGE
import com.example.kiparys.Constants.PROJECT_IDEAS
import com.example.kiparys.Constants.PROJECT_IDEAS_CHANNEL_ID
import com.example.kiparys.Constants.PROJECT_MESSAGES
import com.example.kiparys.Constants.PROJECT_MESSAGES_CHANNEL_ID
import com.example.kiparys.Constants.PROJECT_TASKS
import com.example.kiparys.Constants.PROJECT_TASKS_CHANNEL_ID
import com.example.kiparys.Constants.REPLY_MESSAGE
import com.example.kiparys.Constants.SHOW_VIEW_COUNT
import com.example.kiparys.Constants.UNCOMPLETED_TASK
import com.example.kiparys.Constants.UNLIKE_IDEA
import com.example.kiparys.Constants.UNPIN_MESSAGE
import com.example.kiparys.data.model.AssignedUser
import com.example.kiparys.data.model.BottomSheetOption
import com.example.kiparys.data.model.FileMetadata
import com.example.kiparys.data.model.MediaMetadata
import com.example.kiparys.data.model.Project
import com.example.kiparys.data.model.ProjectFile
import com.example.kiparys.data.model.ProjectIdea
import com.example.kiparys.data.model.ProjectMedia
import com.example.kiparys.data.model.ProjectMessage
import com.example.kiparys.data.model.ProjectTask
import com.example.kiparys.data.model.ReplyInfo
import com.example.kiparys.data.model.User
import com.example.kiparys.data.model.UserTask
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.DataManagementRepository
import com.example.kiparys.data.repository.MessagingRepository
import com.example.kiparys.data.repository.ProjectRepository
import com.example.kiparys.data.repository.UserRepository
import com.example.kiparys.util.FilePathUtil
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.indexOfFirst
import kotlin.collections.map
import kotlin.collections.orEmpty
import kotlin.collections.set


data class ProjectDetailsUiState(
    val project: Project? = null,
)

data class MemberProfileUiState(
    val memberData: User? = null,
    val isLoading: Boolean = true,

    val error: Throwable? = null,
)

data class ProjectMediaUiState(
    val projectMedia: List<ProjectMedia> = emptyList(),
    val isLoading: Boolean = false
)

data class ProjectMessagesUiState(
    val projectMessages: List<Any> = emptyList(),
    val pinnedMessages: List<ProjectMessage> = emptyList(),
    val isLoading: Boolean = false
)

data class ProjectTasksUiState(
    val projectTasks: List<ProjectTask> = emptyList(),
    val isLoading: Boolean = false
)

data class ProjectIdeasUiState(
    val projectIdeas: List<ProjectIdea> = emptyList(),
    val isLoading: Boolean = false
)

data class Attachment(
    val bitmap: Bitmap? = null,
    val url: String? = null,
    val uri: Uri? = null,
    val size: Long? = null,
    val name: String? = null,
    val mimeType: String? = null
)

data class ProjectGalleryUiState(
    val showSystemBars: Boolean = true,
    val mediaMetadata: MediaMetadata? = null,
    val initMediaIndex: Int? = null,

    val error: Throwable? = null,
)

data class ProjectMainScreenUiState(
    val currentOptionTag: String? = null,
    val taskDueTimestamp: Long? = null,
    val taskAssignedUser: User? = null,
    val ideaImageBitmap: Bitmap? = null,
    val chatScrollState: Int? = 0,
    val chatDraft: String? = null,
    val chatScrollPosition: Int? = null,
    val selectedMessageViewCount: Int? = null,
    val selectedProjectTask: ProjectTask? = null,
    val selectedProjectIdea: ProjectIdea? = null,
    val selectedProjectMessage: ProjectMessage? = null,
    val foundMembers: List<User> = emptyList(),
    val attachmentImages: List<Attachment> = emptyList(),
    val attachmentFile: Attachment? = null,

    val isTyping: Boolean? = false,
    val isAtBottom: Boolean = false,
    val initFabShown: Boolean = false,
    val updateInputForEdit: Boolean = false,
    val setInputState: Boolean = false,
    val setupDataToReplyMessage: Boolean = false,
    val initScrollingCompleted: Boolean = false,
    val toggleIdeaVoteStateSuccess: Boolean? = null,
    val toggleTaskStateSuccess: Boolean? = null,
    val saveIdeaSuccess: Boolean = false,
    val saveTaskSuccess: Boolean = false,
    val searchMembersByEmailSuccess: Boolean = false,
    val showConfirmDeleteMessageDialog: Boolean = false,
    val showConfirmDeleteIdeaDialog: Boolean = false,
    val showCreateIdeaDialog: Boolean = false,
    val showCreateTaskDialog: Boolean = false,
    val showConfirmDeleteTaskDialog: Boolean = false,
    val showMessageViewCountDialog: Boolean = false,
    val showGalleryDialogSuccess: Boolean = false,
    val showMemberProfileDialogSuccess: Boolean = false,
    val saveMessageSuccess: Boolean = false,

    val isGetMessageViewCount: Boolean? = null,
    val isSaveChatScrollPosition: Boolean? = null,
    val isSaveDraftChatInput: Boolean? = null,
    val isLoadChatState: Boolean? = false,
    val isIdeaDelete: Boolean? = null,
    val isTaskDelete: Boolean? = null,
    val isToggleIdeaVoteState: Boolean? = null,
    val isToggleTaskState: Boolean? = null,
    val isSaveIdea: Boolean = false,
    val isSaveTask: Boolean = false,
    val isSearchMembersByEmail: Boolean = false,
    val isReplaying: Boolean = false,
    val isUnpinMessage: Boolean = false,
    val isPinMessage: Boolean = false,
    val isMessageDelete: Boolean = false,
    val isMessageUpdate: Boolean = false,
    val isMessageCopied: Boolean = false,
    val isUpdateLastSeenTimestamp: Boolean = false,
    val isSaveMessage: Boolean = false,
    val isMessageEditing: Boolean = false,

    val error: Throwable? = null,
)


class ProjectDetailsViewModel(
    private val userId: StateFlow<String?>,
    private val projectId: StateFlow<String?>,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val dataManagementRepository: DataManagementRepository,
    private val messagingRepository: MessagingRepository
) : ViewModel() {
    private var deleteIdeaJob: Job? = null
    private var unpinMessageJob: Job? = null
    private var pinMessageJob: Job? = null
    private var deleteMessageJob: Job? = null
    private var saveMessageJob: Job? = null
    private var editMessageJob: Job? = null
    private var updateLastSeenTimestampJob: Job? = null
    private var saveTaskJob: Job? = null
    private var saveIdeaJob: Job? = null
    private var toggleTaskStateJob: Job? = null
    private var toggleIdeaVoteStateJob: Job? = null
    private var deleteTaskJob: Job? = null
    private var saveChatScrollPositionJob: Job? = null
    private var saveDraftChatInputJob: Job? = null
    private var updateProjectTypingJob: Job? = null
    private var getMessageViewCountJob: Job? = null

    private val _projectMainScreenUiState = MutableStateFlow(ProjectMainScreenUiState())
    val projectMainScreenUiState: StateFlow<ProjectMainScreenUiState> = _projectMainScreenUiState

    private val _projectGalleryScreenUiState = MutableStateFlow(ProjectGalleryUiState())
    val projectGalleryScreenUiState: StateFlow<ProjectGalleryUiState> = _projectGalleryScreenUiState

    @OptIn(ExperimentalCoroutinesApi::class)
    val projectDetailsUiState: StateFlow<ProjectDetailsUiState> = projectId
        .flatMapLatest { id ->
            if (id != null) {
                projectRepository.getProjectFlow(id)
                    .map { result ->
                        _projectMainScreenUiState.update { it.copy(error = result.exceptionOrNull()) }
                        ProjectDetailsUiState(
                            project = result.getOrNull()
                        )
                    }
            } else {
                flowOf(ProjectDetailsUiState())
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProjectDetailsUiState()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val projectMessagesUiState: StateFlow<ProjectMessagesUiState> = projectId
        .flatMapLatest { id ->
            if (id != null) {
                projectRepository.getProjectMessagesFlow(id)
                    .map { result ->
                        _projectMainScreenUiState.update { it.copy(error = result.exceptionOrNull()) }
                        val rawMessages = result.getOrNull() ?: emptyList()
                        val preparedMessages = prepareMessages(rawMessages)
                        ProjectMessagesUiState(
                            projectMessages = addDateSeparatorsAsync(preparedMessages).await(),
                            pinnedMessages = preparedMessages.filter { it.pinned == true },
                            isLoading = false
                        )
                    }
                    .onStart {
                        if (_projectMainScreenUiState.value.isLoadChatState == false) {
                            val userId = authRepository.getCurrentUserId()
                            require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                            val projectId =
                                requireNotNull(projectId.value) { ERROR_PROJECT_NOT_FOUND }
                            val userProject = projectRepository.getUserProject(
                                userId = userId,
                                projectId = projectId
                            ).getOrThrow()
                            _projectMainScreenUiState.update {
                                it.copy(
                                    chatDraft = userProject.draft,
                                    chatScrollPosition = userProject.chatPosition
                                )
                            }
                        }
                    }
            } else {
                flowOf(ProjectMessagesUiState(isLoading = false))
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ProjectMessagesUiState(isLoading = true)
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadMessagesCountUiState: StateFlow<Result<Int>?> =
        combine(userId, projectId) { userId, projectId -> userId to projectId }
            .flatMapLatest { (userId, projectId) ->
                if (userId != null && projectId != null) {
                    userRepository.getUnreadProjectMessagesCountFlow(userId, projectId)
                        .catch { e -> emit(Result.failure(e)) }
                } else {
                    flowOf(null)
                }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    fun initFabShown() {
        _projectMainScreenUiState.update { it.copy(initFabShown = true) }
    }

    fun setInputStateSuccess() {
        _projectMainScreenUiState.update { it.copy(chatDraft = null, setInputState = true) }
    }

    fun loadChatStateCompletedSuccess() {
        _projectMainScreenUiState.update {
            it.copy(
                chatScrollPosition = null,
                isLoadChatState = true
            )
        }
    }

    fun initScrollingCompleted() {
        _projectMainScreenUiState.update { it.copy(initScrollingCompleted = true) }
    }

    fun addDateSeparatorsAsync(messages: List<ProjectMessage>): Deferred<List<Any>> =
        viewModelScope.async(Dispatchers.Default) {
            val messagesWithDates = mutableListOf<Any>()
            val dateFormatter = SimpleDateFormat("d MMMM", Locale.getDefault())
            var lastDate: String? = null

            for (message in messages) {
                val messageDate = message.timestamp?.let { dateFormatter.format(Date(it)) }

                if (messageDate != null && messageDate != lastDate) {
                    messagesWithDates.add(messageDate)
                    lastDate = messageDate
                }

                messagesWithDates.add(message)
            }
            messagesWithDates
        }

    private fun prepareMessages(messages: List<ProjectMessage>): List<ProjectMessage> {
        for (i in messages.indices) {
            val currentMessage = messages[i]
            val prevMessage = messages.getOrNull(i - 1)
            val nextMessage = messages.getOrNull(i + 1)

            currentMessage.showName = prevMessage == null ||
                    prevMessage.senderId != currentMessage.senderId ||
                    isTimeGapExceeded(prevMessage.timestamp, currentMessage.timestamp)

            currentMessage.showAvatar = nextMessage == null ||
                    nextMessage.senderId != currentMessage.senderId ||
                    isTimeGapExceeded(currentMessage.timestamp, nextMessage.timestamp)

            currentMessage.showTime = currentMessage.showName
        }
        return messages
    }

    private fun isTimeGapExceeded(prevTimestamp: Long?, currentTimestamp: Long?): Boolean {
        return (currentTimestamp ?: 0) - (prevTimestamp ?: 0) > 5 * 60 * 1000
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val projectTasksUiState: StateFlow<ProjectTasksUiState> = projectId
        .flatMapLatest { id ->
            if (id != null) {
                projectRepository.getProjectTasksFlow(id)
                    .map { result ->
                        _projectMainScreenUiState.update { it.copy(error = result.exceptionOrNull()) }
                        ProjectTasksUiState(
                            projectTasks = result.getOrNull() ?: emptyList(),
                            isLoading = false
                        )
                    }
            } else {
                flowOf(ProjectTasksUiState(isLoading = false))
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProjectTasksUiState(isLoading = true)
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val projectIdeasUiState: StateFlow<ProjectIdeasUiState> = projectId
        .flatMapLatest { id ->
            if (id != null) {
                projectRepository.getProjectIdeasFlow(id)
                    .map { result ->
                        _projectMainScreenUiState.update { it.copy(error = result.exceptionOrNull()) }
                        ProjectIdeasUiState(
                            projectIdeas = result.getOrNull() ?: emptyList(),
                            isLoading = false
                        )
                    }
            } else {
                flowOf(ProjectIdeasUiState(isLoading = false))
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProjectIdeasUiState(isLoading = true)
        )


    @OptIn(ExperimentalCoroutinesApi::class)
    val projectMedia: StateFlow<ProjectMediaUiState> = projectId
        .flatMapLatest { id ->
            if (id != null) {
                projectRepository.getProjectMediaFlow(id)
                    .map { result ->
                        _projectGalleryScreenUiState.update { it.copy(error = result.exceptionOrNull()) }
                        val projectMedia = result.getOrNull() ?: emptyList()
                        val initMediaIndex =
                            projectMedia.indexOfFirst {
                                it.id == _projectGalleryScreenUiState.value.mediaMetadata?.id
                            }
                        _projectGalleryScreenUiState.update {
                            it.copy(
                                initMediaIndex = if (initMediaIndex != -1) initMediaIndex else null
                            )
                        }
                        ProjectMediaUiState(
                            projectMedia = projectMedia,
                            isLoading = false
                        )
                    }
            } else {
                flowOf(ProjectMediaUiState(isLoading = false))
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = ProjectMediaUiState(isLoading = true)
        )

    private val _memberId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val memberProfileData: StateFlow<MemberProfileUiState> = _memberId
        .flatMapLatest { id ->
            if (id != null) {
                userRepository.getMemberDataFlow(id)
                    .map { result ->
                        MemberProfileUiState(
                            memberData = result.getOrNull(),
                            isLoading = false,
                            error = result.exceptionOrNull()
                        )
                    }
            } else {
                flowOf(MemberProfileUiState(isLoading = true))
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = MemberProfileUiState(isLoading = true)
        )

    fun toggleIdeaVoteState(idea: ProjectIdea) {
        if (toggleIdeaVoteStateJob?.isActive == true) return

        toggleIdeaVoteStateJob = viewModelScope.launch {
            try {
                _projectMainScreenUiState.update { it.copy(isToggleIdeaVoteState = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId = requireNotNull(projectId.value) { "Project ID cannot be null" }
                val ideaId = requireNotNull(idea.id) { "Idea ID cannot be null" }
                val updatedVotes = idea.votes.orEmpty().toMutableMap().apply {
                    if (this[userId] == true) {
                        remove(userId)
                    } else {
                        this[userId] = true
                    }
                }
                val updates = mapOf(
                    "votes" to updatedVotes
                )
                projectRepository.updateProjectIdea(
                    projectId = projectId,
                    ideaId = ideaId,
                    updates = updates
                ).getOrThrow()
                _projectMainScreenUiState.update { it.copy(toggleIdeaVoteStateSuccess = true) }
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isToggleIdeaVoteState = false) }
                toggleIdeaVoteStateJob = null
            }
        }
    }

    fun toggleTaskState(task: ProjectTask, doneState: Boolean) {
        if (toggleTaskStateJob?.isActive == true) return

        toggleTaskStateJob = viewModelScope.launch {
            try {
                _projectMainScreenUiState.update { it.copy(isToggleTaskState = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId = requireNotNull(projectId.value) { "Project ID cannot be null" }
                val taskId = requireNotNull(task.id) { "Task ID cannot be null" }
                val updates = mapOf("completed" to if (doneState) true else null)
                async {
                    projectRepository.updateProjectTask(
                        projectId = projectId,
                        taskId = taskId,
                        updates = updates
                    ).getOrThrow()
                }
                task.assignedUser?.let { assignedUser ->
                    val assignedUserId =
                        requireNotNull(assignedUser.assignedId) { "Assigned user ID cannot be null" }
                    async {
                        userRepository.updateUserTask(
                            userId = assignedUserId,
                            taskId = taskId,
                            updates = updates
                        ).getOrThrow()
                    }
                }
                _projectMainScreenUiState.update { it.copy(toggleTaskStateSuccess = true) }
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isToggleTaskState = false) }
                toggleTaskStateJob = null
            }
        }
    }

    fun searchMembersByEmail(userEmail: String) {
        viewModelScope.launch {
            try {
                _projectMainScreenUiState.update { it.copy(isSearchMembersByEmail = true) }
                val users = userRepository.searchMembersByEmail(
                    userEmail,
                    projectDetailsUiState.value.project?.members
                ).getOrThrow()
                _projectMainScreenUiState.update {
                    it.copy(
                        searchMembersByEmailSuccess = true,
                        foundMembers = users
                    )
                }
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isSearchMembersByEmail = false) }
            }
        }
    }

    fun saveTask(taskName: String, taskDescription: String?, messageToMember: String) {
        if (saveTaskJob?.isActive == true) return

        saveTaskJob = viewModelScope.launch {
            try {
                _projectMainScreenUiState.update { it.copy(isSaveTask = true) }
                val creatorId = authRepository.getCurrentUserId()
                val creatorName = authRepository.getCurrentUserDisplayName()
                val creatorImageUrl = authRepository.getCurrentUserPhotoUrl()
                require(
                    !creatorId.isNullOrEmpty()
                            && !creatorName.isNullOrEmpty()
                            && !creatorImageUrl.isNullOrEmpty()
                ) { ERROR_USER_NOT_FOUND }

                val projectId = projectId.value
                require(!projectId.isNullOrEmpty()) { ERROR_PROJECT_NOT_FOUND }
                val project = projectDetailsUiState.value.project

                val assignedUser =
                    _projectMainScreenUiState.value.taskAssignedUser?.let { taskAssignedUser ->
                        val fullName = if (taskAssignedUser.lastName.isNullOrEmpty()) {
                            taskAssignedUser.firstName
                        } else {
                            "${taskAssignedUser.firstName} ${taskAssignedUser.lastName}"
                        }
                        AssignedUser(
                            assignedId = taskAssignedUser.id,
                            assignedName = fullName,
                            assignedImageUrl = taskAssignedUser.profileImageUrl
                        )
                    }

                val dueDate = _projectMainScreenUiState.value.taskDueTimestamp
                val currentTime = System.currentTimeMillis()

                val projectTask = ProjectTask(
                    name = taskName,
                    description = taskDescription,
                    created = currentTime,
                    dueDate = dueDate,
                    assignedUser = assignedUser
                ).toMap()

                val newProjectTaskKey = async {
                    projectRepository.saveProjectTask(projectId, projectTask).getOrThrow()
                }.await()
                requireNotNull(newProjectTaskKey) { "Task key is null" }

                assignedUser?.let {
                    val assignedUserId = requireNotNull(it.assignedId) { ERROR_USER_NOT_FOUND }
                    async {
                        userRepository.updateUserTask(
                            userId = assignedUserId,
                            taskId = newProjectTaskKey,
                            updates = UserTask(
                                projectId = projectId,
                                projectName = project?.name.orEmpty(),
                                name = taskName,
                                description = taskDescription,
                                created = currentTime,
                                dueDate = dueDate
                            ).toMap()
                        ).getOrThrow()
                    }

                    if (assignedUserId != creatorId) {
                        launch(Dispatchers.IO) {
                            messagingRepository.sendNotification(
                                users = setOf<String>(assignedUserId),
                                notificationType = PROJECT_TASKS,
                                title = project?.name.orEmpty(),
                                senderImage = project?.projectImageUrl,
                                body = messageToMember,
                                channelId = PROJECT_TASKS_CHANNEL_ID,
                                projectId = projectId
                            ).onFailure {
                                Log.e(
                                    TAG,
                                    "Failed to send notification after retries: ${it.message}"
                                )
                            }
                        }
                    }
                }
                _projectMainScreenUiState.update { it.copy(saveTaskSuccess = true) }
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isSaveTask = false) }
                saveTaskJob = null
            }
        }
    }

    fun saveIdea(ideaDescription: String, messageToMember: String) {
        if (saveIdeaJob?.isActive == true) return

        saveIdeaJob = viewModelScope.launch {
            try {
                _projectMainScreenUiState.update { it.copy(isSaveIdea = true) }

                val creatorId = authRepository.getCurrentUserId()
                val creatorName = authRepository.getCurrentUserDisplayName()
                val creatorImageUrl = authRepository.getCurrentUserPhotoUrl()

                require(
                    !creatorId.isNullOrEmpty()
                            && !creatorName.isNullOrEmpty()
                            && !creatorImageUrl.isNullOrEmpty()
                ) { ERROR_USER_NOT_FOUND }

                val project = projectDetailsUiState.value.project
                val projectId = projectId.value
                require(!projectId.isNullOrEmpty()) { ERROR_PROJECT_NOT_FOUND }

                val ideaBitmap = _projectMainScreenUiState.value.ideaImageBitmap
                val profileImageUrlDeferred = if (ideaBitmap != null) {
                    async {
                        projectRepository.uploadProjectMediaJpeg(
                            ideaBitmap,
                            FilePathUtil.generateFilePath(
                                FilePathUtil.FileType.PROJECT_IDEA_MEDIA_ATTACHMENT_UPLOAD,
                                projectId = projectId, extension = JPG_EXTENSION
                            ),
                            IMAGE_MEDIUM_QUALITY
                        ).getOrThrow().first
                    }.await()
                } else {
                    null
                }

                val currentTime = System.currentTimeMillis()
                val votes = mapOf(creatorId to true)

                val ideaData = ProjectIdea(
                    description = ideaDescription,
                    imageUrl = profileImageUrlDeferred,
                    created = currentTime,
                    votes = votes
                ).toMap()

                async { projectRepository.saveProjectIdea(projectId, ideaData).getOrThrow() }

                val filteredUsers = project?.members?.filter {
                    it.value && it.key != creatorId
                }?.keys.orEmpty()

                if (filteredUsers.isNotEmpty()) {
                    launch(Dispatchers.IO) {
                        messagingRepository.sendNotification(
                            users = filteredUsers,
                            notificationType = PROJECT_IDEAS,
                            title = project?.name.toString(),
                            body = messageToMember,
                            senderImage = project?.projectImageUrl,
                            channelId = PROJECT_IDEAS_CHANNEL_ID,
                            projectId = projectId
                        ).onFailure {
                            Log.e(
                                TAG,
                                "Failed to send notification after retries: ${it.message}"
                            )
                        }
                    }
                }

                _projectMainScreenUiState.update { it.copy(saveIdeaSuccess = true) }
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isSaveIdea = false) }
                saveIdeaJob = null
            }
        }
    }

    fun saveMessage(message: String, notificationMessage: String? = null) {
        if (saveMessageJob?.isActive == true) return

        saveMessageJob = viewModelScope.launch {
            try {
                _projectMainScreenUiState.update { it.copy(isSaveMessage = true) }

                val senderId = authRepository.getCurrentUserId()
                val senderName = authRepository.getCurrentUserDisplayName()
                val senderImageUrl = authRepository.getCurrentUserPhotoUrl()
                require(
                    !senderId.isNullOrEmpty()
                            && !senderName.isNullOrEmpty()
                            && !senderImageUrl.isNullOrEmpty()
                )
                { ERROR_USER_NOT_FOUND }

                val currentTime = System.currentTimeMillis()
                val project = projectDetailsUiState.value.project
                val projectId = projectId.value
                require(!projectId.isNullOrEmpty()) { ERROR_PROJECT_NOT_FOUND }

                val attachmentImages = _projectMainScreenUiState.value.attachmentImages
                val attachmentFile = _projectMainScreenUiState.value.attachmentFile

                val replyInfo = if (_projectMainScreenUiState.value.isReplaying == true) {
                    val replyMessage = _projectMainScreenUiState.value.selectedProjectMessage
                    _projectMainScreenUiState.update { it.copy(isReplaying = false) }
                    ReplyInfo(
                        replyMessageId = replyMessage?.id,
                        replySenderId = replyMessage?.senderId,
                        replySenderImageUrl = replyMessage?.senderImageUrl,
                        replySenderName = replyMessage?.senderName,
                        album = replyMessage?.media?.size?.let { if (it > 1) true else null },
                        image = replyMessage?.media?.size?.let { if (it == 1) true else null },
                        file = if (replyMessage?.file != null) true else null,
                        replyContent = replyMessage?.content
                    )
                } else {
                    null
                }

                val mediaMap = mutableMapOf<String, MediaMetadata>()

                val fileKey = if (attachmentFile != null) {
                    _projectMainScreenUiState.update { it.copy(attachmentFile = null) }
                    projectRepository.saveProjectFile(
                        projectId,
                        ProjectFile(
                            loading = true,
                            fileName = attachmentFile.name,
                            size = attachmentFile.size,
                            mimeType = attachmentFile.mimeType,
                            uploaderId = senderId,
                            uploaderName = senderName,
                            uploaderImageUrl = senderImageUrl,
                        ).toMap()
                    ).getOrThrow()
                } else null

                val fileMetadata = if (attachmentFile != null)
                    FileMetadata(
                        fileId = fileKey,
                        loading = true,
                        fileName = attachmentFile.name,
                        size = attachmentFile.size,
                        mimeType = attachmentFile.mimeType,
                    )
                else null

                if (attachmentImages.isNotEmpty()) {
                    _projectMainScreenUiState.update { it.copy(attachmentImages = emptyList()) }
                    attachmentImages.forEach { attachment ->
                        val aspectRatio = attachment.bitmap?.let {
                            (it.width.toFloat() / it.height.toFloat())
                        }

                        val mediaKey = projectRepository.saveProjectMedia(
                            projectId,
                            ProjectMedia(
                                tempMediaUrl = attachment.uri.toString(),
                                loading = true,
                                aspectRatio = aspectRatio,
                                uploaderId = senderId,
                                uploaderName = senderName,
                                uploaderImageUrl = senderImageUrl,
                            ).toMap()
                        ).getOrThrow()

                        mediaKey?.let { key ->
                            mediaMap[key] = MediaMetadata(
                                tempMediaUrl = attachment.uri.toString(),
                                aspectRatio = aspectRatio,
                                loading = true
                            )
                        }
                    }
                }

                val initialMessageData = ProjectMessage(
                    senderId = senderId,
                    senderName = senderName,
                    senderImageUrl = senderImageUrl,
                    content = if (message.isEmpty()) null else message,
                    timestamp = currentTime,
                    replyTo = replyInfo,
                    media = mediaMap,
                    file = fileMetadata
                ).toMap()

                val messageIdDeferred = async {
                    projectRepository.saveMessage(projectId, initialMessageData).getOrThrow()
                }

                async {
                    projectRepository.saveMessageSeenBy(
                        projectId,
                        messageIdDeferred.await().toString(),
                        senderId
                    ).getOrThrow()
                }

                if (attachmentFile != null) {
                    val messageId = messageIdDeferred.await().toString()
                    async {
                        val uri = attachmentFile.uri
                        require(uri != null) { "Uri can not be null" }
                        val (fileUrl) = projectRepository.uploadProjectFile(
                            uri,
                            FilePathUtil.generateFilePath(
                                FilePathUtil.FileType.PROJECT_CHAT_FILE_ATTACHMENT_UPLOAD,
                                projectId = projectId,
                                name = attachmentFile.name
                            )
                        ).getOrThrow()

                        val uploadedTime = System.currentTimeMillis()

                        require(fileKey != null) { "fileKey can not be null" }
                        awaitAll(
                            async {
                                projectRepository.updateProjectFile(
                                    projectId,
                                    fileKey,
                                    mapOf(
                                        "loading" to null,
                                        "fileUrl" to fileUrl,
                                        "uploaded" to uploadedTime,
                                        "projectMessageId" to messageId
                                    )
                                ).getOrThrow()
                            },
                            async {
                                projectRepository.updateProjectMessage(
                                    projectId = projectId,
                                    messageId = messageId,
                                    updates = mapOf(
                                        "file" to FileMetadata(
                                            loading = null,
                                            fileId = fileKey,
                                            uploaded = uploadedTime,
                                            fileUrl = fileUrl,
                                            fileName = attachmentFile.name,
                                            size = attachmentFile.size,
                                            mimeType = attachmentFile.mimeType,
                                        )
                                    )
                                ).getOrThrow()
                            }
                        )
                    }
                }

                if (attachmentImages.isNotEmpty()) {
                    val messageId = messageIdDeferred.await().toString()
                    val mediaUpdateTasks = attachmentImages.mapIndexed { index, attachment ->
                        async {
                            val mediaKey = mediaMap.keys.elementAt(index)
                            val bitmap = attachment.bitmap
                            require(bitmap != null) { "Bitmap can not be null" }
                            val (imageUrl, metadata) = projectRepository.uploadProjectMediaJpeg(
                                bitmap,
                                FilePathUtil.generateFilePath(
                                    FilePathUtil.FileType.PROJECT_CHAT_MEDIA_ATTACHMENT_UPLOAD,
                                    projectId = projectId,
                                    extension = JPG_EXTENSION
                                ),
                                IMAGE_MEDIUM_QUALITY
                            ).getOrThrow()

                            val uploadedTime = System.currentTimeMillis()
                            val fileSize = metadata.sizeBytes
                            val mimeType = metadata.contentType

                            awaitAll(
                                async {
                                    projectRepository.updateProjectMedia(
                                        projectId,
                                        mediaKey,
                                        mapOf(
                                            "tempMediaUrl" to null,
                                            "loading" to null,
                                            "size" to fileSize,
                                            "mimeType" to mimeType,
                                            "mediaUrl" to imageUrl,
                                            "uploaded" to uploadedTime,
                                            "projectMessageId" to messageId
                                        )
                                    ).getOrThrow()
                                },
                                async {
                                    projectRepository.updateProjectMessageMedia(
                                        projectId,
                                        messageId,
                                        mediaKey,
                                        mapOf(
                                            "tempMediaUrl" to null,
                                            "loading" to null,
                                            "mimeType" to mimeType,
                                            "mediaUrl" to imageUrl,
                                            "uploaded" to uploadedTime
                                        )
                                    ).getOrThrow()
                                }
                            )
                        }
                    }
                    mediaUpdateTasks.awaitAll()
                }

                val messageToTeam =
                    (if (message.isEmpty()) notificationMessage else message).toString()

                async {
                    projectRepository.updateUserProject(
                        userId = senderId,
                        projectId = projectId,
                        updates = mapOf(
                            "lastMessage" to "$senderName: $messageToTeam",
                            "senderId" to senderId,
                            "timestamp" to currentTime,
                            "lastSeenTimestamp" to currentTime,
                        )
                    ).getOrThrow()
                }

                val filteredUsers = project?.members?.filter {
                    it.value && it.key != senderId
                }?.keys.orEmpty()

                if (filteredUsers.isNotEmpty()) {
                    launch(Dispatchers.IO) {
                        messagingRepository.sendNotification(
                            users = filteredUsers,
                            notificationType = PROJECT_MESSAGES,
                            title = project?.name.toString(),
                            body = messageToTeam,
                            senderId = senderId,
                            senderName = senderName,
                            senderImage = senderImageUrl,
                            channelId = PROJECT_MESSAGES_CHANNEL_ID,
                            projectId = projectId,
                            timestamp = currentTime
                        ).onFailure {
                            Log.e(
                                TAG,
                                "Failed to send notification after retries: ${it.message}"
                            )
                        }
                    }
                }

                _projectMainScreenUiState.update { it.copy(saveMessageSuccess = true) }
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isSaveMessage = false) }
                saveMessageJob = null
            }
        }
    }

    fun saveDraftChatInput(draft: String?) {
        if (saveDraftChatInputJob?.isActive == true) return

        saveDraftChatInputJob = viewModelScope.launch {
            try {
                _projectMainScreenUiState.update { it.copy(isSaveDraftChatInput = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId = requireNotNull(projectId.value) { ERROR_PROJECT_NOT_FOUND }
                projectRepository.updateUserProject(
                    userId = userId,
                    projectId = projectId,
                    updates = mapOf(
                        "draft" to if (draft.isNullOrEmpty()) null
                        else draft
                    )
                ).getOrThrow()
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isSaveDraftChatInput = false) }
                saveDraftChatInputJob = null
            }
        }
    }

    fun updateProjectTyping(typingState: Boolean) {
        if (updateProjectTypingJob?.isActive == true) return

        updateProjectTypingJob = viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val userName = authRepository.getCurrentUserDisplayName()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId = requireNotNull(projectId.value) { ERROR_PROJECT_NOT_FOUND }
                projectRepository.updateProjectTyping(
                    projectId = projectId,
                    updates = mapOf(userId to if (typingState == true) userName else null)
                ).getOrThrow()
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                updateProjectTypingJob = null
            }
        }
    }

    fun saveChatScrollPosition(scrollPosition: Int) {
        if (saveChatScrollPositionJob?.isActive == true) return

        saveChatScrollPositionJob = viewModelScope.launch {
            try {
                _projectMainScreenUiState.update { it.copy(isSaveChatScrollPosition = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId = requireNotNull(projectId.value) { ERROR_PROJECT_NOT_FOUND }
                projectRepository.updateUserProject(
                    userId = userId,
                    projectId = projectId,
                    updates = mapOf("chatPosition" to scrollPosition)
                ).getOrThrow()
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isSaveChatScrollPosition = false) }
                saveChatScrollPositionJob = null
            }
        }
    }

    fun updateLastSeenTimestamp() {
        if (updateLastSeenTimestampJob?.isActive == true) return
        updateLastSeenTimestampJob = viewModelScope.launch {
            try {
                if (_projectMainScreenUiState.value.isAtBottom == true) {
                    _projectMainScreenUiState.update { it.copy(isUpdateLastSeenTimestamp = true) }
                    val userId = authRepository.getCurrentUserId()
                    require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                    val projectId = requireNotNull(projectId.value) { ERROR_PROJECT_NOT_FOUND }

                    val lastProjectMessageDeferred =
                        async { projectRepository.getLastProjectMessage(projectId) }
                    val userProjectDeferred =
                        async { projectRepository.getUserProject(userId, projectId) }

                    val lastProjectMessage = lastProjectMessageDeferred.await().getOrNull()
                    val userProject = userProjectDeferred.await().getOrThrow()

                    val messagesToUpdate =
                        projectRepository.getNotSeenProjectMessages(projectId, userId)
                            .getOrThrow()

                    messagesToUpdate.map { message ->
                        async {
                            message.id?.let { messageId ->
                                awaitAll(
                                    async {
                                        projectRepository.updateProjectMessage(
                                            projectId,
                                            messageId,
                                            mapOf("seen" to true)
                                        ).getOrThrow()
                                    },
                                    async {
                                        projectRepository.saveMessageSeenBy(
                                            projectId,
                                            messageId,
                                            userId
                                        ).getOrThrow()
                                    }
                                )
                            }
                        }
                    }.awaitAll()

                    val lastProjectMessageTimestamp = lastProjectMessage?.timestamp
                    val userProjectTimestamp = userProject.timestamp

                    val lastSeenTimestamp = when {
                        lastProjectMessageTimestamp != null && userProjectTimestamp != null ->
                            maxOf(lastProjectMessageTimestamp, userProjectTimestamp)

                        else -> userProjectTimestamp ?: System.currentTimeMillis()
                    }

                    projectRepository.updateUserProject(
                        userId = userId,
                        projectId = projectId,
                        updates = mapOf(
                            "lastSeenTimestamp" to lastSeenTimestamp,
                            "unreadMessagesCount" to null
                        )
                    ).getOrThrow()
                }
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isUpdateLastSeenTimestamp = false) }
                updateLastSeenTimestampJob = null
            }
        }
    }

    fun handleProjectIdeaOptionSelection(option: BottomSheetOption, idea: ProjectIdea) {
        _projectMainScreenUiState.update {
            it.copy(
                selectedProjectIdea = idea,
                currentOptionTag = option.tag
            )
        }
        when (option.tag) {
            UNLIKE_IDEA -> toggleIdeaVoteState(idea)
            LIKE_IDEA -> toggleIdeaVoteState(idea)
            EDIT_IDEA -> {
                // TODO: EditIdea
            }

            DELETE_IDEA -> deleteIdea()
        }
    }

    fun handleProjectTaskOptionSelection(option: BottomSheetOption, task: ProjectTask) {
        _projectMainScreenUiState.update {
            it.copy(
                selectedProjectTask = task,
                currentOptionTag = option.tag
            )
        }
        when (option.tag) {
            COMPLETED_TASK -> toggleTaskState(task, true)
            UNCOMPLETED_TASK -> toggleTaskState(task, false)
            EDIT_TASK -> {
                // TODO: EditTask
            }

            DELETE_TASK -> deleteTask()
        }
    }

    fun handleProjectMessageOptionSelection(option: BottomSheetOption, message: ProjectMessage) {
        _projectMainScreenUiState.update {
            it.copy(
                selectedProjectMessage = message,
                currentOptionTag = option.tag
            )
        }
        when (option.tag) {
            EDIT_MESSAGE -> {
                resetMessageStates()
                editMessage()
                message.media?.values?.forEach { media ->
                    addAttachmentImageBitmap(url = media.mediaUrl, mimeType = media.mimeType)
                }
                message.file?.let { file ->
                    addAttachmentFile(
                        fileName = file.fileName,
                        fileSize = file.size,
                        mimeType = file.mimeType
                    )
                }
            }

            COPY_MESSAGE -> copyMessage()
            ADD_TASK -> addTask()
            ADD_IDEA -> addIdea()
            REPLY_MESSAGE -> {
                resetMessageStates()
                replyMessage()
            }

            UNPIN_MESSAGE -> unpinMessage()
            PIN_MESSAGE -> pinMessage()
            SHOW_VIEW_COUNT -> getMessageViewCount()
            DELETE_MESSAGE -> {
                resetMessageStates()
                deleteMessage()
            }
        }
    }

    private fun getMessageViewCount() {
        if (getMessageViewCountJob?.isActive == true) return

        getMessageViewCountJob = viewModelScope.launch {
            try {
                _projectMainScreenUiState.update { it.copy(isGetMessageViewCount = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId = requireNotNull(projectId.value) { "Project ID cannot be null" }
                val messageId =
                    requireNotNull(_projectMainScreenUiState.value.selectedProjectMessage?.id)
                    { "Message ID cannot be null" }
                val messageViewCount =
                    projectRepository.getProjectMessageSeenByCount(projectId, messageId)
                        .getOrThrow()
                _projectMainScreenUiState.update {
                    it.copy(
                        showMessageViewCountDialog = true,
                        selectedMessageViewCount = messageViewCount
                    )
                }
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isGetMessageViewCount = false) }
                getMessageViewCountJob = null
            }
        }
    }

    fun updateMessage(
        message: String,
        lastMessageAlbum: String,
        lastMessageFile: String,
        lastMessageImage: String
    ) {
        if (editMessageJob?.isActive == true) return

        editMessageJob = viewModelScope.launch {
            try {
                _projectMainScreenUiState.update {
                    it.copy(
                        isMessageUpdate = true,
                        isMessageEditing = false
                    )
                }

                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val project = projectDetailsUiState.value.project
                val projectId = requireNotNull(projectId.value) { "Project ID cannot be null" }
                val messageId =
                    requireNotNull(_projectMainScreenUiState.value.selectedProjectMessage?.id)
                    { "Message ID cannot be null" }
                val senderId =
                    requireNotNull(_projectMainScreenUiState.value.selectedProjectMessage?.senderId)
                    { "Sender ID cannot be null" }
                val senderName =
                    requireNotNull(_projectMainScreenUiState.value.selectedProjectMessage?.senderName)
                    { "Sender Name cannot be null" }
                val selectedMedia = _projectMainScreenUiState.value.selectedProjectMessage?.media

                if (!selectedMedia.isNullOrEmpty()) {
                    val existingUrls =
                        _projectMainScreenUiState.value.attachmentImages.mapNotNull { it.url }
                            .toSet()
                    _projectMainScreenUiState.update { it.copy(attachmentImages = emptyList()) }
                    if (!(existingUrls.size == selectedMedia.size && existingUrls.containsAll(
                            selectedMedia.values.mapNotNull { it.mediaUrl }))
                    ) {
                        val newMediaItems =
                            selectedMedia.filter { it.value.mediaUrl !in existingUrls }
                        val mediaTasks = newMediaItems.map { (key, mediaMetadata) ->
                            async {
                                awaitAll(
                                    async {
                                        projectRepository.deleteProjectMedia(projectId, key)
                                            .getOrThrow()
                                    },
                                    async {
                                        projectRepository.deleteProjectMessageMedia(
                                            projectId,
                                            messageId,
                                            key
                                        )
                                    },
                                    async {
                                        mediaMetadata.mediaUrl?.let { mediaUrl ->
                                            projectRepository.deleteProjectStorageFile(mediaUrl)
                                                .getOrThrow()
                                        }
                                    }
                                )
                            }
                        }
                        mediaTasks.awaitAll()
                    }
                }

                val selectedFile = _projectMainScreenUiState.value.selectedProjectMessage?.file
                val attachmentFile = _projectMainScreenUiState.value.attachmentFile

                if (selectedFile != null) {
                    _projectMainScreenUiState.update { it.copy(attachmentFile = null) }
                    if (attachmentFile == null) {
                        awaitAll(
                            async {
                                selectedFile.fileId?.let {
                                    projectRepository.deleteProjectFile(projectId, it).getOrThrow()
                                }
                            },
                            async {
                                selectedFile.fileUrl?.let {
                                    projectRepository.deleteProjectStorageFile(it).getOrThrow()
                                }
                            }
                        )
                    }
                }

                projectRepository.updateProjectMessage(
                    projectId = projectId,
                    messageId = messageId,
                    updates = mapOf(
                        "content" to if (message.isEmpty()) null else message,
                        "file" to if (attachmentFile == null) null else selectedFile,
                        "edited" to true
                    )
                ).getOrThrow()

                val filteredUsers = project?.members?.filter { it.value }?.keys.orEmpty()

                val lastMessageContent = when {
                    message.isNotEmpty() -> message
                    attachmentFile != null -> lastMessageFile
                    !selectedMedia.isNullOrEmpty() -> {
                        val mediaCount = selectedMedia.size
                        if (mediaCount == 1) lastMessageImage
                        else lastMessageAlbum
                    }

                    else -> ""
                }

                launch(Dispatchers.IO) {
                    dataManagementRepository.updateProjectMessage(
                        senderId,
                        senderName,
                        projectId,
                        messageId,
                        lastMessageContent,
                        if (message.isEmpty()) null else message,
                        filteredUsers
                    )
                        .onFailure { Log.e(TAG, "Failed to update after retries: ${it.message}") }
                }

            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isMessageUpdate = false) }
                editMessageJob = null
            }
        }
    }

    private fun deleteIdea() {
        _projectMainScreenUiState.update { it.copy(showConfirmDeleteIdeaDialog = true) }
    }

    fun confirmDeleteIdea() {
        if (deleteIdeaJob?.isActive == true) return

        deleteIdeaJob = viewModelScope.launch {
            try {
                _projectMainScreenUiState.update { it.copy(isIdeaDelete = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId = requireNotNull(projectId.value) { "Project ID cannot be null" }
                val idea = _projectMainScreenUiState.value.selectedProjectIdea
                    ?: throw IllegalArgumentException("Selected idea cannot be null")
                val ideaId = requireNotNull(idea.id) { "Message ID cannot be null" }
                async {
                    projectRepository.deleteProjectIdea(
                        projectId = projectId,
                        ideaId = ideaId
                    ).getOrThrow()
                }
                _projectMainScreenUiState.update { it.copy(selectedProjectIdea = null) }
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isIdeaDelete = false) }
                deleteIdeaJob = null
            }
        }
    }

    private fun deleteTask() {
        _projectMainScreenUiState.update { it.copy(showConfirmDeleteTaskDialog = true) }
    }

    fun confirmDeleteTask() {
        if (deleteTaskJob?.isActive == true) return

        deleteTaskJob = viewModelScope.launch {
            try {
                _projectMainScreenUiState.update { it.copy(isTaskDelete = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId = requireNotNull(projectId.value) { "Project ID cannot be null" }
                val task = _projectMainScreenUiState.value.selectedProjectTask
                    ?: throw IllegalArgumentException("Selected task cannot be null")
                val taskId = requireNotNull(task.id) { "Message ID cannot be null" }
                async {
                    projectRepository.deleteProjectTask(
                        projectId = projectId,
                        taskId = taskId
                    ).getOrThrow()
                }
                task.assignedUser?.assignedId?.let {
                    async {
                        userRepository.deleteUserTask(it, taskId).getOrThrow()
                    }
                }
                _projectMainScreenUiState.update { it.copy(selectedProjectTask = null) }
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isTaskDelete = false) }
                deleteTaskJob = null
            }
        }
    }

    private fun deleteMessage() {
        _projectMainScreenUiState.update { it.copy(showConfirmDeleteMessageDialog = true) }
    }

    fun confirmDeleteMessage(
        lastMessageAlbum: String,
        lastMessageFile: String,
        lastMessageImage: String
    ) {
        if (deleteMessageJob?.isActive == true) return

        deleteMessageJob = viewModelScope.launch {
            try {
                _projectMainScreenUiState.update { it.copy(isMessageDelete = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val project = projectDetailsUiState.value.project
                val projectId = requireNotNull(projectId.value) { "Project ID cannot be null" }
                val message = _projectMainScreenUiState.value.selectedProjectMessage
                    ?: throw IllegalArgumentException("Selected message cannot be null")
                val messageId = requireNotNull(message.id) { "Message ID cannot be null" }
                val mediaTasks = message.media?.map { (key, mediaMetadata) ->
                    async {
                        projectRepository.deleteProjectMedia(projectId, key).getOrThrow()
                        mediaMetadata.mediaUrl?.let { mediaUrl ->
                            projectRepository.deleteProjectStorageFile(mediaUrl).getOrThrow()
                        }
                    }
                }
                mediaTasks?.awaitAll()

                message.file?.let { file ->
                    awaitAll(
                        async {
                            file.fileId?.let {
                                projectRepository.deleteProjectFile(projectId, it).getOrThrow()
                            }
                        },
                        async {
                            file.fileUrl?.let {
                                projectRepository.deleteProjectStorageFile(it).getOrThrow()
                            }
                        }
                    )
                }

                awaitAll(
                    async {
                        projectRepository.deleteProjectMessage(
                            projectId = projectId,
                            messageId = messageId
                        ).getOrThrow()
                    },
                    async {
                        projectRepository.deleteProjectMessageSeenBy(
                            projectId = projectId,
                            messageId = messageId
                        ).getOrThrow()
                    }
                )

                async {
                    val messagesWithReply =
                        projectRepository.getProjectMessagesWithReply(projectId, messageId)
                            .getOrThrow()
                    if (messagesWithReply.isNotEmpty()) {
                        messagesWithReply.map { messageWithReply ->
                            async {
                                messageWithReply.id?.let {
                                    projectRepository.updateProjectMessage(
                                        projectId,
                                        it,
                                        updates = mapOf("replyTo" to null)
                                    ).getOrThrow()
                                }
                            }
                        }.awaitAll()
                    }
                }.await()

                val lastMessage = async {
                    projectRepository.getLastProjectMessage(projectId).getOrThrow()
                }.await()

                val messageContent = when {
                    !lastMessage?.content.isNullOrEmpty() -> lastMessage.content
                    lastMessage?.file != null -> lastMessageFile
                    !lastMessage?.media.isNullOrEmpty() -> {
                        val mediaCount = lastMessage.media.size
                        if (mediaCount == 1) lastMessageImage
                        else lastMessageAlbum
                    }

                    else -> ""
                }

                val userUpdateTasks = project?.members?.keys?.map { userId ->
                    async {
                        projectRepository.updateUserProject(
                            userId, projectId, updates = mapOf(
                                "lastMessage" to if (lastMessage == null) null
                                else "${lastMessage.senderName}: $messageContent",
                                "senderId" to lastMessage?.senderId,
                                "timestamp" to if (lastMessage == null) project.created
                                else lastMessage.timestamp
                            )
                        ).getOrThrow()
                    }
                }
                userUpdateTasks?.awaitAll()
                _projectMainScreenUiState.update { it.copy(selectedProjectMessage = null) }
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isMessageDelete = false) }
                deleteMessageJob = null
            }
        }
    }

    private fun pinMessage() {
        if (pinMessageJob?.isActive == true) return

        pinMessageJob = viewModelScope.launch {
            try {
                val pinnedMessagesCount = projectMessagesUiState.value.pinnedMessages.count()
                require(pinnedMessagesCount < 5) { ERROR_PIN_MESSAGE_LIMIT_EXCEEDED }
                _projectMainScreenUiState.update { it.copy(isPinMessage = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId = requireNotNull(projectId.value) { "Project ID cannot be null" }
                val messageId =
                    requireNotNull(_projectMainScreenUiState.value.selectedProjectMessage?.id) { "Message ID cannot be null" }
                projectRepository.updateProjectMessage(
                    projectId = projectId,
                    messageId = messageId,
                    updates = mapOf("pinned" to true)
                ).getOrThrow()
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isPinMessage = false) }
                pinMessageJob = null
            }
        }
    }

    fun unpinMessage() {
        if (unpinMessageJob?.isActive == true) return

        unpinMessageJob = viewModelScope.launch {
            try {
                _projectMainScreenUiState.update { it.copy(isUnpinMessage = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId = requireNotNull(projectId.value) { "Project ID cannot be null" }
                val messageId =
                    requireNotNull(_projectMainScreenUiState.value.selectedProjectMessage?.id)
                    { "Message ID cannot be null" }
                projectRepository.updateProjectMessage(
                    projectId = projectId,
                    messageId = messageId,
                    updates = mapOf("pinned" to null)
                ).getOrThrow()
            } catch (e: Exception) {
                _projectMainScreenUiState.update { it.copy(error = e) }
            } finally {
                _projectMainScreenUiState.update { it.copy(isUnpinMessage = false) }
                unpinMessageJob = null
            }
        }
    }

    private fun replyMessage() {
        _projectMainScreenUiState.update {
            it.copy(
                isReplaying = true,
                setupDataToReplyMessage = true
            )
        }
    }

    fun replyFormShown() {
        _projectMainScreenUiState.update { it.copy(setupDataToReplyMessage = false) }
    }

    private fun addIdea() {
        _projectMainScreenUiState.update { it.copy(showCreateIdeaDialog = true) }
    }

    private fun addTask() {
        _projectMainScreenUiState.update { it.copy(showCreateTaskDialog = true) }
    }

    private fun copyMessage() {
        _projectMainScreenUiState.update { it.copy(isMessageCopied = true) }
    }

    fun createTaskDialogShown() {
        _projectMainScreenUiState.update { it.copy(showCreateTaskDialog = false) }
    }

    fun copyMessageDone() {
        _projectMainScreenUiState.update { it.copy(isMessageCopied = false) }
    }

    fun resetMessageStates() {
        _projectMainScreenUiState.update {
            it.copy(
                isReplaying = false,
                isMessageEditing = false,
                attachmentFile = null,
                attachmentImages = emptyList()
            )
        }
    }

    private fun editMessage() {
        _projectMainScreenUiState.update {
            it.copy(
                isMessageEditing = true,
                updateInputForEdit = true
            )
        }
    }

    fun editMessageFormShown() {
        _projectMainScreenUiState.update { it.copy(updateInputForEdit = false) }
    }

    fun confirmDeleteMessageDialogShown() {
        _projectMainScreenUiState.update { it.copy(showConfirmDeleteMessageDialog = false) }
    }

    fun confirmDeleteTaskDialogShown() {
        _projectMainScreenUiState.update { it.copy(showConfirmDeleteTaskDialog = false) }
    }

    fun setSelectedMessage(selectedProjectMessage: ProjectMessage) {
        _projectMainScreenUiState.update { it.copy(selectedProjectMessage = selectedProjectMessage) }
    }

    fun closeReplyMessage() {
        _projectMainScreenUiState.update { it.copy(isReplaying = false) }
    }

    fun setTaskDueTimestamp(timeInMillis: Long?) {
        _projectMainScreenUiState.update { it.copy(taskDueTimestamp = timeInMillis) }
    }

    fun setAssignedUser(taskAssignedUser: User?) {
        _projectMainScreenUiState.update { it.copy(taskAssignedUser = taskAssignedUser) }
    }

    fun errorMessageShown() {
        _projectMainScreenUiState.update { it.copy(error = null) }
    }

    fun isUserSelected(user: User): Boolean {
        return _projectMainScreenUiState.value.taskAssignedUser == user
    }

    fun searchMembersByEmailMessageShown() {
        _projectMainScreenUiState.update { it.copy(searchMembersByEmailSuccess = false) }
    }

    fun saveTaskSuccessMessageShown() {
        _projectMainScreenUiState.update { it.copy(saveTaskSuccess = false) }
    }

    fun getCurrentUserDisplayName(): String {
        return authRepository.getCurrentUserDisplayName().toString()
    }

    fun createIdeaDialogShown() {
        _projectMainScreenUiState.update { it.copy(showCreateIdeaDialog = false) }
    }

    fun setIdeaImageBitmap(ideaImageBitmap: Bitmap?) {
        _projectMainScreenUiState.update { it.copy(ideaImageBitmap = ideaImageBitmap) }
    }

    fun saveIdeaSuccessMessageShown() {
        _projectMainScreenUiState.update { it.copy(saveIdeaSuccess = false) }
    }

    fun addAttachmentImageBitmap(
        url: String? = null,
        uri: Uri? = null,
        imageBitmap: Bitmap? = null,
        mimeType: String?
    ) {
        _projectMainScreenUiState.update { state ->
            state.copy(
                attachmentImages = state.attachmentImages.toMutableList().apply {
                    add(
                        Attachment(
                            url = url,
                            bitmap = imageBitmap,
                            uri = uri,
                            mimeType = mimeType
                        )
                    )
                }
            )
        }
    }

    fun removeAttachmentImageSelection(attachment: Attachment) {
        _projectMainScreenUiState.update { state ->
            state.copy(
                attachmentImages = state.attachmentImages.filter { it != attachment }
            )
        }
    }

    fun updateOptionTag(currentOptionTag: String?) {
        _projectMainScreenUiState.update { it.copy(currentOptionTag = currentOptionTag) }
    }

    fun confirmDeleteIdeaDialogShown() {
        _projectMainScreenUiState.update { it.copy(showConfirmDeleteIdeaDialog = false) }
    }

    fun addAttachmentFile(
        uri: Uri? = null,
        fileSize: Long? = null,
        fileName: String? = null,
        mimeType: String?
    ) {
        _projectMainScreenUiState.update { state ->
            state.copy(
                attachmentFile = Attachment(
                    size = fileSize,
                    name = fileName,
                    uri = uri,
                    mimeType = mimeType
                )
            )
        }
    }

    fun removeAttachmentFile() {
        _projectMainScreenUiState.update { state ->
            state.copy(
                attachmentFile = null
            )
        }
    }

    fun isAtBottomStateUpdate(isAtBottom: Boolean) {
        _projectMainScreenUiState.update { it.copy(isAtBottom = isAtBottom) }
    }

    fun showGalleryDialog(mediaMetadata: MediaMetadata) {
        _projectGalleryScreenUiState.update {
            it.copy(
                mediaMetadata = mediaMetadata,
                initMediaIndex = null,
                showSystemBars = true
            )
        }
        _projectMainScreenUiState.update { it.copy(showGalleryDialogSuccess = true) }
    }

    fun galleryDialogShown() {
        _projectMainScreenUiState.update { it.copy(showGalleryDialogSuccess = false) }
    }

    fun updateSystemBars() {
        _projectGalleryScreenUiState.update { it.copy(showSystemBars = !_projectGalleryScreenUiState.value.showSystemBars) }
    }

    fun showMemberProfileDialog(userId: String) {
        _memberId.value = userId
        _projectMainScreenUiState.update { it.copy(showMemberProfileDialogSuccess = true) }
    }

    fun memberProfileDialogShown() {
        _projectMainScreenUiState.update { it.copy(showMemberProfileDialogSuccess = false) }
    }

    fun updateTypingState(isTyping: Boolean) {
        _projectMainScreenUiState.update { it.copy(isTyping = isTyping) }
    }

    fun messageViewCountDialogShown() {
        _projectMainScreenUiState.update { it.copy(showMessageViewCountDialog = false) }
    }

    fun updateChatScrollState(state: Int) {
        _projectMainScreenUiState.update { it.copy(chatScrollState = state) }
    }

    companion object {
        private const val TAG = "ProjectDetailsViewModel"
    }
}
