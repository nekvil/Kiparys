package com.example.kiparys.data.repository

import android.net.Uri
import android.util.Log
import com.example.kiparys.Constants.APP_NAME
import com.example.kiparys.Constants.ERROR_UNKNOWN
import com.example.kiparys.Constants.ERROR_USER_NOT_AUTHENTICATED
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.TotpMultiFactorGenerator
import com.google.firebase.auth.TotpSecret
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale

class AuthRepository {

    private val firebaseAuth: FirebaseAuth = Firebase.auth

    init {
        setLanguageForFirebaseAuth()
    }

    private fun setLanguageForFirebaseAuth() {
        val languageCode = Locale.getDefault().language
        firebaseAuth.setLanguageCode(languageCode)
    }

    fun getCurrentUserPhotoUrl(): String? {
        return firebaseAuth.currentUser?.photoUrl.toString()
    }

    fun getCurrentUserDisplayName(): String? {
        return firebaseAuth.currentUser?.displayName
    }

    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    fun isUserAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun verifyMultiFactor(
        resolver: MultiFactorResolver,
        factorId: String,
        code: String
    ): Result<Unit> {
        return try {
            val assertion = when (factorId) {
                TotpMultiFactorGenerator.FACTOR_ID -> {
                    TotpMultiFactorGenerator.getAssertionForSignIn(
                        resolver.hints.find { it.factorId == factorId }?.uid
                            ?: throw IllegalArgumentException("UID not found"),
                        code
                    )
                }

                PhoneMultiFactorGenerator.FACTOR_ID -> {
                    PhoneMultiFactorGenerator.getAssertion(
                        PhoneAuthProvider.getCredential(
                            resolver.hints.find { it.factorId == factorId }?.uid
                                ?: throw IllegalArgumentException("UID not found"),
                            code
                        )
                    )
                }

                else -> throw IllegalArgumentException("Unsupported factor ID: $factorId")
            }

            resolver.resolveSignIn(assertion).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getEnrolledFactors(): Result<List<MultiFactorInfo>> {
        return try {
            val currentUser = firebaseAuth.currentUser
                ?: throw IllegalStateException("Current user is null")
            val enrolledFactors = currentUser.multiFactor.enrolledFactors
            Result.success(enrolledFactors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateTotpSecret(): Result<TotpSecret> {
        return try {
            val session = firebaseAuth.currentUser?.multiFactor?.session?.await()
            if (session == null) {
                throw IllegalStateException("Multi-factor session is null")
            }
            val totpSecret = TotpMultiFactorGenerator.generateSecret(session).await()
            Result.success(totpSecret)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateQrCodeUrl(totpSecret: TotpSecret, email: String): Result<String> {
        return try {
            val qrCodeUri = totpSecret.generateQrCodeUrl(
                email,
                APP_NAME
            )
            Result.success(qrCodeUri)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openInOtpApp(totpSecret: TotpSecret, qrCodeUri: String): Result<Unit> {
        return try {
            totpSecret.openInOtpApp(qrCodeUri)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun enrollTotp(totpSecret: TotpSecret, verificationCode: String): Result<Unit> {
        return try {
            val multiFactorAssertion =
                TotpMultiFactorGenerator.getAssertionForEnrollment(totpSecret, verificationCode)
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                throw IllegalStateException("Current user is null")
            }
            currentUser.multiFactor.enroll(multiFactorAssertion, "TOTP").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unEnrollTotp(mfaEnrollmentId: String): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                throw IllegalStateException("Current user is null")
            }
            currentUser.multiFactor.unenroll(mfaEnrollmentId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAuthStateFlow(): Flow<Boolean> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val currentUser = auth.currentUser

            currentUser?.reload()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(true).isSuccess
                } else {
                    trySend(false).isSuccess
                }
            } ?: run {
                trySend(false).isSuccess
            }
        }

        firebaseAuth.addAuthStateListener(authStateListener)
        Log.d(TAG, "addAuthStateListener")
        awaitClose {
            Log.d(TAG, "removeAuthStateListener")
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception(ERROR_UNKNOWN))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserPassword(newPassword: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser ?: return Result.failure(
                Exception(ERROR_USER_NOT_AUTHENTICATED)
            )
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(displayName: String?, photoUrl: Uri?): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser ?: return Result.failure(
                Exception(ERROR_USER_NOT_AUTHENTICATED)
            )
            val profileUpdates = UserProfileChangeRequest.Builder().apply {
                displayName?.let { setDisplayName(it) }
                photoUrl?.let { photoUri = it }
            }.build()
            user.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyAndUpdateUserEmail(newEmail: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser ?: return Result.failure(
                Exception(ERROR_USER_NOT_AUTHENTICATED)
            )
            user.verifyBeforeUpdateEmail(newEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser ?: return Result.failure(
                Exception(ERROR_USER_NOT_AUTHENTICATED)
            )
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception(ERROR_UNKNOWN))
            }
        } catch (e: FirebaseAuthMultiFactorException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reAuthenticateUser(email: String, password: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser ?: return Result.failure(
                Exception(ERROR_USER_NOT_AUTHENTICATED)
            )
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthMultiFactorException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyActionCode(oobCode: String): Result<Unit> {
        return try {
            firebaseAuth.applyActionCode(oobCode).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmPasswordReset(oobCode: String, newPassword: String): Result<Unit> {
        return try {
            firebaseAuth.confirmPasswordReset(oobCode, newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccount(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser ?: return Result.failure(
                Exception(ERROR_USER_NOT_AUTHENTICATED)
            )
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
