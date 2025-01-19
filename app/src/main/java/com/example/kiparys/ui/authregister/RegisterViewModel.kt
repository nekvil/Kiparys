package com.example.kiparys.ui.authregister

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiparys.Constants.IMAGE_MEDIUM_QUALITY
import com.example.kiparys.Constants.IS_USER_AUTHENTICATED
import com.example.kiparys.data.model.User
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.DataStoreRepository
import com.example.kiparys.data.repository.UserRepository
import com.example.kiparys.util.FilePathUtil
import com.example.kiparys.util.ImageUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class RegisterUiState(
    val email: String? = null,
    val error: Throwable? = null,
    val isRegister: Boolean = false,
    val registerSuccess: Boolean = false
)

class RegisterViewModel(
    private val dataStoreRepository: DataStoreRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _registerUiState = MutableStateFlow(RegisterUiState())
    val registerUiState: StateFlow<RegisterUiState> = _registerUiState

    private var registerJob: Job? = null

    fun registerUser(name: String, email: String, password: String) {
        if (registerJob != null) return

        registerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                _registerUiState.update { it.copy(isRegister = true) }
                val user =
                    authRepository.createUserWithEmailAndPassword(email, password).getOrThrow()

                val userId = user.uid
                val profileImageBitmap =
                    ImageUtil.generatePlaceholderImage(name.first().uppercaseChar(), 256, 256)
                val profileImageFileName = FilePathUtil.generateFilePath(
                    FilePathUtil.FileType.USER_PROFILE_IMAGE_PLACEHOLDER,
                    userId
                )

                val profileImageUrl = userRepository.uploadProfileImage(
                    profileImageBitmap,
                    profileImageFileName,
                    IMAGE_MEDIUM_QUALITY
                ).getOrThrow()

                val userData = User(
                    firstName = name,
                    email = email,
                    profileImageUrl = profileImageUrl
                )

                userRepository.updateUserData(userId, userData.toMap()).getOrThrow()

                authRepository.updateProfile(name, Uri.parse(profileImageUrl)).getOrThrow()

                authRepository.sendEmailVerification().getOrThrow()

                userRepository.goOffline()

                dataStoreRepository.putBoolean(IS_USER_AUTHENTICATED, false)

                authRepository.signOut()

                _registerUiState.update { it.copy(registerSuccess = true, email = email) }
            } catch (e: Exception) {
                _registerUiState.update { it.copy(error = e) }
            } finally {
                _registerUiState.update { it.copy(isRegister = false) }
                val endTime = System.currentTimeMillis()
                val elapsedSeconds = (endTime - startTime)
                Log.d("registerUser", "Operation took $elapsedSeconds milliseconds")
                registerJob = null
            }
        }
    }

    fun errorMessageShown() {
        _registerUiState.update { it.copy(error = null) }
    }

    fun registerMessageShown() {
        _registerUiState.update { it.copy(registerSuccess = false) }
    }

}
