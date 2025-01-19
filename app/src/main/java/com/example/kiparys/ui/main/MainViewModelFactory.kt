package com.example.kiparys.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.DataStoreRepository
import com.example.kiparys.data.repository.UserRepository

class MainViewModelFactory(
    private val dataStoreRepository: DataStoreRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return MainViewModel(dataStoreRepository, authRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
