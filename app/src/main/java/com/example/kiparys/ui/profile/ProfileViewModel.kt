package com.example.kiparys.ui.profile

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiparys.data.model.User
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.UserRepository
import com.example.kiparys.Constants.CONTAIN_PLACEHOLDER
import com.example.kiparys.Constants.CONTAIN_UPLOAD
import com.example.kiparys.Constants.ERROR_MFA_ABORTED
import com.example.kiparys.Constants.IMAGE_LOW_QUALITY
import com.example.kiparys.Constants.PHONE_FACTOR
import com.example.kiparys.Constants.TOTP_FACTOR
import com.example.kiparys.data.repository.DataManagementRepository
import com.example.kiparys.util.FilePathUtil
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.TotpSecret
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class ProfileUiState(
    val enrolledFactors: List<MultiFactorInfo>? = null,
    val totpSecret: TotpSecret? = null,
    val qrCodeUri: String? = null,
    val newEmail: String? = null,

    val multiFactorHints: List<MultiFactorInfo> = emptyList(),
    val isMultiFactorRequired: Boolean = false,
    val isMultiFactorAborted: Boolean = false,
    val selectedFactorIndex: Int? = null,
    val selectedFactorId: String? = null,
    val showTotpDialog: Boolean = false,
    val showPhoneDialog: Boolean = false,
    val currentResolver: MultiFactorResolver? = null,

    val handleMultiFactorExceptionSuccess: Boolean = false,
    val getEnrolledFactorsSuccess: Boolean = false,
    val verifyAndUpdateUserEmailSuccess: Boolean = false,
    val generateTotpSecretSuccess: Boolean = false,
    val verifyAndChangeEmailSuccess: Boolean = false,
    val updatePasswordSuccess: Boolean = false,
    val unEnrollTotpSuccess: Boolean = false,
    val enrollTotpSuccess: Boolean = false,
    val saveFullNameSuccess: Boolean = false,
    val saveAboutSuccess: Boolean = false,
    val saveBirthdateSuccess: Boolean = false,
    val deleteAccountSuccess: Boolean = false,

    val isConfirmMultiFactor: Boolean = false,
    val isEnrolledFactors: Boolean = false,
    val isVerifyAndUpdateUserEmail: Boolean = false,
    val isGenerateTotpSecret: Boolean = false,
    val isSaveAbout: Boolean = false,
    val isSaveBirthdate: Boolean = false,
    val isSaveProfileImage: Boolean = false,
    val isSaveFullName: Boolean = false,
    val isDeleteProfileImage: Boolean = false,
    val isVerifyAndChangeEmail: Boolean = false,
    val isUnEnrollTotp: Boolean = false,
    val isEnrollTotp: Boolean = false,
    val isOpenInOtpApp: Boolean = false,
    val isDeleteAccount: Boolean = false,
    val isUpdatePassword: Boolean = false,

    val error: Throwable? = null,
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val dataManagementRepository: DataManagementRepository
) : ViewModel() {
    private var multiFactorContinuationJob: (suspend () -> Unit)? = null
    private var saveBirthdateJob: Job? = null
    private var deleteProfileImageJob: Job? = null
    private var saveProfileImageJob: Job? = null
    private var saveAboutJob: Job? = null
    private var saveFullNameJob: Job? = null
    private var verifyAndUpdateUserEmailJob: Job? = null
    private var verifyAndChangeEmailJob: Job? = null
    private var updatePasswordJob: Job? = null
    private var deleteAccountJob: Job? = null
    private var generateSecretJob: Job? = null
    private var enrollTotpJob: Job? = null
    private var openInOtpAppJob: Job? = null
    private var unEnrollTotpAppJob: Job? = null
    private var confirmTotpAuthenticationJob: Job? = null

    private val _profileUiState = MutableStateFlow(ProfileUiState())
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState

    private fun handleMultiFactorException(
        e: FirebaseAuthMultiFactorException,
        continuation: suspend () -> Unit
    ) {
        multiFactorContinuationJob = continuation

        _profileUiState.update {
            it.copy(
                isMultiFactorAborted = false,
                isMultiFactorRequired = true,
                currentResolver = e.resolver,
                multiFactorHints = e.resolver.hints.map { hint -> hint }
            )
        }
    }

    fun abortMultiFactorAuthentication() {
        if (_profileUiState.value.handleMultiFactorExceptionSuccess == true) {
            _profileUiState.update {
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
            _profileUiState.update {
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
        if (_profileUiState.value.isMultiFactorAborted) {
            return
        }

        val selectedFactor = _profileUiState.value.multiFactorHints.getOrNull(selectedIndex)
        _profileUiState.update {
            it.copy(
                isMultiFactorRequired = false,
                selectedFactorIndex = selectedIndex,
                selectedFactorId = selectedFactor?.factorId
            )
        }

        when (selectedFactor?.factorId) {
            TOTP_FACTOR -> _profileUiState.update { it.copy(showTotpDialog = true) }
            PHONE_FACTOR -> _profileUiState.update { it.copy(showPhoneDialog = true) }
            else -> {}
        }
    }

    fun confirmTotpAuthentication(code: String) {
        if (_profileUiState.value.isMultiFactorAborted) {
            return
        }

        if (confirmTotpAuthenticationJob?.isActive == true) return

        confirmTotpAuthenticationJob = viewModelScope.launch {
            val resolver = _profileUiState.value.currentResolver ?: return@launch
            val selectedFactorId = _profileUiState.value.selectedFactorId ?: return@launch

            try {
                _profileUiState.update { it.copy(isConfirmMultiFactor = true) }
                authRepository.verifyMultiFactor(resolver, selectedFactorId, code).getOrThrow()
                _profileUiState.update { it.copy(handleMultiFactorExceptionSuccess = true) }
                multiFactorContinuationJob?.invoke()
            } catch (e: Exception) {
                _profileUiState.update { it.copy(error = e) }
            } finally {
                _profileUiState.update {
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

    fun observeEnrolledFactors() {
        viewModelScope.launch {
            try {
                _profileUiState.update { it.copy(isEnrolledFactors = true) }

                val enrolledFactors = authRepository.getEnrolledFactors().getOrThrow()

                _profileUiState.update {
                    it.copy(
                        enrolledFactors = enrolledFactors,
                        getEnrolledFactorsSuccess = true
                    )
                }
            } catch (e: Exception) {
                _profileUiState.update { it.copy(error = e) }
            } finally {
                _profileUiState.update { it.copy(isEnrolledFactors = false) }
            }
        }
    }

    fun unEnrollTotp(currentEmail: String, currentPassword: String, mfaEnrollmentId: String) {
        if (unEnrollTotpAppJob?.isActive == true) return

        unEnrollTotpAppJob = viewModelScope.launch {
            try {
                _profileUiState.update { it.copy(isUnEnrollTotp = true) }
                authRepository.reAuthenticateUser(currentEmail, currentPassword).getOrThrow()
                continueUnEnrollTotp(mfaEnrollmentId)
            } catch (e: FirebaseAuthMultiFactorException) {
                handleMultiFactorException(e) {
                    continueUnEnrollTotp(mfaEnrollmentId)
                }
            } catch (e: Exception) {
                _profileUiState.update { it.copy(error = e) }
            } finally {
                _profileUiState.update { it.copy(isUnEnrollTotp = false) }
                unEnrollTotpAppJob = null
            }
        }
    }

    private suspend fun continueUnEnrollTotp(mfaEnrollmentId: String) {
        authRepository.unEnrollTotp(mfaEnrollmentId).getOrThrow()
        observeEnrolledFactors()
        _profileUiState.update { it.copy(unEnrollTotpSuccess = true) }
    }

    fun openInOtpApp(totpSecret: TotpSecret, qrCodeUri: String) {
        if (openInOtpAppJob?.isActive == true) return

        openInOtpAppJob = viewModelScope.launch {
            try {
                _profileUiState.update { it.copy(isOpenInOtpApp = true) }
                authRepository.openInOtpApp(totpSecret, qrCodeUri).getOrThrow()
            } catch (e: Exception) {
                _profileUiState.update { it.copy(error = e) }
            } finally {
                _profileUiState.update { it.copy(isOpenInOtpApp = false) }
                openInOtpAppJob = null
            }
        }
    }

    fun enrollTotp(totpSecret: TotpSecret, verificationCode: String) {
        if (enrollTotpJob?.isActive == true) return

        enrollTotpJob = viewModelScope.launch {
            try {
                _profileUiState.update { it.copy(isEnrollTotp = true) }
                authRepository.enrollTotp(totpSecret, verificationCode).getOrThrow()
                observeEnrolledFactors()
                _profileUiState.update { it.copy(enrollTotpSuccess = true) }
            } catch (e: Exception) {
                _profileUiState.update { it.copy(error = e) }
            } finally {
                _profileUiState.update { it.copy(isEnrollTotp = false) }
                enrollTotpJob = null
            }
        }
    }

    fun generateTotpSecret(currentEmail: String, currentPassword: String) {
        if (generateSecretJob?.isActive == true) return

        generateSecretJob = viewModelScope.launch {
            try {
                _profileUiState.update { it.copy(isGenerateTotpSecret = true) }
                authRepository.reAuthenticateUser(currentEmail, currentPassword).getOrThrow()
                continueTotpGeneration(currentEmail)
            } catch (e: FirebaseAuthMultiFactorException) {
                handleMultiFactorException(e) {
                    continueTotpGeneration(currentEmail)
                }
            } catch (e: Exception) {
                _profileUiState.update { it.copy(error = e) }
            } finally {
                _profileUiState.update { it.copy(isGenerateTotpSecret = false) }
                generateSecretJob = null
            }
        }
    }

    private suspend fun continueTotpGeneration(currentEmail: String) {
        val totpSecret = authRepository.generateTotpSecret().getOrThrow()
        val qrCodeUri = authRepository.generateQrCodeUrl(totpSecret, currentEmail).getOrThrow()
        _profileUiState.update {
            it.copy(
                totpSecret = totpSecret,
                qrCodeUri = qrCodeUri,
                generateTotpSecretSuccess = true
            )
        }
    }

    fun deleteAccount(currentEmail: String, currentPassword: String) {
        if (deleteAccountJob?.isActive == true) return

        deleteAccountJob = viewModelScope.launch {
            try {
                _profileUiState.update { it.copy(isDeleteAccount = true) }
                authRepository.reAuthenticateUser(currentEmail, currentPassword).getOrThrow()
                continueDeleteAccount()
            } catch (e: FirebaseAuthMultiFactorException) {
                handleMultiFactorException(e) {
                    continueDeleteAccount()
                }
            } catch (e: Exception) {
                _profileUiState.update { it.copy(error = e) }
            } finally {
                _profileUiState.update { it.copy(isDeleteAccount = false) }
                deleteAccountJob = null
            }
        }
    }

    private suspend fun continueDeleteAccount() {
        val userId = authRepository.getCurrentUserId()
        if (userId.isNullOrEmpty()) {
            throw IllegalArgumentException("User ID cannot be null or empty")
        }
        val userData = User(
            deleted = true
        ).toMap().toMutableMap()
        userRepository.updateUserData(userId, userData).getOrThrow()
        authRepository.deleteUserAccount().getOrThrow()
        _profileUiState.update { it.copy(deleteAccountSuccess = true) }
    }

    fun updatePassword(newPassword: String, currentEmail: String, currentPassword: String) {
        if (updatePasswordJob?.isActive == true) return

        updatePasswordJob = viewModelScope.launch {
            try {
                _profileUiState.update { it.copy(isUpdatePassword = true) }
                authRepository.reAuthenticateUser(currentEmail, currentPassword).getOrThrow()
                continueUpdatePassword(newPassword)
            } catch (e: FirebaseAuthMultiFactorException) {
                handleMultiFactorException(e) {
                    continueUpdatePassword(newPassword)
                }
            } catch (e: Exception) {
                _profileUiState.update { it.copy(error = e) }
            } finally {
                _profileUiState.update { it.copy(isUpdatePassword = false) }
                updatePasswordJob = null
            }
        }
    }

    private suspend fun continueUpdatePassword(newPassword: String) {
        authRepository.updateUserPassword(newPassword).getOrThrow()
        _profileUiState.update { it.copy(updatePasswordSuccess = true) }
    }

    fun verifyAndChangeEmail(oobCode: String) {
        if (verifyAndChangeEmailJob?.isActive == true) return

        verifyAndChangeEmailJob = viewModelScope.launch {
            try {
                _profileUiState.update { it.copy(isVerifyAndChangeEmail = true) }

                authRepository.applyActionCode(oobCode).getOrThrow()
                val userId = authRepository.getCurrentUserId()

                if (userId.isNullOrEmpty()) {
                    throw IllegalArgumentException("User ID cannot be null or empty")
                }

                val userDataResult = userRepository.fetchUserDataOnce(userId)
                val userDataMap = userDataResult.getOrThrow()

                val unverifiedEmail = userDataMap["unverifiedEmail"] as? String

                if (unverifiedEmail.isNullOrEmpty()) {
                    throw IllegalStateException("Unverified email is missing in the user's data")
                }

                val userData = User(
                    email = unverifiedEmail
                ).toMap().toMutableMap()
                userData["unverifiedEmail"] = null

                userRepository.updateUserData(userId, userData).getOrThrow()
                _profileUiState.update { it.copy(verifyAndChangeEmailSuccess = true) }
            } catch (e: Exception) {
                _profileUiState.update { it.copy(error = e) }
            } finally {
                _profileUiState.update { it.copy(isVerifyAndChangeEmail = false) }
                verifyAndChangeEmailJob = null
            }
        }
    }

    fun verifyAndUpdateUserEmail(newEmail: String, currentEmail: String, currentPassword: String) {
        if (verifyAndUpdateUserEmailJob?.isActive == true) return

        verifyAndUpdateUserEmailJob = viewModelScope.launch {
            try {
                _profileUiState.update { it.copy(isVerifyAndUpdateUserEmail = true) }
                authRepository.reAuthenticateUser(currentEmail, currentPassword).getOrThrow()
                continueVerifyAndUpdateUserEmail(newEmail)
            } catch (e: FirebaseAuthMultiFactorException) {
                handleMultiFactorException(e) {
                    continueVerifyAndUpdateUserEmail(newEmail)
                }
            } catch (e: Exception) {
                _profileUiState.update { it.copy(error = e) }
            } finally {
                _profileUiState.update { it.copy(isVerifyAndUpdateUserEmail = false) }
                verifyAndUpdateUserEmailJob = null
            }
        }
    }

    private suspend fun continueVerifyAndUpdateUserEmail(newEmail: String) {
        authRepository.verifyAndUpdateUserEmail(newEmail)
        val userId = authRepository.getCurrentUserId()
        if (userId.isNullOrEmpty()) {
            throw IllegalArgumentException("User ID cannot be null or empty")
        }
        val userData = User(
            unverifiedEmail = newEmail
        ).toMap().toMutableMap()
        userRepository.updateUserData(userId, userData).getOrThrow()
        _profileUiState.update {
            it.copy(
                newEmail = newEmail,
                verifyAndUpdateUserEmailSuccess = true
            )
        }
    }

    fun saveFullName(firstName: String, lastName: String?) {
        if (saveFullNameJob?.isActive == true) return

        saveFullNameJob = viewModelScope.launch {
            try {
                _profileUiState.update { it.copy(isSaveFullName = true) }

                val userId = authRepository.getCurrentUserId()

                if (userId.isNullOrEmpty()) {
                    throw IllegalArgumentException("User ID cannot be null or empty")
                }

                val userData = User(
                    firstName = firstName,
                    lastName = lastName
                ).toMap().toMutableMap()

                if (lastName == null) {
                    userData["lastName"] = null
                }

                val displayName = if (!lastName.isNullOrEmpty()) {
                    "$firstName $lastName"
                } else {
                    firstName
                }

                authRepository.updateProfile(displayName = displayName, photoUrl = null)
                    .getOrThrow()
                userRepository.updateUserData(userId, userData).getOrThrow()

                launch(Dispatchers.IO) {
                    dataManagementRepository.updateUsername(userId)
                        .onFailure { Log.e(TAG, "Failed to update after retries: ${it.message}") }
                }
                _profileUiState.update { it.copy(saveFullNameSuccess = true) }
            } catch (e: Exception) {
                _profileUiState.update { it.copy(error = e) }
            } finally {
                _profileUiState.update { it.copy(isSaveFullName = false) }
                saveFullNameJob = null
            }
        }
    }

    fun saveAbout(about: String?) {
        if (saveAboutJob?.isActive == true) return

        saveAboutJob = viewModelScope.launch {
            try {
                _profileUiState.update { it.copy(isSaveAbout = true) }

                val userId = authRepository.getCurrentUserId()

                if (userId.isNullOrEmpty()) {
                    throw IllegalArgumentException("User ID cannot be null or empty")
                }

                val userData = User(
                    about = about
                ).toMap().toMutableMap()

                if (about == null) {
                    userData["about"] = null
                }

                userRepository.updateUserData(userId, userData).getOrThrow()
                _profileUiState.update { it.copy(saveAboutSuccess = true) }
            } catch (e: Exception) {
                _profileUiState.update { it.copy(error = e) }
            } finally {
                _profileUiState.update { it.copy(isSaveAbout = false) }
                saveAboutJob = null
            }
        }
    }

    fun saveBirthdate(birthdate: Long?) {
        if (saveBirthdateJob?.isActive == true) return

        saveBirthdateJob = viewModelScope.launch {
            try {
                _profileUiState.update { it.copy(isSaveBirthdate = true) }
                val userId = authRepository.getCurrentUserId()

                if (userId.isNullOrEmpty()) {
                    throw IllegalArgumentException("User ID cannot be null or empty")
                }

                val userData = User(
                    birthdate = birthdate
                ).toMap().toMutableMap()

                if (birthdate == null) {
                    userData["birthdate"] = null
                }

                userRepository.updateUserData(userId, userData).getOrThrow()
                _profileUiState.update { it.copy(saveBirthdateSuccess = true) }
            } catch (e: Exception) {
                _profileUiState.update {
                    it.copy(
                        error = e
                    )
                }
            } finally {
                _profileUiState.update { it.copy(isSaveBirthdate = false) }
                saveBirthdateJob = null
            }
        }
    }

    fun saveProfileImage(profileImageBitmap: Bitmap) {
        if (saveProfileImageJob?.isActive == true) return

        saveProfileImageJob = viewModelScope.launch {
            try {
                _profileUiState.update { it.copy(isSaveProfileImage = true) }

                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { "User ID cannot be null or empty" }

                val profileImageFileName = FilePathUtil.generateFilePath(
                    FilePathUtil.FileType.USER_PROFILE_IMAGE_UPLOAD,
                    userId
                )

                val profileImageUrlDeferred = async {
                    userRepository.uploadProfileImage(
                        profileImageBitmap,
                        profileImageFileName,
                        IMAGE_LOW_QUALITY
                    ).getOrThrow()
                }

                val deletePreviousImageDeferred = async {
                    val uploadProfileImageUrl =
                        userRepository.getProfileImageUrl(userId, CONTAIN_UPLOAD)
                    if (!uploadProfileImageUrl.isNullOrEmpty()) {
                        userRepository.deleteFileFromStorage(uploadProfileImageUrl).getOrThrow()
                    }
                }

                deletePreviousImageDeferred.await()

                val profileImageUrl = profileImageUrlDeferred.await()

                val userDataDeferred = async {
                    val userData = User(profileImageUrl = profileImageUrl).toMap()
                    userRepository.updateUserData(userId, userData).getOrThrow()
                }

                val updateProfileDeferred = async {
                    val profileImageUri = Uri.parse(profileImageUrl)
                    authRepository.updateProfile(displayName = null, photoUrl = profileImageUri)
                        .getOrThrow()
                }

                userDataDeferred.await()
                updateProfileDeferred.await()

                launch(Dispatchers.IO) {
                    dataManagementRepository.updateProfileImageUrl(userId)
                        .onFailure { Log.e(TAG, "Failed to update after retries: ${it.message}") }
                }
            } catch (e: Exception) {
                _profileUiState.update { it.copy(error = e) }
            } finally {
                _profileUiState.update { it.copy(isSaveProfileImage = false) }
                saveProfileImageJob = null
            }
        }
    }

    fun deleteProfileImage(profileImageUrl: String) {
        if (deleteProfileImageJob?.isActive == true) return

        deleteProfileImageJob = viewModelScope.launch {
            try {
                _profileUiState.update { it.copy(isDeleteProfileImage = true) }

                val userId = authRepository.getCurrentUserId()
                require(!userId.isNullOrEmpty()) { "User ID cannot be null or empty" }

                val defaultProfileImageUrl =
                    userRepository.getProfileImageUrl(userId, CONTAIN_PLACEHOLDER)
                        ?: throw Exception("Failed to get profile image URL")

                val userData = User(profileImageUrl = defaultProfileImageUrl).toMap().toMutableMap()

                val updateUserDataDeferred = async {
                    userRepository.updateUserData(userId, userData).getOrThrow()
                }

                val updateProfileDeferred = async {
                    val profileImageUri = Uri.parse(defaultProfileImageUrl)
                    authRepository.updateProfile(displayName = null, photoUrl = profileImageUri)
                        .getOrThrow()
                }

                val deleteProfileImageDeferred = async {
                    userRepository.deleteFileFromStorage(profileImageUrl).getOrThrow()
                }

                awaitAll(updateUserDataDeferred, updateProfileDeferred, deleteProfileImageDeferred)

                launch(Dispatchers.IO) {
                    dataManagementRepository.updateProfileImageUrl(userId)
                        .onFailure { Log.e(TAG, "Failed to update after retries: ${it.message}") }
                }
            } catch (e: Exception) {
                _profileUiState.update { it.copy(error = e) }
            } finally {
                _profileUiState.update { it.copy(isDeleteProfileImage = false) }
                deleteProfileImageJob = null
            }
        }
    }

    fun deleteAccountMessageShown() {
        _profileUiState.update { it.copy(deleteAccountSuccess = false) }
    }

    fun saveBirthdateMessageShown() {
        _profileUiState.update { it.copy(saveBirthdateSuccess = false) }
    }

    fun saveAboutMessageShown() {
        _profileUiState.update { it.copy(saveAboutSuccess = false) }
    }

    fun saveFullNameMessageShown() {
        _profileUiState.update { it.copy(saveFullNameSuccess = false) }
    }

    fun enrollTotpMessageShown() {
        _profileUiState.update { it.copy(enrollTotpSuccess = false) }
    }

    fun unEnrollTotpMessageShown() {
        _profileUiState.update { it.copy(unEnrollTotpSuccess = false) }
    }

    fun updatePasswordMessageShown() {
        _profileUiState.update { it.copy(updatePasswordSuccess = false) }
    }

    fun verifyAndChangeEmailMessageShown() {
        _profileUiState.update { it.copy(verifyAndChangeEmailSuccess = false) }
    }

    fun verifyAndUpdateUserEmailMessageShown() {
        _profileUiState.update { it.copy(verifyAndUpdateUserEmailSuccess = false) }
    }

    fun generateTotpSecretMessageShown() {
        _profileUiState.update { it.copy(generateTotpSecretSuccess = false) }
    }

    fun errorMessageShown() {
        _profileUiState.update { it.copy(error = null) }
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}
