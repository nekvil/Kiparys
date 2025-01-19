package com.example.kiparys.ui.authregister

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.FcmTokenRepository
import com.example.kiparys.data.repository.UserRepository

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val fcmTokenRepository: FcmTokenRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository, userRepository, fcmTokenRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
