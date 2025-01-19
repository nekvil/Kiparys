package com.example.kiparys.ui.authregister

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kiparys.R
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.FcmTokenRepository
import com.example.kiparys.data.repository.UserRepository
import com.example.kiparys.databinding.BottomSheetSelectTwoFactorBinding
import com.example.kiparys.databinding.DialogNewPasswordBinding
import com.example.kiparys.databinding.DialogPasswordResetBinding
import com.example.kiparys.databinding.DialogTotpCodeRequiredBinding
import com.example.kiparys.databinding.FragmentAuthBinding
import com.example.kiparys.ui.adapter.TwoFactorOptionsAdapter
import com.example.kiparys.util.ErrorUtil
import com.example.kiparys.util.StringUtil.maskEmail
import com.example.kiparys.util.SystemUtil.isInternetAvailable
import com.example.kiparys.util.ValidationUtil
import com.example.kiparys.util.SystemUtil.triggerValidationFailureVibration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.MultiFactorInfo
import kotlinx.coroutines.launch
import kotlin.getValue


class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val fragmentAuthBinding get() = _binding!!
    private var passwordResetDialog: AlertDialog? = null
    private var newPasswordDialog: AlertDialog? = null
    private var verifyEmailSuccessDialog: AlertDialog? = null
    private var recoverEmailSuccessDialog: AlertDialog? = null
    private var passwordResetSentLinkNoticeDialog: AlertDialog? = null
    private var revertSecondFactorAdditionSuccessDialog: AlertDialog? = null
    private var selectMultiFactorDialog: BottomSheetDialog? = null
    private var totpCodeRequiredDialog: AlertDialog? = null
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            AuthRepository(),
            UserRepository(),
            FcmTokenRepository()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        setupFieldListeners()
        return fragmentAuthBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val appLink = arguments?.getString("appLink")

        appLink?.let { link ->
            val uri = Uri.parse(link)
            val mode = uri.getQueryParameter("mode")
            val oobCode = uri.getQueryParameter("oobCode")

            when (mode) {
                "resetPassword" -> {
                    if (oobCode != null) {
                        showNewPasswordDialog(oobCode)
                    } else {
                        Snackbar.make(
                            requireView(),
                            getString(R.string.error_invalid_link),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                "verifyEmail" -> {
                    if (oobCode != null) {
                        authViewModel.verifyEmail(oobCode)
                    } else {
                        Snackbar.make(
                            requireView(),
                            getString(R.string.error_invalid_link),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                "recoverEmail" -> {
                    if (oobCode != null) {
                        authViewModel.recoverEmail(oobCode)
                    } else {
                        Snackbar.make(
                            requireView(),
                            getString(R.string.error_invalid_link),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                "revertSecondFactorAddition" -> {
                    if (oobCode != null) {
                        authViewModel.revertSecondFactorAddition(oobCode)
                    } else {
                        Snackbar.make(
                            requireView(),
                            getString(R.string.error_invalid_link),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                else -> {
                    Log.e(TAG, "Unknown appLink mode: $mode")
                }
            }

            arguments?.putString("appLink", null)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {

                authViewModel.authUiState.collect { uiState ->
                    uiState.error?.let { error ->
                        passwordResetDialog?.dismiss()
                        newPasswordDialog?.dismiss()
                        verifyEmailSuccessDialog?.dismiss()
                        recoverEmailSuccessDialog?.dismiss()
                        revertSecondFactorAdditionSuccessDialog?.dismiss()
                        selectMultiFactorDialog?.dismiss()
                        totpCodeRequiredDialog?.dismiss()
                        passwordResetSentLinkNoticeDialog?.dismiss()
                        Snackbar.make(
                            requireView(),
                            ErrorUtil.getErrorMessage(
                                requireContext(),
                                error
                            ),
                            Snackbar.LENGTH_LONG
                        ).show()
                        authViewModel.errorMessageShown()
                    }

                    if (uiState.isMultiFactorRequired) {
                        fragmentAuthBinding.root.clearFocus()

                        val inputMethodManager =
                            fragmentAuthBinding.root.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(
                            fragmentAuthBinding.root.windowToken,
                            0
                        )

                        fragmentAuthBinding.root.postDelayed({
                            showSelectMultiFactorDialog(uiState.multiFactorHints)
                        }, 200)
                    }

                    if (uiState.showTotpDialog) {
                        showTotpCodeRequiredDialog()
                    }

                    if (uiState.handleMultiFactorExceptionSuccess == true) {
                        totpCodeRequiredDialog?.dismiss()
                    }

                    fragmentAuthBinding.btnAuth.isEnabled = !uiState.isSignInWithEmailAndPassword
                    fragmentAuthBinding.btnPasswordReset.isEnabled =
                        !uiState.isSignInWithEmailAndPassword

                    if (uiState.isSignInWithEmailAndPassword) {
                        fragmentAuthBinding.btnAuth.text = getString(R.string.prompt_empty_string)
                        fragmentAuthBinding.authProgressBar.visibility = View.VISIBLE
                    } else {
                        fragmentAuthBinding.btnAuth.text = getString(R.string.action_sign_in)
                        fragmentAuthBinding.authProgressBar.visibility = View.GONE
                    }

                    if (uiState.signInWithEmailAndPasswordSuccess) {
                        findNavController().navigate(R.id.action_nav_graph_auth_to_nav_graph)
                        authViewModel.signInWithEmailAndPasswordMessageShown()
                    }

                    if (uiState.verifyEmailSuccess) {
                        verifyEmailSuccessDialog = MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.dialog_title_email_verification_success))
                            .setMessage(getString(R.string.dialog_message_email_verification_success))
                            .setPositiveButton(getString(R.string.action_ok)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setOnDismissListener {
                                verifyEmailSuccessDialog = null
                            }
                            .create()
                        verifyEmailSuccessDialog?.show()
                        authViewModel.verifyEmailMessageShown()
                    }

                    if (uiState.recoverEmailSuccess) {
                        recoverEmailSuccessDialog = MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.dialog_title_email_recovery_success))
                            .setMessage(getString(R.string.dialog_message_email_recovery_success))
                            .setPositiveButton(getString(R.string.action_ok)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setOnDismissListener {
                                recoverEmailSuccessDialog = null
                            }
                            .create()
                        recoverEmailSuccessDialog?.show()
                        authViewModel.recoverEmailMessageShown()
                    }

                    if (uiState.revertSecondFactorAdditionSuccess) {
                        revertSecondFactorAdditionSuccessDialog =
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(getString(R.string.dialog_title_revert_second_factor_addition_success))
                                .setMessage(getString(R.string.dialog_message_revert_second_factor_addition_success))
                                .setPositiveButton(getString(R.string.action_ok)) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setOnDismissListener {
                                    revertSecondFactorAdditionSuccessDialog = null
                                }
                                .create()
                        revertSecondFactorAdditionSuccessDialog?.show()
                        authViewModel.revertSecondFactorAdditionMessageShown()
                    }

                    if (uiState.sendPasswordResetEmailSuccess) {
                        passwordResetDialog?.dismiss()
                        val maskedEmail = uiState.email?.let { maskEmail(it) }
                        passwordResetSentLinkNoticeDialog =
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(getString(R.string.dialog_title_password_reset_link_sent))
                                .setMessage(
                                    getString(
                                        R.string.dialog_message_password_reset_link_sent,
                                        maskedEmail
                                    )
                                )
                                .setPositiveButton(getString(R.string.action_ok)) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setOnDismissListener {
                                    passwordResetSentLinkNoticeDialog = null
                                }
                                .create()
                        passwordResetSentLinkNoticeDialog?.show()
                        authViewModel.sendPasswordResetEmailMessageShown()
                    }

                    if (uiState.resetPasswordSuccess) {
                        newPasswordDialog?.dismiss()
                        Snackbar.make(
                            fragmentAuthBinding.root,
                            R.string.snack_bar_password_reset_success,
                            Snackbar.LENGTH_LONG
                        ).show()
                        authViewModel.resetPasswordMessageShown()
                    }

                }

            }
        }

    }

    private fun setupFieldListeners() {
        fragmentAuthBinding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                fragmentAuthBinding.emailInputLayout.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        fragmentAuthBinding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                fragmentAuthBinding.passwordInputLayout.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        fragmentAuthBinding.btnAuth.setOnClickListener {
            val email =
                fragmentAuthBinding.etEmail.text.toString().trim().replace("\\s+".toRegex(), "")
            val password =
                fragmentAuthBinding.etPassword.text.toString().trim().replace("\\s+".toRegex(), "")

            if (validateFields(email, password)) {
                if (isInternetAvailable(requireContext())) {
                    authViewModel.signInWithEmailAndPassword(email, password)
                } else {
                    Snackbar.make(
                        fragmentAuthBinding.root,
                        R.string.error_network_unavailable,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        fragmentAuthBinding.btnPasswordReset.setOnClickListener {
            showPasswordResetDialog()
        }
    }

    private fun validateFields(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            fragmentAuthBinding.emailInputLayout.error = getString(R.string.error_email_empty)
            isValid = false
        } else if (!ValidationUtil.isValidEmail(email)) {
            fragmentAuthBinding.emailInputLayout.error = getString(R.string.error_email_invalid)
            isValid = false
        }

        if (password.isEmpty()) {
            fragmentAuthBinding.passwordInputLayout.error = getString(R.string.error_password_empty)
            isValid = false
        }

        if (!isValid) {
            triggerValidationFailureVibration(requireContext())
        }

        return isValid
    }

    private fun showTotpCodeRequiredDialog() {
        if (totpCodeRequiredDialog != null) return

        val dialogTotpCodeRequiredBinding = DialogTotpCodeRequiredBinding.inflate(layoutInflater)
        totpCodeRequiredDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_title_totp_code_required))
            .setView(dialogTotpCodeRequiredBinding.root)
            .setCancelable(false)
            .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.action_confirm), null)
            .create()

        totpCodeRequiredDialog?.show()

        totpCodeRequiredDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val totpCodeInput = dialogTotpCodeRequiredBinding.etTotpCode.text.toString().trim()
                .replace("\\s+".toRegex(), "")
            var isValid = true

            if (totpCodeInput.length != 6) {
                dialogTotpCodeRequiredBinding.tilTotpCode.error =
                    getString(R.string.error_invalid_code_length)
                isValid = false
            }

            if (!isValid) {
                triggerValidationFailureVibration(requireContext())
            } else {
                authViewModel.confirmTotpAuthentication(totpCodeInput)
            }
        }

        dialogTotpCodeRequiredBinding.etTotpCode.requestFocus()
        dialogTotpCodeRequiredBinding.etTotpCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                dialogTotpCodeRequiredBinding.tilTotpCode.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val confirmTotpAuthenticationJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                authViewModel.authUiState.collect { uiState ->
                    totpCodeRequiredDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                        !uiState.isConfirmMultiFactor
                    totpCodeRequiredDialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled =
                        !uiState.isConfirmMultiFactor

                    if (uiState.isConfirmMultiFactor) {
                        dialogTotpCodeRequiredBinding.lpiTotpCode.visibility = View.VISIBLE
                    } else {
                        dialogTotpCodeRequiredBinding.lpiTotpCode.visibility = View.GONE
                    }
                }
            }
        }

        totpCodeRequiredDialog?.setOnDismissListener {
            confirmTotpAuthenticationJob.cancel()
            selectMultiFactorDialog?.dismiss()
            totpCodeRequiredDialog = null
        }
    }

    private fun showSelectMultiFactorDialog(multiFactorHints: List<MultiFactorInfo>) {
        if (selectMultiFactorDialog?.isShowing == true) {
            return
        }

        selectMultiFactorDialog = BottomSheetDialog(requireContext())
        selectMultiFactorDialog?.behavior?.isFitToContents = true
        selectMultiFactorDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        val bottomSheetSelectTwoFactorBinding =
            BottomSheetSelectTwoFactorBinding.inflate(layoutInflater)

        val adapter = TwoFactorOptionsAdapter(multiFactorHints) { selectedIndex ->
            authViewModel.handleFactorSelection(selectedIndex)
        }
        bottomSheetSelectTwoFactorBinding.rvMfaOptions.adapter = adapter
        bottomSheetSelectTwoFactorBinding.rvMfaOptions.layoutManager = LinearLayoutManager(context)

        selectMultiFactorDialog?.setOnDismissListener {
            bottomSheetSelectTwoFactorBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetSelectTwoFactorBinding.root)
            }
            authViewModel.abortMultiFactorAuthentication()
            selectMultiFactorDialog = null
        }

        selectMultiFactorDialog?.setContentView(bottomSheetSelectTwoFactorBinding.root)
        selectMultiFactorDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        selectMultiFactorDialog?.show()
    }

    private fun showPasswordResetDialog() {
        if (passwordResetDialog?.isShowing == true) {
            return
        }

        val dialogPasswordResetBinding =
            DialogPasswordResetBinding.inflate(LayoutInflater.from(requireContext()))

        dialogPasswordResetBinding.etEmailForReset.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                dialogPasswordResetBinding.emailForResetInputLayout.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        passwordResetDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title_password_reset)
            .setView(dialogPasswordResetBinding.root)
            .setCancelable(false)
            .setPositiveButton(R.string.action_send, null)
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener {
                passwordResetDialog = null
            }
            .create()

        passwordResetDialog?.show()

        passwordResetDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val email = dialogPasswordResetBinding.etEmailForReset.text.toString().trim()

            if (email.isEmpty()) {
                triggerValidationFailureVibration(requireContext())
                dialogPasswordResetBinding.emailForResetInputLayout.error =
                    getString(R.string.error_email_empty)
            } else if (!ValidationUtil.isValidEmail(email)) {
                triggerValidationFailureVibration(requireContext())
                dialogPasswordResetBinding.emailForResetInputLayout.error =
                    getString(R.string.error_email_invalid)
            } else {
                if (isInternetAvailable(requireContext())) {
                    authViewModel.sendPasswordResetEmail(email)
                } else {
                    Snackbar.make(
                        fragmentAuthBinding.root,
                        R.string.error_network_unavailable,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                authViewModel.authUiState.collect { uiState ->
                    passwordResetDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                        !uiState.isSendPasswordResetEmail
                    passwordResetDialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled =
                        !uiState.isSendPasswordResetEmail
                    if (uiState.isSendPasswordResetEmail) {
                        dialogPasswordResetBinding.passResetProgressBar.visibility = View.VISIBLE
                    } else {
                        dialogPasswordResetBinding.passResetProgressBar.visibility = View.GONE
                    }
                }
            }
        }

    }

    private fun showNewPasswordDialog(oobCode: String) {
        if (newPasswordDialog?.isShowing == true) {
            return
        }

        val dialogNewPasswordBinding =
            DialogNewPasswordBinding.inflate(LayoutInflater.from(requireContext()))

        dialogNewPasswordBinding.etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                dialogNewPasswordBinding.newPasswordInputLayout.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        newPasswordDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title_new_password)
            .setView(dialogNewPasswordBinding.root)
            .setCancelable(false)
            .setPositiveButton(R.string.action_confirm, null)
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener {
                newPasswordDialog = null
            }
            .create()

        newPasswordDialog?.show()

        newPasswordDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val newPassword = dialogNewPasswordBinding.etNewPassword.text.toString().trim()

            if (newPassword.isEmpty()) {
                triggerValidationFailureVibration(requireContext())
                dialogNewPasswordBinding.newPasswordInputLayout.error =
                    getString(R.string.error_password_empty)
            } else if (!ValidationUtil.isValidPasswordLength(newPassword)) {
                triggerValidationFailureVibration(requireContext())
                dialogNewPasswordBinding.newPasswordInputLayout.error =
                    getString(R.string.error_password_length)
            } else if (!ValidationUtil.isValidPassword(newPassword)) {
                triggerValidationFailureVibration(requireContext())
                dialogNewPasswordBinding.newPasswordInputLayout.error =
                    getString(R.string.prompt_password_invalid)
            } else {
                if (isInternetAvailable(requireContext())) {
                    authViewModel.resetPassword(oobCode, newPassword)
                } else {
                    Snackbar.make(
                        fragmentAuthBinding.root,
                        R.string.error_network_unavailable,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                authViewModel.authUiState.collect { uiState ->
                    newPasswordDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                        !uiState.isResetPassword
                    newPasswordDialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled =
                        !uiState.isResetPassword
                    if (uiState.isResetPassword) {
                        dialogNewPasswordBinding.setNewPassProgressBar.visibility = View.VISIBLE
                    } else {
                        dialogNewPasswordBinding.setNewPassProgressBar.visibility = View.GONE
                    }
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "AuthFragment"
    }
}
