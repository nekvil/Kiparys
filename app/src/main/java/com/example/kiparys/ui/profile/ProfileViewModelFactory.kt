package com.example.kiparys.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.DataManagementRepository
import com.example.kiparys.data.repository.UserRepository


class ProfileViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val dataManagementRepository: DataManagementRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(authRepository, userRepository, dataManagementRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
