package com.example.kiparys.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiparys.Constants.IS_USER_AUTHENTICATED
import com.example.kiparys.Constants.NOTIFICATION_PERMISSION_DENIED
import com.example.kiparys.data.model.User
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.DataStoreRepository
import com.example.kiparys.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


class MainViewModel(
    private val dataStoreRepository: DataStoreRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    val projectId: StateFlow<String?> get() = _projectId
    private val _projectId = MutableStateFlow<String?>(null)

    val userId: StateFlow<String?> get() = _userId
    private val _userId = MutableStateFlow<String?>(authRepository.getCurrentUserId())

    val isUserAuthenticated: StateFlow<Boolean> = authRepository.getAuthStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2500),
            initialValue = authRepository.isUserAuthenticated()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val userProjectsUnreadCount: StateFlow<Result<Int>?> = _userId
        .flatMapLatest { id ->
            if (id != null) {
                userRepository.getUserProjectsUnreadCountFlow(id)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val userIncompleteTasksCount: StateFlow<Result<Int>?> = _userId
        .flatMapLatest { id ->
            if (id != null) {
                userRepository.getUserIncompleteTasksCountFlow(id)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val userData: StateFlow<Result<User>?> = _userId
        .flatMapLatest { id ->
            if (id != null) {
                userRepository.getUserDataFlow(id)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val userOnlineState: StateFlow<Result<Boolean>?> = _userId
        .flatMapLatest { id ->
            if (id != null) {
                userRepository.observeUserOnlineStatus(id)
                    .catch { e -> emit(Result.failure(e)) }
            } else {
                flowOf(null)
            }
        }
        .onStart { goOnline() }
        .onEach { goOnline() }
        .onCompletion { goOffline() }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private var lastProjectId: String? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val userPresenceInProjectChatState: StateFlow<Result<Boolean>?> =
        combine(_userId, _projectId) { userId, projectId ->
            val oldProjectId = lastProjectId
            if (oldProjectId != null && oldProjectId != projectId) {
                viewModelScope.launch {
                    val userId = _userId.value
                    if (userId != null) {
                        try {
                            userRepository.cleanupUserStatus(userId, oldProjectId)
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error cleaning up user status for old projectId: $oldProjectId",
                                e
                            )
                        }
                    }
                }
            }
            lastProjectId = projectId
            userId to projectId
        }
            .flatMapLatest { (userId, projectId) ->
                if (userId != null && projectId != null) {
                    userRepository.userInProjectChatStatusFlow(userId, projectId)
                        .catch { e -> emit(Result.failure(e)) }
                } else {
                    flowOf(null)
                }
            }
            .onCompletion {
                val userId = _userId.value
                val projectId = lastProjectId
                if (userId != null && projectId != null) {
                    try {
                        userRepository.cleanupUserStatus(userId, projectId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error when deleting a user from a chat room", e)
                    }
                }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(2500),
                initialValue = null
            )

    fun getCurrentUser(): Boolean {
        return authRepository.getCurrentUser() != null
    }

    fun refreshUserState() {
        val currentUserId = authRepository.getCurrentUserId()
        _userId.value = currentUserId
        if (currentUserId == null) _projectId.value = null
        viewModelScope.launch {
            dataStoreRepository.putBoolean(IS_USER_AUTHENTICATED, currentUserId != null)
        }
    }

    fun signOut() {
        val currentUserId = _userId.value
        if (currentUserId != null) {
            viewModelScope.launch {
                userRepository.goOffline()
                authRepository.signOut()
                refreshUserState()
            }
        } else {
            authRepository.signOut()
        }
    }

    fun saveNotificationPermissionDenied(denied: Boolean) {
        viewModelScope.launch {
            dataStoreRepository.putBoolean(NOTIFICATION_PERMISSION_DENIED, denied)
        }
    }

    suspend fun isNotificationPermissionDenied(): Boolean {
        return dataStoreRepository.getBoolean(NOTIFICATION_PERMISSION_DENIED).first()
    }

    suspend fun isUserAuthenticatedByDataStore(): Boolean {
        return dataStoreRepository.getBoolean(IS_USER_AUTHENTICATED).first()
    }

    fun setProjectId(projectId: String?) {
        _projectId.value = projectId
    }

    fun goOffline() {
        userRepository.goOffline()
    }

    fun goOnline() {
        userRepository.goOnline()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }

}
