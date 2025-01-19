package com.example.kiparys.ui.projects

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiparys.Constants.CLEAR_PROJECT_HISTORY
import com.example.kiparys.Constants.DISABLE_PROJECT_NOTIFICATIONS
import com.example.kiparys.Constants.ENABLE_PROJECT_NOTIFICATIONS
import com.example.kiparys.Constants.ERROR_PIN_PROJECT_LIMIT_EXCEEDED
import com.example.kiparys.Constants.ERROR_USER_NOT_FOUND
import com.example.kiparys.Constants.IMAGE_MEDIUM_QUALITY
import com.example.kiparys.Constants.LEAVE_PROJECT
import com.example.kiparys.Constants.MARK_AS_READ
import com.example.kiparys.Constants.MARK_AS_UNREAD
import com.example.kiparys.Constants.PIN_PROJECT
import com.example.kiparys.Constants.PROJECT_INVITE
import com.example.kiparys.Constants.PROJECT_MESSAGES_CHANNEL_ID
import com.example.kiparys.Constants.UNPIN_PROJECT
import com.example.kiparys.data.model.BottomSheetOption
import com.example.kiparys.data.model.Project
import com.example.kiparys.data.model.User
import com.example.kiparys.data.model.UserProject
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.MessagingRepository
import com.example.kiparys.data.repository.ProjectRepository
import com.example.kiparys.data.repository.UserRepository
import com.example.kiparys.util.FilePathUtil
import com.example.kiparys.util.ImageUtil
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class ProjectsUiState(
    val foundUsers: List<User> = emptyList(),
    val selectedUsers: List<User> = emptyList(),

    val clearProjectSuccess: Boolean = false,
    val searchUsersByEmailSuccess: Boolean = false,
    val saveProjectSuccess: Boolean = false,
    val deleteProjectSuccess: Boolean = false,

    val isConfirmClearProject: Boolean = false,
    val isLeaveProject: Boolean = false,
    val isConfirmLeaveProject: Boolean = false,
    val isPin: Boolean = false,
    val isUnpin: Boolean = false,
    val isMarkAsUnread: Boolean = false,
    val isMarkAsRead: Boolean = false,
    val isSaveProject: Boolean = false,
    val isSearchUsersByEmail: Boolean = false,
    val isSaveSelectedUsers: Boolean = false,
    val isEnableNotifications: Boolean = false,
    val isDisableNotifications: Boolean = false,

    val saveProjectId: String? = null,
    val selectedUserProject: UserProject? = null,
    val selectedProject: Project? = null,
    val showLeaveProjectDialog: Boolean = false,
    val showTransferManagementMessage: Boolean = false,
    val showConfirmClearProjectDialog: Boolean = false,

    val error: Throwable? = null,
)

data class UserProjectsUiState(
    val userProjects: List<UserProject> = emptyList(),
    val isLoading: Boolean = false
)

class ProjectsViewModel(
    userId: StateFlow<String?>,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val messagingRepository: MessagingRepository
) : ViewModel() {

    private val _projectsUiState = MutableStateFlow(ProjectsUiState())
    val projectsUiState: StateFlow<ProjectsUiState> = _projectsUiState

    private var saveProjectJob: Job? = null
    private var markAsReadJob: Job? = null
    private var markAsUnreadJob: Job? = null
    private var pinJob: Job? = null
    private var unpinJob: Job? = null
    private var leaveProjectJob: Job? = null
    private var confirmLeaveProjectJob: Job? = null
    private var confirmClearProjectJob: Job? = null
    private var clearProjectJob: Job? = null
    private var enableNotificationsJob: Job? = null
    private var disableNotificationsJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val userProjectsUiState: StateFlow<UserProjectsUiState> = userId
        .flatMapLatest { id ->
            if (id != null) {
                userRepository.getUserProjectsFlow(id)
                    .map { result ->
                        _projectsUiState.update { it.copy(error = result.exceptionOrNull()) }
                        UserProjectsUiState(
                            userProjects = result.getOrNull() ?: emptyList(),
                            isLoading = false
                        )
                    }
            } else {
                flowOf(UserProjectsUiState(isLoading = false))
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProjectsUiState(isLoading = true)
        )


    fun saveProject(
        name: String,
        description: String?,
        messageToMembers: String,
        messageForMe: String
    ) {
        if (saveProjectJob?.isActive == true) return

        saveProjectJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                _projectsUiState.update { it.copy(isSaveProject = true) }

                val creatorId = authRepository.getCurrentUserId()
                val creatorName = authRepository.getCurrentUserDisplayName()
                val creatorImageUrl = authRepository.getCurrentUserPhotoUrl()
                require(
                    !creatorId.isNullOrEmpty()
                            && !creatorName.isNullOrEmpty()
                            && !creatorImageUrl.isNullOrEmpty()
                )
                { ERROR_USER_NOT_FOUND }

                val members = _projectsUiState.value.selectedUsers
                    .mapNotNull { it.id?.let { id -> id to true } }
                    .toMap()
                    .toMutableMap()

                members[creatorId] = true
                val managers = mapOf(creatorId to true)
                val currentTime = System.currentTimeMillis()

                val projectData = Project(
                    name = name,
                    description = description,
                    created = currentTime,
                    managers = managers,
                    members = members
                ).toMap()

                val newProjectKeyDeferred = async {
                    projectRepository.saveProject(projectData).getOrThrow()
                }

                val projectImageDeferred = async {
                    val bitmap = ImageUtil.generatePlaceholderImageWithGradient(
                        name.first().uppercaseChar(),
                        256,
                        256
                    )
                    val projectKey = newProjectKeyDeferred.await().toString()
                    val fileName = FilePathUtil.generateFilePath(
                        FilePathUtil.FileType.PROJECT_PROFILE_IMAGE_PLACEHOLDER,
                        projectId = projectKey
                    )
                    projectRepository.uploadProjectMediaJpeg(bitmap, fileName, IMAGE_MEDIUM_QUALITY)
                        .getOrThrow().first
                }

                val newProjectKey = newProjectKeyDeferred.await()
                val projectImageUrl = projectImageDeferred.await()

                async {
                    projectRepository.updateProject(
                        newProjectKey.toString(),
                        Project(projectImageUrl = projectImageUrl).toMap()
                    )
                }

                val userUpdateTasks = members.keys.map { userId ->
                    async {
                        val updateTask = async {
                            if (userId == creatorId)
                                projectRepository.updateUserProject(
                                    userId, newProjectKey.toString(),
                                    mapOf(
                                        "name" to name,
                                        "projectImageUrl" to projectImageUrl,
                                        "lastMessage" to messageForMe,
                                        "timestamp" to currentTime,
                                        "lastSeenTimestamp" to currentTime,
                                        "chatPosition" to -2
                                    )
                                ).getOrThrow()
                            else
                                projectRepository.updateUserProject(
                                    userId, newProjectKey.toString(),
                                    mapOf(
                                        "name" to name,
                                        "projectImageUrl" to projectImageUrl,
                                        "lastMessage" to "$creatorName $messageToMembers",
                                        "timestamp" to currentTime,
                                        "lastSeenTimestamp" to currentTime.minus(300_000),
                                        "chatPosition" to -2
                                    )
                                ).getOrThrow()
                        }
                        updateTask.await()
                    }
                }
                userUpdateTasks.awaitAll()

                val filteredUsers = members.filter {
                    it.value && it.key != creatorId
                }.keys

                if (filteredUsers.isNotEmpty()) {
                    launch(Dispatchers.IO) {
                        messagingRepository.sendNotification(
                            users = filteredUsers,
                            notificationType = PROJECT_INVITE,
                            title = name,
                            body = messageToMembers,
                            senderId = creatorId,
                            senderName = creatorName,
                            senderImage = creatorImageUrl,
                            channelId = PROJECT_MESSAGES_CHANNEL_ID,
                            projectId = newProjectKey,
                            timestamp = currentTime
                        ).onFailure {
                            Log.e(
                                "NotificationError",
                                "Failed to send notification after retries: ${it.message}"
                            )
                        }
                    }
                }

                _projectsUiState.update {
                    it.copy(
                        saveProjectSuccess = true,
                        saveProjectId = newProjectKey
                    )
                }
            } catch (e: Exception) {
                _projectsUiState.update { it.copy(error = e) }
            } finally {
                _projectsUiState.update { it.copy(isSaveProject = false) }
                val endTime = System.currentTimeMillis()
                val elapsedSeconds = (endTime - startTime)
                Log.d("SaveProject", "Operation took $elapsedSeconds milliseconds")
                saveProjectJob = null
            }
        }
    }

    fun searchUsersByEmail(userEmail: String) {
        viewModelScope.launch {
            try {
                _projectsUiState.update { it.copy(isSearchUsersByEmail = true) }
                val currentUserId = authRepository.getCurrentUserId() ?: return@launch
                val users = userRepository.searchUsersByEmail(userEmail, currentUserId).getOrThrow()
                _projectsUiState.update {
                    it.copy(
                        searchUsersByEmailSuccess = true,
                        foundUsers = users
                    )
                }
            } catch (e: Exception) {
                _projectsUiState.update { it.copy(error = e) }
            } finally {
                _projectsUiState.update { it.copy(isSearchUsersByEmail = false) }
            }
        }
    }

    fun toggleUserSelection(user: User) {
        val updatedSelectedUsers = _projectsUiState.value.selectedUsers.toMutableList()
        if (updatedSelectedUsers.contains(user)) {
            updatedSelectedUsers.remove(user)
        } else {
            updatedSelectedUsers.add(user)
        }
        _projectsUiState.update { it.copy(selectedUsers = updatedSelectedUsers) }
    }

    fun handleOptionSelection(option: BottomSheetOption, project: UserProject) {
        _projectsUiState.update { it.copy(selectedUserProject = project) }
        when (option.tag) {
            MARK_AS_READ -> markAsRead()
            MARK_AS_UNREAD -> markAsUnread()
            ENABLE_PROJECT_NOTIFICATIONS -> enableNotifications()
            DISABLE_PROJECT_NOTIFICATIONS -> disableNotifications()
            LEAVE_PROJECT -> leaveProject()
            CLEAR_PROJECT_HISTORY -> clearProject()
            PIN_PROJECT -> pin()
            UNPIN_PROJECT -> unpin()
        }
    }

    fun enableNotifications() {
        if (enableNotificationsJob?.isActive == true) return

        enableNotificationsJob = viewModelScope.launch {
            try {
                _projectsUiState.update { it.copy(isEnableNotifications = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId =
                    requireNotNull(_projectsUiState.value.selectedUserProject?.id)
                    { "Project ID cannot be null" }
                projectRepository.updateUserProject(
                    userId = userId,
                    projectId = projectId,
                    updates = mapOf("notifications" to null)
                ).getOrThrow()
            } catch (e: Exception) {
                _projectsUiState.update { it.copy(error = e) }
            } finally {
                _projectsUiState.update { it.copy(isEnableNotifications = false) }
                enableNotificationsJob = null
            }
        }
    }

    fun disableNotifications() {
        if (disableNotificationsJob?.isActive == true) return

        disableNotificationsJob = viewModelScope.launch {
            try {
                _projectsUiState.update { it.copy(isDisableNotifications = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId =
                    requireNotNull(_projectsUiState.value.selectedUserProject?.id)
                    { "Project ID cannot be null" }
                projectRepository.updateUserProject(
                    userId = userId,
                    projectId = projectId,
                    updates = mapOf("notifications" to false)
                ).getOrThrow()
            } catch (e: Exception) {
                _projectsUiState.update { it.copy(error = e) }
            } finally {
                _projectsUiState.update { it.copy(isDisableNotifications = false) }
                disableNotificationsJob = null
            }
        }
    }

    fun pin() {
        if (pinJob?.isActive == true) return

        pinJob = viewModelScope.launch {
            try {
                val pinnedProjectsCount =
                    userProjectsUiState.value.userProjects.count { it.pinned == true }
                require(pinnedProjectsCount < 5) { ERROR_PIN_PROJECT_LIMIT_EXCEEDED }
                _projectsUiState.update { it.copy(isPin = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId =
                    requireNotNull(_projectsUiState.value.selectedUserProject?.id)
                    { "Project ID cannot be null" }
                projectRepository.updateUserProject(
                    userId = userId,
                    projectId = projectId,
                    updates = mapOf("pinned" to true)
                ).getOrThrow()
            } catch (e: Exception) {
                _projectsUiState.update { it.copy(error = e) }
            } finally {
                _projectsUiState.update { it.copy(isPin = false) }
                pinJob = null
            }
        }
    }

    fun unpin() {
        if (unpinJob?.isActive == true) return

        unpinJob = viewModelScope.launch {
            try {
                _projectsUiState.update { it.copy(isUnpin = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId =
                    requireNotNull(_projectsUiState.value.selectedUserProject?.id)
                    { "Project ID cannot be null" }
                projectRepository.updateUserProject(
                    userId = userId,
                    projectId = projectId,
                    updates = mapOf("pinned" to null)
                ).getOrThrow()
            } catch (e: Exception) {
                _projectsUiState.update { it.copy(error = e) }
            } finally {
                _projectsUiState.update { it.copy(isUnpin = false) }
                unpinJob = null
            }
        }
    }

    fun markAsRead() {
        if (markAsReadJob?.isActive == true) return

        markAsReadJob = viewModelScope.launch {
            try {
                _projectsUiState.update { it.copy(isMarkAsRead = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { "User ID cannot be null or empty" }
                val projectId =
                    requireNotNull(_projectsUiState.value.selectedUserProject?.id)
                    { "Project ID cannot be null" }
                projectRepository.updateUserProject(
                    userId = userId,
                    projectId = projectId,
                    updates = mapOf(
                        "lastSeenTimestamp" to _projectsUiState.value.selectedUserProject?.timestamp,
                        "unreadMessagesCount" to null
                    )
                ).getOrThrow()
            } catch (e: Exception) {
                _projectsUiState.update { it.copy(error = e) }
            } finally {
                _projectsUiState.update { it.copy(isMarkAsRead = false) }
                markAsReadJob = null
            }
        }
    }

    fun markAsUnread() {
        if (markAsUnreadJob?.isActive == true) return

        markAsUnreadJob = viewModelScope.launch {
            try {
                _projectsUiState.update { it.copy(isMarkAsUnread = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId =
                    requireNotNull(_projectsUiState.value.selectedUserProject?.id)
                    { "Project ID cannot be null" }
                val adjustedTimestamp =
                    _projectsUiState.value.selectedUserProject?.timestamp?.minus(1)
                projectRepository.updateUserProject(
                    userId = userId,
                    projectId = projectId,
                    updates = mapOf("lastSeenTimestamp" to adjustedTimestamp)
                ).getOrThrow()
            } catch (e: Exception) {
                _projectsUiState.update { it.copy(error = e) }
            } finally {
                _projectsUiState.update { it.copy(isMarkAsUnread = false) }
                markAsUnreadJob = null
            }
        }
    }

    private fun clearProject() {
        if (clearProjectJob?.isActive == true) return

        clearProjectJob = viewModelScope.launch {
            try {
                _projectsUiState.update { it.copy(isLeaveProject = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId =
                    requireNotNull(_projectsUiState.value.selectedUserProject?.id) { "Project ID cannot be null" }
                val projectData = projectRepository.getProject(projectId).getOrThrow()
                requireNotNull(projectData) { "Project data cannot be null" }
                _projectsUiState.update {
                    it.copy(
                        showConfirmClearProjectDialog = true,
                        selectedProject = projectData
                    )
                }
            } catch (e: Exception) {
                _projectsUiState.update { it.copy(error = e) }
            } finally {
                _projectsUiState.update { it.copy(isLeaveProject = false) }
                clearProjectJob = null
            }
        }
    }

    fun confirmClearProject(message: String) {
        if (confirmClearProjectJob?.isActive == true) return

        confirmClearProjectJob = viewModelScope.launch {
            try {
                _projectsUiState.update { it.copy(isConfirmClearProject = false) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val userName = authRepository.getCurrentUserDisplayName()
                val projectId =
                    requireNotNull(_projectsUiState.value.selectedUserProject?.id)
                    { "Project ID cannot be null" }
                val projectData = _projectsUiState.value.selectedProject
                requireNotNull(projectData) { "Project data cannot be null" }
                val currentTime = System.currentTimeMillis()

                val tasks = mutableListOf<Deferred<Unit>>()
                tasks.add(async {
                    projectRepository.deleteProjectStorageAttachmentsChat(projectId).getOrThrow()
                })
                tasks.add(async {
                    projectRepository.deleteProjectMessagesSeenBy(projectId).getOrThrow()
                })
                tasks.add(async { projectRepository.deleteProjectMessages(projectId).getOrThrow() })
                tasks.add(async { projectRepository.deleteAllProjectMedia(projectId).getOrThrow() })
                tasks.add(async { projectRepository.deleteAllProjectFile(projectId).getOrThrow() })
                projectData.members?.let { members ->
                    members.keys.forEach { memberId ->
                        tasks.add(async {
                            projectRepository.updateUserProject(
                                memberId,
                                projectId,
                                updates = mapOf(
                                    "lastMessage" to "$userName $message",
                                    "unreadMessagesCount" to null,
                                    "timestamp" to currentTime
                                )
                            ).getOrThrow()
                        })
                    }
                }
                tasks.awaitAll()
                _projectsUiState.update { it.copy(clearProjectSuccess = true) }
            } catch (e: Exception) {
                _projectsUiState.update { it.copy(error = e) }
            } finally {
                _projectsUiState.update { it.copy(isConfirmClearProject = false) }
                confirmClearProjectJob = null
            }
        }
    }

    fun leaveProject() {
        if (leaveProjectJob?.isActive == true) return

        leaveProjectJob = viewModelScope.launch {
            try {
                _projectsUiState.update { it.copy(isLeaveProject = true) }

                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId =
                    requireNotNull(_projectsUiState.value.selectedUserProject?.id)
                    { "Project ID cannot be null" }
                val projectData = projectRepository.getProject(projectId).getOrThrow()
                requireNotNull(projectData) { "Project data cannot be null" }

                _projectsUiState.update { it.copy(selectedProject = projectData) }

                val managers = projectData.managers?.filterValues { it } ?: emptyMap()
                val isUserManager = managers.containsKey(userId)

                _projectsUiState.update {
                    it.copy(
                        showLeaveProjectDialog = true,
                        showTransferManagementMessage = isUserManager && managers.size == 1
                    )
                }
            } catch (e: Exception) {
                _projectsUiState.update { it.copy(error = e) }
            } finally {
                _projectsUiState.update { it.copy(isLeaveProject = false) }
                leaveProjectJob = null
            }
        }
    }

    fun confirmLeaveProject() {
        if (confirmLeaveProjectJob?.isActive == true) return

        confirmLeaveProjectJob = viewModelScope.launch {
            try {
                _projectsUiState.update { it.copy(isConfirmLeaveProject = false) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId =
                    requireNotNull(_projectsUiState.value.selectedUserProject?.id)
                    { "Project ID cannot be null" }
                val projectData = _projectsUiState.value.selectedProject
                requireNotNull(projectData) { "Project data cannot be null" }

                val managers = projectData.managers?.filterValues { it } ?: emptyMap()
                val isUserManager = managers.containsKey(userId)

                if (isUserManager && managers.size == 1) {
                    val tasks = mutableListOf<Deferred<Unit>>()
                    tasks.add(async {
                        projectRepository.deleteProjectStorage(projectId).getOrThrow()
                    })
                    tasks.add(async {
                        projectRepository.deleteProjectMessages(projectId).getOrThrow()
                    })
                    tasks.add(async {
                        projectRepository.deleteProjectIdeas(projectId).getOrThrow()
                    })
                    tasks.add(async {
                        projectRepository.deleteProjectTasks(projectId).getOrThrow()
                    })
                    tasks.add(async {
                        projectRepository.deleteAllProjectMedia(projectId).getOrThrow()
                    })
                    projectData.members?.let { members ->
                        members.keys.forEach { memberId ->
                            tasks.add(async {
                                projectRepository.deleteUserProject(memberId, projectId)
                                    .getOrThrow()
                                val tasksResult =
                                    projectRepository.getUserProjectTasks(memberId, projectId)
                                val userTasks = tasksResult.getOrNull() ?: emptyList()
                                userTasks.forEach { taskId ->
                                    userRepository.deleteUserTask(memberId, taskId).getOrThrow()
                                }
                            })
                        }
                    }
                    tasks.awaitAll()
                    projectRepository.deleteProject(projectId).getOrThrow()
                    _projectsUiState.update { it.copy(deleteProjectSuccess = true) }
                } else {
                    val updates = mutableMapOf<String, Any?>(
                        "members/$userId" to null
                    )
                    if (isUserManager) {
                        updates["managers/$userId"] = null
                    }
                    coroutineScope {
                        val tasks = mutableListOf<Deferred<Unit>>()
                        tasks.add(async {
                            projectRepository.deleteUserProject(userId, projectId).getOrThrow()
                        })
                        val tasksResult = projectRepository.getUserProjectTasks(userId, projectId)
                        val userTasks = tasksResult.getOrNull() ?: emptyList()
                        userTasks.forEach { taskId ->
                            tasks.add(async {
                                userRepository.deleteUserTask(userId, taskId).getOrThrow()
                            })
                            tasks.add(async {
                                projectRepository.updateProjectTask(
                                    projectId,
                                    taskId,
                                    updates = mapOf("assignedUser" to null)
                                ).getOrThrow()
                            })
                        }
                        tasks.awaitAll()
                    }
                    projectRepository.updateProject(projectId, updates).getOrThrow()
                }
            } catch (e: Exception) {
                _projectsUiState.update { it.copy(error = e) }
            } finally {
                _projectsUiState.update { it.copy(isConfirmLeaveProject = false) }
                confirmLeaveProjectJob = null
            }
        }
    }

    fun errorMessageShown() {
        _projectsUiState.update { it.copy(error = null) }
    }

    fun leaveProjectDialogShown() {
        _projectsUiState.update { it.copy(showLeaveProjectDialog = false) }
    }

    fun clearSelectedUsers() {
        _projectsUiState.update {
            it.copy(
                isSaveSelectedUsers = false,
                selectedUsers = emptyList()
            )
        }
    }

    fun saveSelectedUsers() {
        _projectsUiState.update { it.copy(isSaveSelectedUsers = true) }
    }

    fun saveProjectMessageShown() {
        _projectsUiState.update { it.copy(saveProjectSuccess = false) }
    }

    fun deleteProjectMessageShown() {
        _projectsUiState.update { it.copy(deleteProjectSuccess = false) }
    }

    fun clearProjectMessageShown() {
        _projectsUiState.update { it.copy(clearProjectSuccess = false) }
    }

    fun searchUsersByEmailMessageShown() {
        _projectsUiState.update {
            it.copy(
                searchUsersByEmailSuccess = false,
                foundUsers = emptyList()
            )
        }
    }

    fun clearProjectDialogShown() {
        _projectsUiState.update { it.copy(showConfirmClearProjectDialog = false) }
    }

}
