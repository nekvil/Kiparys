package com.example.kiparys.ui.authregister

import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.kiparys.KiparysApplication
import com.example.kiparys.R
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.UserRepository
import com.example.kiparys.databinding.FragmentRegisterBinding
import com.example.kiparys.util.ErrorUtil
import com.example.kiparys.util.StringUtil.maskEmail
import com.example.kiparys.util.ValidationUtil
import com.example.kiparys.util.SystemUtil
import com.example.kiparys.util.SystemUtil.isInternetAvailable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlin.getValue


class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val fragmentRegisterBinding get() = _binding!!
    private var emailVerificationNoticeDialog: AlertDialog? = null
    private val registerViewModel: RegisterViewModel by viewModels {
        RegisterViewModelFactory(
            (requireContext().applicationContext as KiparysApplication).dataStoreRepository,
            AuthRepository(),
            UserRepository()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        fragmentRegisterBinding.btnRegister.setOnClickListener {
            val name =
                fragmentRegisterBinding.etName.text.toString().trim().replace("\\s+".toRegex(), " ")
            val email = fragmentRegisterBinding.etEmail.text.toString().trim()
            val password = fragmentRegisterBinding.etPassword.text.toString().trim()
                .replace("\\s+".toRegex(), "")

            if (validateFields(name, email, password)) {
                if (isInternetAvailable(requireContext())) {
                    registerViewModel.registerUser(name, email, password)
                } else {
                    Snackbar.make(
                        fragmentRegisterBinding.root,
                        getString(R.string.error_network_unavailable),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        setupFieldListeners()
        setupPrivacyPolicyText()

        return fragmentRegisterBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                registerViewModel.registerUiState.collect { uiState ->
                    uiState.error?.let { error ->
                        Snackbar.make(
                            fragmentRegisterBinding.root,
                            ErrorUtil.getErrorMessage(requireContext(), error),
                            Snackbar.LENGTH_LONG
                        ).show()
                        registerViewModel.errorMessageShown()
                    }

                    fragmentRegisterBinding.tvPrivacyLink.isClickable = !uiState.isRegister
                    fragmentRegisterBinding.tvPrivacyLink.isEnabled = !uiState.isRegister
                    fragmentRegisterBinding.btnRegister.isEnabled = !uiState.isRegister

                    if (uiState.isRegister) {
                        fragmentRegisterBinding.btnRegister.text =
                            getString(R.string.prompt_empty_string)
                        fragmentRegisterBinding.registrationProgressBar.visibility = View.VISIBLE
                    } else {
                        fragmentRegisterBinding.btnRegister.text =
                            getString(R.string.action_sign_up)
                        fragmentRegisterBinding.registrationProgressBar.visibility = View.INVISIBLE
                    }

                    if (uiState.registerSuccess) {
                        fragmentRegisterBinding.root.clearFocus()

                        fragmentRegisterBinding.etName.text?.clear()
                        fragmentRegisterBinding.etEmail.text?.clear()
                        fragmentRegisterBinding.etPassword.text?.clear()

                        val maskedEmail = uiState.email?.let { maskEmail(it) }
                        emailVerificationNoticeDialog = MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.dialog_title_verification_email_sent))
                            .setMessage(
                                getString(
                                    R.string.dialog_message_verification_register_email_sent,
                                    maskedEmail
                                )
                            )
                            .setPositiveButton(getString(R.string.action_ok)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setOnDismissListener {
                                emailVerificationNoticeDialog = null
                            }
                            .create()
                        emailVerificationNoticeDialog?.show()
                        registerViewModel.registerMessageShown()
                    }

                }
            }
        }
    }

    private fun setupFieldListeners() {
        fragmentRegisterBinding.etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                fragmentRegisterBinding.nameInputLayout.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        fragmentRegisterBinding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                fragmentRegisterBinding.emailInputLayout.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        fragmentRegisterBinding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                fragmentRegisterBinding.passwordInputLayout.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupPrivacyPolicyText() {
        val privacyText = getString(R.string.prompt_privacy_policy_agreement)
        val spannableString = SpannableString(privacyText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                findNavController().navigate(R.id.action_registerFragment_to_privacyPolicyFragment)
            }
        }

        val startIndex =
            privacyText.indexOf(getString(R.string.prompt_privacy_policy_agreement_last_part))
        val endIndex = privacyText.length

        if (startIndex >= 0) {
            spannableString.setSpan(
                clickableSpan,
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            fragmentRegisterBinding.tvPrivacyLink.text = spannableString
            fragmentRegisterBinding.tvPrivacyLink.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun validateFields(name: String, email: String, password: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            fragmentRegisterBinding.nameInputLayout.error = getString(R.string.error_name_empty)
            isValid = false
        }

        if (email.isEmpty()) {
            fragmentRegisterBinding.emailInputLayout.error = getString(R.string.error_email_empty)
            isValid = false
        } else if (!ValidationUtil.isValidEmail(email)) {
            fragmentRegisterBinding.emailInputLayout.error = getString(R.string.error_email_invalid)
            isValid = false
        }

        if (password.isEmpty()) {
            fragmentRegisterBinding.passwordInputLayout.error =
                getString(R.string.error_password_empty)
            isValid = false
        } else if (!ValidationUtil.isValidPasswordLength(password)) {
            fragmentRegisterBinding.passwordInputLayout.error =
                getString(R.string.error_password_length)
            isValid = false
        } else if (!ValidationUtil.isValidPassword(password)) {
            fragmentRegisterBinding.passwordInputLayout.error =
                getString(R.string.prompt_password_invalid)
            isValid = false
        }

        if (!isValid) {
            SystemUtil.triggerValidationFailureVibration(requireContext())
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
