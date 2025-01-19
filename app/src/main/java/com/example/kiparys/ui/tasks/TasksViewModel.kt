package com.example.kiparys.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiparys.Constants.ERROR_USER_NOT_FOUND
import com.example.kiparys.data.model.UserTask
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.ProjectRepository
import com.example.kiparys.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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


data class TasksUiState(
    val isToggleTaskState: Boolean? = null,
    val toggleTaskStateSuccess: Boolean? = null,

    val error: Throwable? = null,
)

data class UserTasksUiState(
    val userTasks: List<UserTask> = emptyList(),
    val isLoading: Boolean = false
)

class TasksViewModel(
    userId: StateFlow<String?>,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val _tasksUiState = MutableStateFlow(TasksUiState())
    val tasksUiState: StateFlow<TasksUiState> = _tasksUiState

    private var toggleTaskStateJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val userTasksUiState: StateFlow<UserTasksUiState> = userId
        .flatMapLatest { id ->
            if (id != null) {
                userRepository.getUserTasksFlow(id)
                    .map { result ->
                        _tasksUiState.update { it.copy(error = result.exceptionOrNull()) }
                        UserTasksUiState(
                            userTasks = result.getOrNull() ?: emptyList(),
                            isLoading = false
                        )
                    }
            } else {
                flowOf(UserTasksUiState(isLoading = false))
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserTasksUiState(isLoading = true)
        )

    fun toggleTaskState(task: UserTask, doneState: Boolean) {
        if (toggleTaskStateJob?.isActive == true) return

        toggleTaskStateJob = viewModelScope.launch {
            try {
                _tasksUiState.update { it.copy(isToggleTaskState = true) }
                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { ERROR_USER_NOT_FOUND }
                val projectId = requireNotNull(task.projectId) { "Project ID cannot be null" }
                val taskId = requireNotNull(task.id) { "Task ID cannot be null" }
                val updates = mapOf("completed" to if (doneState) true else null)
                async {
                    projectRepository.updateProjectTask(
                        projectId = projectId,
                        taskId = taskId,
                        updates = updates
                    ).getOrThrow()
                }
                async {
                    userRepository.updateUserTask(
                        userId = userId,
                        taskId = taskId,
                        updates = updates
                    ).getOrThrow()
                }
                _tasksUiState.update { it.copy(toggleTaskStateSuccess = true) }
            } catch (e: Exception) {
                _tasksUiState.update { it.copy(error = e) }
            } finally {
                _tasksUiState.update { it.copy(isToggleTaskState = false) }
                toggleTaskStateJob = null
            }
        }
    }

    fun errorMessageShown() {
        _tasksUiState.update { it.copy(error = null) }
    }

}
