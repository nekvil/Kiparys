package com.example.kiparys.ui.projectdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.DataManagementRepository
import com.example.kiparys.data.repository.MessagingRepository
import com.example.kiparys.data.repository.ProjectRepository
import com.example.kiparys.data.repository.UserRepository
import kotlinx.coroutines.flow.StateFlow


class ProjectDetailsViewModelFactory(
    private val userId: StateFlow<String?>,
    private val projectId: StateFlow<String?>,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val projectRepository: ProjectRepository,
    private val dataManagementRepository: DataManagementRepository,
    private val messagingRepository: MessagingRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectDetailsViewModel(
                userId,
                projectId,
                authRepository,
                userRepository,
                projectRepository,
                dataManagementRepository,
                messagingRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}
