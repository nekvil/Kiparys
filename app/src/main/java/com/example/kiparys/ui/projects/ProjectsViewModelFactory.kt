package com.example.kiparys.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.MessagingRepository
import com.example.kiparys.data.repository.ProjectRepository
import com.example.kiparys.data.repository.UserRepository
import kotlinx.coroutines.flow.StateFlow


class ProjectsViewModelFactory(
    private val userId: StateFlow<String?>,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val messagingRepository: MessagingRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectsViewModel(
                userId,
                authRepository,
                userRepository,
                projectRepository,
                messagingRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}
