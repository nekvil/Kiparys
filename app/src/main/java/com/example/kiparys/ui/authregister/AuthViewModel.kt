package com.example.kiparys.ui.authregister

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiparys.data.model.User
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.FcmTokenRepository
import com.example.kiparys.data.repository.UserRepository
import com.example.kiparys.Constants
import com.example.kiparys.Constants.ERROR_MFA_ABORTED
import com.example.kiparys.Constants.PHONE_FACTOR
import com.example.kiparys.Constants.TOTP_FACTOR
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.MultiFactorResolver
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class AuthUiState(
    val multiFactorHints: List<MultiFactorInfo> = emptyList(),
    val isMultiFactorRequired: Boolean = false,
    val isMultiFactorAborted: Boolean = false,
    val selectedFactorIndex: Int? = null,
    val selectedFactorId: String? = null,
    val showTotpDialog: Boolean = false,
    val showPhoneDialog: Boolean = false,
    val currentResolver: MultiFactorResolver? = null,

    val email: String? = null,
    val error: Throwable? = null,

    val isResetPassword: Boolean = false,
    val isSendPasswordResetEmail: Boolean = false,
    val isRecoverEmail: Boolean = false,
    val isRevertSecondFactorAddition: Boolean = false,
    val isVerifyEmail: Boolean = false,
    val isConfirmMultiFactor: Boolean = false,
    val isSignInWithEmailAndPassword: Boolean = false,

    val signInWithEmailAndPasswordSuccess: Boolean = false,
    val handleMultiFactorExceptionSuccess: Boolean = false,
    val resetPasswordSuccess: Boolean = false,
    val sendPasswordResetEmailSuccess: Boolean = false,
    val recoverEmailSuccess: Boolean = false,
    val revertSecondFactorAdditionSuccess: Boolean = false,
    val verifyEmailSuccess: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val fcmTokenRepository: FcmTokenRepository
) : ViewModel() {
    private var multiFactorContinuationJob: (suspend () -> Unit)? = null
    private var signInWithEmailAndPasswordJob: Job? = null
    private var verifyEmailJob: Job? = null
    private var sendPasswordResetEmailJob: Job? = null
    private var resetPasswordJob: Job? = null
    private var recoverEmailJob: Job? = null
    private var revertSecondFactorAdditionJob: Job? = null
    private var confirmTotpAuthenticationJob: Job? = null

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState

    private fun handleMultiFactorException(
        e: FirebaseAuthMultiFactorException,
        continuation: suspend () -> Unit
    ) {
        multiFactorContinuationJob = continuation

        _authUiState.update {
            it.copy(
                isMultiFactorAborted = false,
                isMultiFactorRequired = true,
                currentResolver = e.resolver,
                multiFactorHints = e.resolver.hints.map { hint -> hint }
            )
        }
    }

    fun abortMultiFactorAuthentication() {
        if (_authUiState.value.handleMultiFactorExceptionSuccess == true) {
            _authUiState.update {
                it.copy(
                    isMultiFactorAborted = false,
                    isMultiFactorRequired = false,
                    showTotpDialog = false,
                    showPhoneDialog = false,
                    multiFactorHints = emptyList(),
                    selectedFactorIndex = null,
                    selectedFactorId = null,
                    currentResolver = null,
                    handleMultiFactorExceptionSuccess = false
                )
            }
        } else {
            _authUiState.update {
                it.copy(
                    isMultiFactorAborted = true,
                    isMultiFactorRequired = false,
                    showTotpDialog = false,
                    showPhoneDialog = false,
                    multiFactorHints = emptyList(),
                    selectedFactorIndex = null,
                    selectedFactorId = null,
                    currentResolver = null,
                    error = Exception(ERROR_MFA_ABORTED)
                )
            }
            multiFactorContinuationJob = null
        }
    }

    fun handleFactorSelection(selectedIndex: Int) {
        if (_authUiState.value.isMultiFactorAborted) {
            return
        }

        val selectedFactor = _authUiState.value.multiFactorHints.getOrNull(selectedIndex)
        _authUiState.update {
            it.copy(
                isMultiFactorRequired = false,
                selectedFactorIndex = selectedIndex,
                selectedFactorId = selectedFactor?.factorId
            )
        }

        when (selectedFactor?.factorId) {
            TOTP_FACTOR -> _authUiState.update { it.copy(showTotpDialog = true) }
            PHONE_FACTOR -> _authUiState.update { it.copy(showPhoneDialog = true) }
            else -> {}
        }
    }

    fun confirmTotpAuthentication(code: String) {
        if (_authUiState.value.isMultiFactorAborted) {
            return
        }

        if (confirmTotpAuthenticationJob?.isActive == true) return

        confirmTotpAuthenticationJob = viewModelScope.launch {
            val resolver = _authUiState.value.currentResolver ?: return@launch
            val selectedFactorId = _authUiState.value.selectedFactorId ?: return@launch

            try {
                _authUiState.update { it.copy(isConfirmMultiFactor = true) }
                authRepository.verifyMultiFactor(resolver, selectedFactorId, code).getOrThrow()
                _authUiState.update { it.copy(handleMultiFactorExceptionSuccess = true) }
                multiFactorContinuationJob?.invoke()
            } catch (e: Exception) {
                _authUiState.update { it.copy(error = e) }
            } finally {
                _authUiState.update {
                    it.copy(
                        isConfirmMultiFactor = false,
                        isMultiFactorRequired = false,
                        multiFactorHints = emptyList(),
                        selectedFactorIndex = null,
                        selectedFactorId = null,
                        showTotpDialog = false,
                        showPhoneDialog = false,
                        currentResolver = null,
                        isMultiFactorAborted = false
                    )
                }
                confirmTotpAuthenticationJob = null
                multiFactorContinuationJob = null
            }
        }
    }

    fun signInWithEmailAndPassword(email: String, password: String) {
        if (signInWithEmailAndPasswordJob?.isActive == true) return

        signInWithEmailAndPasswordJob = viewModelScope.launch {
            try {
                _authUiState.update { it.copy(isSignInWithEmailAndPassword = true) }
                authRepository.signInWithEmailAndPassword(email, password).getOrThrow()
                continueSignInWithEmailAndPassword()
            } catch (e: FirebaseAuthMultiFactorException) {
                handleMultiFactorException(e) {
                    continueSignInWithEmailAndPassword()
                }
            } catch (e: Exception) {
                _authUiState.update {
                    it.copy(error = e)
                }
            } finally {
                _authUiState.update { it.copy(isSignInWithEmailAndPassword = false) }
                signInWithEmailAndPasswordJob = null
            }
        }
    }

    private suspend fun continueSignInWithEmailAndPassword() {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            if (user.isEmailVerified) {
                userRepository.goOnline()
                val hasUserData = userRepository.checkUserData(user.uid).getOrThrow()
                Log.d("hasUserData", hasUserData.toString())
                if (hasUserData) {
                    val databaseUserData = userRepository.fetchUserDataOnce(user.uid).getOrThrow()
                    val databaseEmail = databaseUserData["email"] as? String
                    if (databaseEmail != user.email) {
                        val userData = User(
                            email = user.email
                        ).toMap().toMutableMap()
                        userRepository.updateUserData(user.uid, userData).getOrThrow()
                    }
                    val token = fcmTokenRepository.getToken().getOrThrow()
                    fcmTokenRepository.setToken(user.uid, token).getOrThrow()
                    _authUiState.update { it.copy(signInWithEmailAndPasswordSuccess = true) }
                } else {
                    authRepository.deleteUserAccount()
                    _authUiState.update { it.copy(error = Exception(Constants.ERROR_REGISTRATION_FAILED)) }
                }
            } else {
                userRepository.goOffline()
                authRepository.signOut()
                _authUiState.update { it.copy(error = Exception(Constants.ERROR_EMAIL_NOT_VERIFIED)) }
            }
        } else _authUiState.update { it.copy(error = Exception(Constants.ERROR_USER_NOT_FOUND)) }
    }

    fun verifyEmail(oobCode: String) {
        if (verifyEmailJob?.isActive == true) return

        verifyEmailJob = viewModelScope.launch {
            try {
                _authUiState.update { it.copy(isVerifyEmail = true) }
                authRepository.applyActionCode(oobCode).getOrThrow()
                _authUiState.update { it.copy(verifyEmailSuccess = true) }
            } catch (e: Exception) {
                _authUiState.update { it.copy(error = e) }
            } finally {
                _authUiState.update { it.copy(isVerifyEmail = false) }
                verifyEmailJob = null
            }
        }
    }

    fun revertSecondFactorAddition(oobCode: String) {
        if (revertSecondFactorAdditionJob?.isActive == true) return

        revertSecondFactorAdditionJob = viewModelScope.launch {
            try {
                _authUiState.update { it.copy(isRevertSecondFactorAddition = true) }
                authRepository.applyActionCode(oobCode).getOrThrow()
                _authUiState.update { it.copy(revertSecondFactorAdditionSuccess = true) }
            } catch (e: Exception) {
                _authUiState.update { it.copy(error = e) }
            } finally {
                _authUiState.update { it.copy(isRevertSecondFactorAddition = false) }
                revertSecondFactorAdditionJob = null
            }
        }
    }

    fun recoverEmail(oobCode: String) {
        if (recoverEmailJob?.isActive == true) return

        recoverEmailJob = viewModelScope.launch {
            try {
                _authUiState.update { it.copy(isRecoverEmail = true) }
                authRepository.applyActionCode(oobCode).getOrThrow()
                _authUiState.update { it.copy(recoverEmailSuccess = true) }
            } catch (e: Exception) {
                _authUiState.update { it.copy(error = e) }
            } finally {
                _authUiState.update { it.copy(isRecoverEmail = false) }
                recoverEmailJob = null
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        if (sendPasswordResetEmailJob?.isActive == true) return

        sendPasswordResetEmailJob = viewModelScope.launch {
            try {
                _authUiState.update { it.copy(isSendPasswordResetEmail = true) }
                authRepository.sendPasswordResetEmail(email).getOrThrow()
                _authUiState.update { it.copy(email = email, sendPasswordResetEmailSuccess = true) }
            } catch (e: Exception) {
                _authUiState.update { it.copy(error = e) }
            } finally {
                _authUiState.update { it.copy(isSendPasswordResetEmail = false) }
                sendPasswordResetEmailJob = null
            }
        }
    }

    fun resetPassword(oobCode: String, newPassword: String) {
        if (resetPasswordJob?.isActive == true) return

        resetPasswordJob = viewModelScope.launch {
            try {
                _authUiState.update { it.copy(isResetPassword = true) }
                authRepository.confirmPasswordReset(oobCode, newPassword).getOrThrow()
                _authUiState.update { it.copy(resetPasswordSuccess = true) }
            } catch (e: Exception) {
                _authUiState.update { it.copy(error = e) }
            } finally {
                _authUiState.update { it.copy(isResetPassword = false) }
                resetPasswordJob = null
            }
        }
    }

    fun errorMessageShown() {
        _authUiState.update { it.copy(error = null) }
    }

    fun signInWithEmailAndPasswordMessageShown() {
        _authUiState.update { it.copy(signInWithEmailAndPasswordSuccess = false) }
    }

    fun revertSecondFactorAdditionMessageShown() {
        _authUiState.update { it.copy(revertSecondFactorAdditionSuccess = false) }
    }

    fun recoverEmailMessageShown() {
        _authUiState.update { it.copy(recoverEmailSuccess = false) }
    }

    fun verifyEmailMessageShown() {
        _authUiState.update { it.copy(verifyEmailSuccess = false) }
    }

    fun sendPasswordResetEmailMessageShown() {
        _authUiState.update { it.copy(sendPasswordResetEmailSuccess = false) }
    }

    fun resetPasswordMessageShown() {
        _authUiState.update { it.copy(resetPasswordSuccess = false) }
    }

}
