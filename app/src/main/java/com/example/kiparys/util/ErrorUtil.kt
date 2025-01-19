package com.example.kiparys.util

import android.content.Context
import com.example.kiparys.Constants
import com.example.kiparys.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthEmailException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException


object ErrorUtil {

    fun getErrorMessage(context: Context, exception: Throwable): String {
        return when {
            exception.message == Constants.ERROR_PIN_MESSAGE_LIMIT_EXCEEDED -> {
                context.getString(R.string.error_pin_message_limit_exceeded)
            }

            exception.message == Constants.ERROR_PROJECT_NOT_FOUND -> {
                context.getString(R.string.error_project_not_found)
            }

            exception.message == Constants.ERROR_PIN_PROJECT_LIMIT_EXCEEDED -> {
                context.getString(R.string.error_pin_project_limit_exceeded)
            }

            exception.message == Constants.ERROR_USER_NOT_AUTHENTICATED -> {
                context.getString(R.string.error_user_not_authenticated)
            }

            exception.message == Constants.ERROR_EMAIL_NOT_VERIFIED -> {
                context.getString(R.string.error_email_not_verified)
            }

            exception.message == Constants.ERROR_REGISTRATION_FAILED -> {
                context.getString(R.string.error_registration_failed)
            }

            exception.message == Constants.ERROR_MFA_ABORTED -> {
                context.getString(R.string.error_multi_factor_aborted)
            }

            exception is FirebaseAuthMultiFactorException -> context.getString(R.string.error_multi_factor)
            exception is FirebaseAuthWeakPasswordException -> context.getString(R.string.error_weak_password)
            exception is FirebaseAuthActionCodeException -> when (exception.errorCode) {
                Constants.ERROR_EXPIRED_ACTION_CODE -> context.getString(R.string.error_expired_action_code)
                Constants.ERROR_INVALID_ACTION_CODE -> context.getString(R.string.error_invalid_action_code)
                else -> context.getString(
                    R.string.error_auth_failed,
                    exception.localizedMessage ?: R.string.error_unknown
                )
            }

            exception is FirebaseAuthInvalidCredentialsException -> when (exception.errorCode) {
                Constants.ERROR_WRONG_PASSWORD -> context.getString(R.string.error_password)
                Constants.ERROR_INVALID_EMAIL -> context.getString(R.string.error_invalid_email_format)
                Constants.ERROR_INVALID_CREDENTIAL -> context.getString(R.string.error_invalid_credentials)
                Constants.ERROR_USER_MISMATCH -> context.getString(R.string.error_user_mismatch)
                Constants.ERROR_REQUIRES_RECENT_LOGIN -> context.getString(R.string.error_requires_recent_login)
                Constants.ERROR_INVALID_PHONE_NUMBER -> context.getString(R.string.error_invalid_phone_number)
                Constants.ERROR_MISSING_PHONE_NUMBER -> context.getString(R.string.error_missing_phone_number)
                Constants.ERROR_MISSING_VERIFICATION_CODE -> context.getString(R.string.error_missing_verification_code)
                Constants.ERROR_INVALID_VERIFICATION_CODE -> context.getString(R.string.error_invalid_verification_code)
                Constants.ERROR_SESSION_EXPIRED -> context.getString(R.string.error_session_expired)
                Constants.ERROR_QUOTA_EXCEEDED -> context.getString(R.string.error_quota_exceeded)
                else -> context.getString(
                    R.string.error_auth_failed,
                    exception.localizedMessage ?: R.string.error_unknown
                )
            }

            exception is FirebaseAuthInvalidUserException -> when (exception.errorCode) {
                Constants.ERROR_USER_DISABLED -> context.getString(R.string.error_user_disabled)
                Constants.ERROR_USER_NOT_FOUND -> context.getString(R.string.error_user_not_found)
                Constants.ERROR_USER_TOKEN_EXPIRED -> context.getString(R.string.error_user_token_expired)
                Constants.ERROR_INVALID_USER_TOKEN -> context.getString(R.string.error_invalid_user_token)
                else -> context.getString(
                    R.string.error_auth_failed,
                    exception.localizedMessage ?: R.string.error_unknown
                )
            }

            exception is FirebaseAuthUserCollisionException -> when (exception.errorCode) {
                Constants.ERROR_EMAIL_ALREADY_IN_USE -> context.getString(R.string.error_user_exists)
                Constants.ERROR_CREDENTIAL_ALREADY_IN_USE -> context.getString(R.string.error_credential_already_in_use)
                else -> context.getString(
                    R.string.error_auth_failed,
                    exception.localizedMessage ?: R.string.error_unknown
                )
            }

            exception is FirebaseAuthEmailException -> {
                context.getString(
                    R.string.error_auth_failed,
                    exception.localizedMessage ?: R.string.error_unknown
                )
            }

            exception is FirebaseNetworkException -> context.getString(R.string.error_network_unavailable)
            exception is FirebaseTooManyRequestsException -> context.getString(R.string.error_too_many_requests)
            exception is FirebaseAuthException -> when (exception.errorCode) {
                Constants.ERROR_CUSTOM_TOKEN_MISMATCH -> context.getString(R.string.error_custom_token_mismatch)
                Constants.ERROR_OPERATION_NOT_ALLOWED -> context.getString(R.string.error_operation_not_allowed)
                Constants.ERROR_TOO_MANY_REQUESTS -> context.getString(R.string.error_too_many_requests)
                Constants.ERROR_APP_NOT_AUTHORIZED -> context.getString(R.string.error_app_not_authorized)
                Constants.ERROR_API_NOT_AVAILABLE -> context.getString(R.string.error_api_not_available)
                Constants.ERROR_WEB_CONTEXT_CANCELED -> context.getString(R.string.error_web_context_canceled)
                else -> context.getString(
                    R.string.error_auth_failed,
                    exception.localizedMessage ?: R.string.error_unknown
                )
            }

            else -> context.getString(
                R.string.error_unknown_with_details,
                exception.localizedMessage ?: R.string.error_unknown
            )
        }
    }

}
