package com.example.kiparys.ui.authregister

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.DataStoreRepository
import com.example.kiparys.data.repository.UserRepository

class RegisterViewModelFactory(
    private val dataStoreRepository: DataStoreRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            return RegisterViewModel(dataStoreRepository, authRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
