package com.example.kiparys.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.ProjectRepository
import com.example.kiparys.data.repository.UserRepository
import kotlinx.coroutines.flow.StateFlow


class TasksViewModelFactory(
    private val userId: StateFlow<String?>,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TasksViewModel(userId, authRepository, userRepository, projectRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}
