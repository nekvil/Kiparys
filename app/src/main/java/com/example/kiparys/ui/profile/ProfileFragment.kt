package com.example.kiparys.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.CircleCropTransformation
import com.example.kiparys.Constants.DATE_PICKER
import com.example.kiparys.KiparysApplication
import com.example.kiparys.R
import com.example.kiparys.data.model.User
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.UserRepository
import com.example.kiparys.databinding.BottomSheetDeleteAccountBinding
import com.example.kiparys.databinding.BottomSheetEditAboutBinding
import com.example.kiparys.databinding.BottomSheetEditBirthdateBinding
import com.example.kiparys.databinding.BottomSheetEditEmailBinding
import com.example.kiparys.databinding.BottomSheetEditFullNameBinding
import com.example.kiparys.databinding.BottomSheetEditPasswordBinding
import com.example.kiparys.databinding.BottomSheetEditProfileImageBinding
import com.example.kiparys.databinding.BottomSheetEditTwoFactorBinding
import com.example.kiparys.databinding.BottomSheetSelectTwoFactorBinding
import com.example.kiparys.databinding.BottomSheetTotpActivateBinding
import com.example.kiparys.databinding.DialogProfileImagePreviewBinding
import com.example.kiparys.databinding.DialogReAuthBinding
import com.example.kiparys.databinding.DialogTotpCodeRequiredBinding
import com.example.kiparys.databinding.FragmentProfileBinding
import com.example.kiparys.ui.adapter.TwoFactorOptionsAdapter
import com.example.kiparys.ui.main.MainViewModel
import com.example.kiparys.Constants.MAX_IMAGE_SIZE
import com.example.kiparys.util.ErrorUtil
import com.example.kiparys.util.ImageUtil.generateQrCode
import com.example.kiparys.util.ImageUtil.getBitmapFromUri
import com.example.kiparys.util.ImageUtil.getFileSizeFromUri
import com.example.kiparys.util.StringUtil.maskEmail
import com.example.kiparys.util.StringUtil.stringToTimestamp
import com.example.kiparys.util.StringUtil.timestampToString
import com.example.kiparys.util.ValidationUtil
import com.example.kiparys.util.ValidationUtil.validateBirthdate
import com.example.kiparys.util.SystemUtil.triggerSingleVibration
import com.example.kiparys.util.SystemUtil.triggerValidationFailureVibration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.TotpMultiFactorInfo
import com.google.firebase.auth.TotpSecret
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.getValue


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val fragmentProfileBinding get() = _binding!!
    private var deleteProfileImageDialog: AlertDialog? = null
    private var editBirthdateBottomSheetDialog: BottomSheetDialog? = null
    private var editProfileImageBottomSheetDialog: BottomSheetDialog? = null
    private var editAboutBottomSheetDialog: BottomSheetDialog? = null
    private var editFullNameBottomSheetDialog: BottomSheetDialog? = null
    private var editEmailBottomSheetDialog: BottomSheetDialog? = null
    private var editPasswordBottomSheetDialog: BottomSheetDialog? = null
    private var editTwoFactorBottomSheetDialog: BottomSheetDialog? = null
    private var deleteAccountBottomSheetDialog: BottomSheetDialog? = null
    private var totpActivateBottomSheetDialog: BottomSheetDialog? = null
    private var selectMultiFactorDialog: BottomSheetDialog? = null
    private var datePickerDialog: MaterialDatePicker<Long>? = null
    private lateinit var captureImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var currentPhotoPath: String
    private var totpCodeRequiredDialog: AlertDialog? = null
    private var reAuthToUnEnrollTotpDialog: AlertDialog? = null
    private var profileImagePreviewDialog: AlertDialog? = null
    private var emailVerificationNoticeDialog: AlertDialog? = null
    private var verifyAndChangeEmailSuccessDialog: AlertDialog? = null
    private var warningUsernameUpdateProcessingDialog: AlertDialog? = null
    private var warningImageUpdateProcessingDialog: AlertDialog? = null
    private var reAuthToEnrollTotpDialog: AlertDialog? = null
    private val mainViewModel: MainViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(
            AuthRepository(),
            UserRepository(),
            (requireActivity().application as KiparysApplication).dataManagementRepository
        )
    }
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val fileSizeInBytes = getFileSizeFromUri(requireContext(), uri)
                val imageBitmap = getBitmapFromUri(requireContext(), uri)
                if (imageBitmap != null) {
                    showProfileImagePreviewDialog(imageBitmap, fileSizeInBytes)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return fragmentProfileBinding.root
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File =
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val appLink = arguments?.getString("appLink")
        appLink?.let { link ->
            val uri = Uri.parse(link)
            val mode = uri.getQueryParameter("mode")
            val oobCode = uri.getQueryParameter("oobCode")

            when (mode) {
                "verifyAndChangeEmail" -> {
                    if (oobCode != null) {
                        profileViewModel.verifyAndChangeEmail(oobCode)
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

        fragmentProfileBinding.topAppBar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        captureImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val imageBitmap = BitmapFactory.decodeFile(currentPhotoPath)
                    val imageFile = File(currentPhotoPath)
                    val fileSizeInBytes = imageFile.length()
                    showProfileImagePreviewDialog(imageBitmap, fileSizeInBytes)
                }
            }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                profileViewModel.observeEnrolledFactors()
                launch {
                    mainViewModel.userData.collect { result ->
                        result?.onSuccess { user ->

                            val profileImageUrl = user.profileImageUrl
                            if (user.profileImageUrl != null) {
                                val imageLoader =
                                    (requireContext().applicationContext as KiparysApplication).imageLoader
                                val request = ImageRequest.Builder(requireContext())
                                    .placeholder(R.drawable.baseline_circle_24)
                                    .error(R.drawable.baseline_circle_24)
                                    .data(profileImageUrl)
                                    .precision(Precision.EXACT)
                                    .transformations(CircleCropTransformation())
                                    .target(fragmentProfileBinding.sivProfileImage)
                                    .build()
                                imageLoader.enqueue(request)
                            }

                            val fullName = "${user.firstName} ${user.lastName ?: ""}"
                            fragmentProfileBinding.mtvUsername.text = fullName
                            fragmentProfileBinding.mtvNameInfo.text = fullName
                            fragmentProfileBinding.mtvEmailInfo.text = user.email
                            fragmentProfileBinding.mtvAboutInfo.text =
                                user.about ?: getString(R.string.info_about)
                            fragmentProfileBinding.mtvBirthdateInfo.text =
                                timestampToString(requireContext(), user.birthdate)
                                    ?: getString(R.string.info_birthdate)

                            fragmentProfileBinding.mbEditProfileImage.setOnClickListener {
                                if (user.profileImageUpdating == true) {
                                    warningImageUpdateProcessingDialog =
                                        MaterialAlertDialogBuilder(requireContext())
                                            .setTitle(getString(R.string.dialog_title_warning))
                                            .setMessage(getString(R.string.dialog_message_warning_image_update_processing))
                                            .setPositiveButton(getString(R.string.action_ok)) { dialog, _ ->
                                                dialog.dismiss()
                                            }
                                            .setOnDismissListener {
                                                warningImageUpdateProcessingDialog = null
                                            }
                                            .create()
                                    warningImageUpdateProcessingDialog?.show()
                                } else {
                                    showEditProfileImageDialog(user)
                                }
                            }

                            fragmentProfileBinding.clEmail.setOnClickListener {
                                showEditEmailDialog(user)
                            }

                            fragmentProfileBinding.clPassword.setOnClickListener {
                                showEditPasswordDialog(user)
                            }

                            fragmentProfileBinding.clFullName.setOnClickListener {
                                if (user.nameUpdating == true) {
                                    warningUsernameUpdateProcessingDialog =
                                        MaterialAlertDialogBuilder(requireContext())
                                            .setTitle(getString(R.string.dialog_title_warning))
                                            .setMessage(getString(R.string.dialog_message_warning_username_update_processing))
                                            .setPositiveButton(getString(R.string.action_ok)) { dialog, _ ->
                                                dialog.dismiss()
                                            }
                                            .setOnDismissListener {
                                                warningUsernameUpdateProcessingDialog = null
                                            }
                                            .create()
                                    warningUsernameUpdateProcessingDialog?.show()
                                } else {
                                    showEditFullNameDialog(user)
                                }
                            }

                            fragmentProfileBinding.clAbout.setOnClickListener {
                                showEditAboutDialog(user)
                            }

                            fragmentProfileBinding.clBirthdate.setOnClickListener {
                                showEditBirthdateDialog(user)
                            }

                            launch {
                                profileViewModel.profileUiState.collect { uiState ->
                                    val enrolledFactors = uiState.enrolledFactors ?: emptyList()

                                    if (enrolledFactors.isNotEmpty()) {
                                        fragmentProfileBinding.mtvTwoFactorInfo.text =
                                            getString(R.string.info_two_factor_enabled)
                                    } else {
                                        fragmentProfileBinding.mtvTwoFactorInfo.text =
                                            getString(R.string.info_two_factor_disabled)
                                    }
                                    fragmentProfileBinding.clTwoFactor.setOnClickListener {
                                        showEditTwoFactorDialog(user, enrolledFactors)
                                    }
                                }
                            }

                            fragmentProfileBinding.mbDeleteAccount.setOnClickListener {
                                showDeleteAccountDialog(user)
                            }
                        }?.onFailure {
                            Snackbar.make(
                                fragmentProfileBinding.root,
                                R.string.error_loading_profile,
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                launch {
                    profileViewModel.profileUiState.collect { uiState ->
                        uiState.error?.let { error ->
                            datePickerDialog?.dismiss()
                            editBirthdateBottomSheetDialog?.dismiss()
                            deleteProfileImageDialog?.dismiss()
                            editAboutBottomSheetDialog?.dismiss()
                            editProfileImageBottomSheetDialog?.dismiss()
                            editFullNameBottomSheetDialog?.dismiss()
                            editEmailBottomSheetDialog?.dismiss()
                            emailVerificationNoticeDialog?.dismiss()
                            profileImagePreviewDialog?.dismiss()
                            verifyAndChangeEmailSuccessDialog?.dismiss()
                            editPasswordBottomSheetDialog?.dismiss()
                            deleteAccountBottomSheetDialog?.dismiss()
                            editTwoFactorBottomSheetDialog?.dismiss()
                            reAuthToEnrollTotpDialog?.dismiss()
                            totpActivateBottomSheetDialog?.dismiss()
                            reAuthToUnEnrollTotpDialog?.dismiss()
                            selectMultiFactorDialog?.dismiss()
                            totpCodeRequiredDialog?.dismiss()
                            Snackbar.make(
                                requireView(),
                                ErrorUtil.getErrorMessage(
                                    requireContext(),
                                    error
                                ),
                                Snackbar.LENGTH_LONG
                            ).show()
                            profileViewModel.errorMessageShown()
                        }

                        fragmentProfileBinding.mbEditProfileImage.isEnabled =
                            !uiState.isDeleteProfileImage
                        if (uiState.isDeleteProfileImage) {
                            fragmentProfileBinding.cpiProfileImage.visibility = View.VISIBLE
                        } else {
                            fragmentProfileBinding.cpiProfileImage.visibility = View.GONE
                        }

                        fragmentProfileBinding.mbEditProfileImage.isEnabled =
                            !uiState.isSaveProfileImage
                        if (uiState.isSaveProfileImage) {
                            fragmentProfileBinding.cpiProfileImage.visibility = View.VISIBLE
                        } else {
                            fragmentProfileBinding.cpiProfileImage.visibility = View.GONE
                        }

                        if (uiState.isMultiFactorRequired) {
                            showSelectMultiFactorDialog(uiState.multiFactorHints)
                            editEmailBottomSheetDialog?.dismiss()
                            editPasswordBottomSheetDialog?.dismiss()
                            deleteAccountBottomSheetDialog?.dismiss()
                            editTwoFactorBottomSheetDialog?.dismiss()
                            reAuthToEnrollTotpDialog?.dismiss()
                            totpActivateBottomSheetDialog?.dismiss()
                            reAuthToUnEnrollTotpDialog?.dismiss()
                        }

                        if (uiState.showTotpDialog) {
                            showTotpCodeRequiredDialog()
                        }

                        if (uiState.handleMultiFactorExceptionSuccess == true) {
                            totpCodeRequiredDialog?.dismiss()
                        }

                        if (uiState.verifyAndChangeEmailSuccess == true) {
                            verifyAndChangeEmailSuccessDialog =
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(getString(R.string.dialog_title_email_update_success))
                                    .setMessage(getString(R.string.dialog_message_email_update_success))
                                    .setPositiveButton(getString(R.string.action_ok)) { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .setOnDismissListener {
                                        verifyAndChangeEmailSuccessDialog = null
                                        mainViewModel.signOut()
                                    }
                                    .create()
                            verifyAndChangeEmailSuccessDialog?.show()
                            profileViewModel.verifyAndChangeEmailMessageShown()
                        }

                        if (uiState.saveFullNameSuccess) {
                            editFullNameBottomSheetDialog?.dismiss()
                            profileViewModel.saveFullNameMessageShown()
                        }

                        if (uiState.saveAboutSuccess) {
                            editAboutBottomSheetDialog?.dismiss()
                            profileViewModel.saveAboutMessageShown()
                        }

                        if (uiState.saveBirthdateSuccess) {
                            editBirthdateBottomSheetDialog?.dismiss()
                            profileViewModel.saveBirthdateMessageShown()
                        }

                        if (uiState.deleteAccountSuccess) {
                            deleteAccountBottomSheetDialog?.dismiss()
                            profileViewModel.deleteAccountMessageShown()
                        }

                        if (uiState.updatePasswordSuccess == true) {
                            editPasswordBottomSheetDialog?.dismiss()
                            Snackbar.make(
                                requireView(),
                                getString(R.string.snack_bar_password_reset_success),
                                Snackbar.LENGTH_LONG
                            ).show()
                            profileViewModel.updatePasswordMessageShown()
                        }

                        if (uiState.verifyAndUpdateUserEmailSuccess == true) {
                            editEmailBottomSheetDialog?.dismiss()
                            val maskedEmail = uiState.newEmail?.let { maskEmail(it) }
                            emailVerificationNoticeDialog =
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(getString(R.string.dialog_title_verification_email_sent))
                                    .setMessage(
                                        getString(
                                            R.string.dialog_message_verification_update_email_sent,
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
                            profileViewModel.verifyAndUpdateUserEmailMessageShown()
                        }

                        if (uiState.enrollTotpSuccess == true) {
                            totpActivateBottomSheetDialog?.dismiss()
                            Snackbar.make(
                                requireView(),
                                getString(R.string.snack_bar_totp_enroll_success),
                                Snackbar.LENGTH_LONG
                            ).show()
                            profileViewModel.enrollTotpMessageShown()
                        }

                        if (uiState.generateTotpSecretSuccess == true) {
                            reAuthToEnrollTotpDialog?.dismiss()
                            editTwoFactorBottomSheetDialog?.dismiss()
                            uiState.totpSecret?.let {
                                uiState.qrCodeUri?.let { qrCodeUri ->
                                    showTotpActivateDialog(it, qrCodeUri)
                                }
                            }
                            profileViewModel.generateTotpSecretMessageShown()
                        }

                        if (uiState.unEnrollTotpSuccess == true) {
                            Snackbar.make(
                                requireView(),
                                getString(R.string.snack_bar_totp_un_enroll_success),
                                Snackbar.LENGTH_LONG
                            ).show()
                            profileViewModel.unEnrollTotpMessageShown()
                        }

                    }
                }

            }
        }

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
                profileViewModel.confirmTotpAuthentication(totpCodeInput)
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
                profileViewModel.profileUiState.collect { uiState ->
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
            profileViewModel.handleFactorSelection(selectedIndex)
        }
        bottomSheetSelectTwoFactorBinding.rvMfaOptions.adapter = adapter
        bottomSheetSelectTwoFactorBinding.rvMfaOptions.layoutManager = LinearLayoutManager(context)

        selectMultiFactorDialog?.setOnDismissListener {
            bottomSheetSelectTwoFactorBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetSelectTwoFactorBinding.root)
            }
            profileViewModel.abortMultiFactorAuthentication()
            selectMultiFactorDialog = null
        }

        selectMultiFactorDialog?.setContentView(bottomSheetSelectTwoFactorBinding.root)
        selectMultiFactorDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        selectMultiFactorDialog?.show()
    }

    private fun showEditProfileImageDialog(user: User) {
        if (editProfileImageBottomSheetDialog?.isShowing == true) {
            return
        }

        editProfileImageBottomSheetDialog = BottomSheetDialog(requireContext())
        editProfileImageBottomSheetDialog?.behavior?.isFitToContents = true
        editProfileImageBottomSheetDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        val bottomSheetEditProfileImageBinding =
            BottomSheetEditProfileImageBinding.inflate(layoutInflater)

        user.profileImageUrl?.let { profileImageUrl ->
            if (profileImageUrl.contains("upload", ignoreCase = true)) {
                bottomSheetEditProfileImageBinding.clDeleteOption.visibility = View.VISIBLE
                bottomSheetEditProfileImageBinding.clDeleteOption.setOnClickListener {
                    if (deleteProfileImageDialog?.isShowing == true) {
                        return@setOnClickListener
                    }
                    editProfileImageBottomSheetDialog?.dismiss()
                    deleteProfileImageDialog = MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.dialog_title_profile_image_delete))
                        .setMessage(getString(R.string.dialog_message_profile_image_delete))
                        .setPositiveButton(getString(R.string.action_delete)) { dialog, _ ->
                            profileViewModel.deleteProfileImage(user.profileImageUrl)
                            dialog.dismiss()
                        }
                        .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .setOnDismissListener {
                            deleteProfileImageDialog = null
                        }
                        .create()

                    deleteProfileImageDialog?.show()
                }
            } else {
                bottomSheetEditProfileImageBinding.clDeleteOption.visibility = View.GONE
            }
        }

        bottomSheetEditProfileImageBinding.clGalleryOption.setOnClickListener {
            editProfileImageBottomSheetDialog?.dismiss()
            galleryLauncher.launch("image/*")
        }

        bottomSheetEditProfileImageBinding.clPhotoOption.setOnClickListener {
            checkCameraPermission()
        }

        editProfileImageBottomSheetDialog?.setOnDismissListener {
            bottomSheetEditProfileImageBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetEditProfileImageBinding.root)
            }
            editProfileImageBottomSheetDialog = null
        }

        editProfileImageBottomSheetDialog?.setContentView(bottomSheetEditProfileImageBinding.root)
        editProfileImageBottomSheetDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        editProfileImageBottomSheetDialog?.show()
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_camera_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.toast_camera_permission_required),
                    Toast.LENGTH_LONG
                ).show()
            }

            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val photoFile: File = createImageFile()
        val photoURI: Uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        }
        editProfileImageBottomSheetDialog?.dismiss()
        captureImageLauncher.launch(intent)
    }

    private fun showProfileImagePreviewDialog(imageBitmap: Bitmap, fileSizeInBytes: Long) {
        if (profileImagePreviewDialog != null) return

        val dialogBinding = DialogProfileImagePreviewBinding.inflate(layoutInflater)

        val imageLoader = (requireContext().applicationContext as KiparysApplication).imageLoader
        val request = ImageRequest.Builder(requireContext())
            .placeholder(R.drawable.baseline_circle_24)
            .error(R.drawable.baseline_circle_24)
            .data(imageBitmap)
            .precision(Precision.EXACT)
            .transformations(CircleCropTransformation())
            .target(dialogBinding.sivProfileImagePreview)
            .build()
        imageLoader.enqueue(request)

        profileImagePreviewDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_title_profile_image_preview))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.action_save), null)
            .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener {
                profileImagePreviewDialog = null
            }
            .create()

        profileImagePreviewDialog?.show()

        val positiveButton = profileImagePreviewDialog?.getButton(AlertDialog.BUTTON_POSITIVE)

        if (fileSizeInBytes > MAX_IMAGE_SIZE) {
            dialogBinding.mtvProfileImageSizeWarning.visibility = View.VISIBLE
            positiveButton?.isEnabled = false
        } else {
            dialogBinding.mtvProfileImageSizeWarning.visibility = View.GONE
            positiveButton?.isEnabled = true
        }

        positiveButton?.setOnClickListener {
            if (fileSizeInBytes <= MAX_IMAGE_SIZE) {
                profileViewModel.saveProfileImage(imageBitmap)
                profileImagePreviewDialog?.dismiss()
            }
        }
    }

    private fun showEditEmailDialog(user: User) {
        if (editEmailBottomSheetDialog?.isShowing == true) {
            return
        }

        editEmailBottomSheetDialog = BottomSheetDialog(requireContext())
        editEmailBottomSheetDialog?.behavior?.isFitToContents = true
        editEmailBottomSheetDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        val bottomSheetEditEmailBinding = BottomSheetEditEmailBinding.inflate(layoutInflater)

        user.email?.let { email ->
            bottomSheetEditEmailBinding.etEmail.setText(email)
            bottomSheetEditEmailBinding.etEmail.setSelection(email.length)
        }

        bottomSheetEditEmailBinding.mbEmailSave.setOnClickListener {
            val emailInput = bottomSheetEditEmailBinding.etEmail.text.toString().trim()
                .replace("\\s+".toRegex(), "")
            val currentPasswordInput =
                bottomSheetEditEmailBinding.etCurrentPassword.text.toString().trim()
                    .replace("\\s+".toRegex(), "")
            var isValid = true

            if (emailInput.isEmpty()) {
                bottomSheetEditEmailBinding.tilEmail.error = getString(R.string.error_email_empty)
                isValid = false
            } else if (!ValidationUtil.isValidEmail(emailInput)) {
                bottomSheetEditEmailBinding.tilEmail.error = getString(R.string.error_email_invalid)
                isValid = false
            }

            if (currentPasswordInput.isEmpty()) {
                bottomSheetEditEmailBinding.tilCurrentPassword.error =
                    getString(R.string.error_password_empty)
                isValid = false
            }

            if (!isValid) {
                triggerValidationFailureVibration(requireContext())
            } else profileViewModel.verifyAndUpdateUserEmail(
                emailInput,
                user.email.toString(),
                currentPasswordInput
            )
        }

        bottomSheetEditEmailBinding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bottomSheetEditEmailBinding.tilEmail.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        bottomSheetEditEmailBinding.etCurrentPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bottomSheetEditEmailBinding.tilCurrentPassword.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val verifyAndUpdateUserEmailJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                profileViewModel.profileUiState.collect { uiState ->
                    editEmailBottomSheetDialog?.setCancelable(!uiState.isVerifyAndUpdateUserEmail)
                    bottomSheetEditEmailBinding.mbEmailSave.isEnabled =
                        !uiState.isVerifyAndUpdateUserEmail

                    if (uiState.isVerifyAndUpdateUserEmail) {
                        bottomSheetEditEmailBinding.mbEmailSave.text =
                            getString(R.string.prompt_empty_string)
                        bottomSheetEditEmailBinding.cpiEmailSave.visibility = View.VISIBLE
                    } else {
                        bottomSheetEditEmailBinding.mbEmailSave.text =
                            getString(R.string.action_save)
                        bottomSheetEditEmailBinding.cpiEmailSave.visibility = View.GONE

                    }
                }
            }
        }

        editEmailBottomSheetDialog?.setOnDismissListener {
            bottomSheetEditEmailBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetEditEmailBinding.root)
            }
            verifyAndUpdateUserEmailJob.cancel()
            editEmailBottomSheetDialog = null
        }

        editEmailBottomSheetDialog?.setContentView(bottomSheetEditEmailBinding.root)
        editEmailBottomSheetDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        editEmailBottomSheetDialog?.show()
    }

    private fun showEditPasswordDialog(user: User) {
        if (editPasswordBottomSheetDialog?.isShowing == true) {
            return
        }

        editPasswordBottomSheetDialog = BottomSheetDialog(requireContext())
        editPasswordBottomSheetDialog?.behavior?.isFitToContents = true
        editPasswordBottomSheetDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        val bottomSheetEditPasswordBinding = BottomSheetEditPasswordBinding.inflate(layoutInflater)

        bottomSheetEditPasswordBinding.mbNewPasswordSave.setOnClickListener {
            val newPasswordInput =
                bottomSheetEditPasswordBinding.etNewPassword.text.toString().trim()
                    .replace("\\s+".toRegex(), "")
            val currentPasswordInput =
                bottomSheetEditPasswordBinding.etCurrentPassword.text.toString().trim()
                    .replace("\\s+".toRegex(), "")
            var isValid = true

            if (newPasswordInput.isEmpty()) {
                bottomSheetEditPasswordBinding.tilNewPassword.error =
                    getString(R.string.error_password_empty)
                isValid = false
            } else if (!ValidationUtil.isValidPasswordLength(newPasswordInput)) {
                bottomSheetEditPasswordBinding.tilNewPassword.error =
                    getString(R.string.error_password_length)
                isValid = false
            } else if (!ValidationUtil.isValidPassword(newPasswordInput)) {
                bottomSheetEditPasswordBinding.tilNewPassword.error =
                    getString(R.string.prompt_password_invalid)
                isValid = false
            }

            if (currentPasswordInput.isEmpty()) {
                bottomSheetEditPasswordBinding.tilCurrentPassword.error =
                    getString(R.string.error_password_empty)
                isValid = false
            }

            if (!isValid) {
                triggerValidationFailureVibration(requireContext())
            } else profileViewModel.updatePassword(
                newPasswordInput,
                user.email.toString(),
                currentPasswordInput
            )
        }

        bottomSheetEditPasswordBinding.etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bottomSheetEditPasswordBinding.tilNewPassword.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        bottomSheetEditPasswordBinding.etCurrentPassword.addTextChangedListener(object :
            TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bottomSheetEditPasswordBinding.tilCurrentPassword.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val updatePasswordJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                profileViewModel.profileUiState.collect { uiState ->
                    editPasswordBottomSheetDialog?.setCancelable(!uiState.isUpdatePassword)
                    bottomSheetEditPasswordBinding.mbNewPasswordSave.isEnabled =
                        !uiState.isUpdatePassword

                    if (uiState.isUpdatePassword) {
                        bottomSheetEditPasswordBinding.mbNewPasswordSave.text =
                            getString(R.string.prompt_empty_string)
                        bottomSheetEditPasswordBinding.cpiNewPasswordSave.visibility = View.VISIBLE
                    } else {
                        bottomSheetEditPasswordBinding.mbNewPasswordSave.text =
                            getString(R.string.action_save)
                        bottomSheetEditPasswordBinding.cpiNewPasswordSave.visibility = View.GONE
                    }
                }
            }
        }

        editPasswordBottomSheetDialog?.setOnDismissListener {
            bottomSheetEditPasswordBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetEditPasswordBinding.root)
            }
            updatePasswordJob.cancel()
            editPasswordBottomSheetDialog = null
        }

        editPasswordBottomSheetDialog?.setContentView(bottomSheetEditPasswordBinding.root)
        editPasswordBottomSheetDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        editPasswordBottomSheetDialog?.show()
    }

    private fun showEditFullNameDialog(user: User) {
        if (editFullNameBottomSheetDialog?.isShowing == true) {
            return
        }

        editFullNameBottomSheetDialog = BottomSheetDialog(requireContext())
        editFullNameBottomSheetDialog?.behavior?.isFitToContents = true
        editFullNameBottomSheetDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        val bottomSheetEditFullNameBinding = BottomSheetEditFullNameBinding.inflate(layoutInflater)

        user.firstName?.let { firstName ->
            bottomSheetEditFullNameBinding.etFirstName.setText(firstName)
            bottomSheetEditFullNameBinding.etFirstName.setSelection(firstName.length)
        }
        user.lastName?.let { lastName ->
            bottomSheetEditFullNameBinding.etLastName.setText(lastName)
            bottomSheetEditFullNameBinding.etLastName.setSelection(lastName.length)
        }

        bottomSheetEditFullNameBinding.mbFullNameSave.setOnClickListener {
            val firstNameInput = bottomSheetEditFullNameBinding.etFirstName.text.toString().trim()
                .replace("\\s+".toRegex(), " ")
            val lastNameInput = bottomSheetEditFullNameBinding.etLastName.text.toString().trim()
                .replace("\\s+".toRegex(), " ")
            if (firstNameInput.isEmpty()) {
                bottomSheetEditFullNameBinding.tilFirstName.error =
                    getString(R.string.error_name_empty)
                triggerValidationFailureVibration(requireContext())
            } else profileViewModel.saveFullName(
                firstNameInput,
                if (lastNameInput.isEmpty()) null else lastNameInput
            )
        }

        bottomSheetEditFullNameBinding.etFirstName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bottomSheetEditFullNameBinding.tilFirstName.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        bottomSheetEditFullNameBinding.etLastName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bottomSheetEditFullNameBinding.tilLastName.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val saveFullNameJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                profileViewModel.profileUiState.collect { uiState ->
                    editFullNameBottomSheetDialog?.setCancelable(!uiState.isSaveFullName)
                    bottomSheetEditFullNameBinding.mbFullNameSave.isEnabled =
                        !uiState.isSaveFullName

                    if (uiState.isSaveFullName) {
                        bottomSheetEditFullNameBinding.mbFullNameSave.text =
                            getString(R.string.prompt_empty_string)
                        bottomSheetEditFullNameBinding.cpiFullNameSave.visibility = View.VISIBLE
                    } else {
                        bottomSheetEditFullNameBinding.mbFullNameSave.text =
                            getString(R.string.action_save)
                        bottomSheetEditFullNameBinding.cpiFullNameSave.visibility = View.GONE
                    }
                }
            }
        }

        editFullNameBottomSheetDialog?.setOnDismissListener {
            bottomSheetEditFullNameBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetEditFullNameBinding.root)
            }
            saveFullNameJob.cancel()
            editFullNameBottomSheetDialog = null
        }

        editFullNameBottomSheetDialog?.setContentView(bottomSheetEditFullNameBinding.root)
        editFullNameBottomSheetDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        editFullNameBottomSheetDialog?.show()
    }

    private fun showEditAboutDialog(user: User) {
        if (editAboutBottomSheetDialog?.isShowing == true) {
            return
        }

        editAboutBottomSheetDialog = BottomSheetDialog(requireContext())
        editAboutBottomSheetDialog?.behavior?.isFitToContents = true
        editAboutBottomSheetDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        val bottomSheetEditAboutBinding = BottomSheetEditAboutBinding.inflate(layoutInflater)

        user.about?.let { about ->
            bottomSheetEditAboutBinding.etAbout.setText(about)
            bottomSheetEditAboutBinding.etAbout.setSelection(about.length)
        }

        bottomSheetEditAboutBinding.mbAboutSave.setOnClickListener {
            val aboutInput = bottomSheetEditAboutBinding.etAbout.text.toString().trim()
                .replace("\\s+".toRegex(), " ")
            profileViewModel.saveAbout(if (aboutInput.isEmpty()) null else aboutInput)
        }

        bottomSheetEditAboutBinding.etAbout.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bottomSheetEditAboutBinding.tilAbout.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val saveAboutJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                profileViewModel.profileUiState.collect { uiState ->
                    editAboutBottomSheetDialog?.setCancelable(!uiState.isSaveAbout)
                    bottomSheetEditAboutBinding.mbAboutSave.isEnabled = !uiState.isSaveAbout

                    if (uiState.isSaveAbout) {
                        bottomSheetEditAboutBinding.mbAboutSave.text =
                            getString(R.string.prompt_empty_string)
                        bottomSheetEditAboutBinding.cpiAboutSave.visibility = View.VISIBLE
                    } else {
                        bottomSheetEditAboutBinding.mbAboutSave.text =
                            getString(R.string.action_save)
                        bottomSheetEditAboutBinding.cpiAboutSave.visibility = View.GONE
                    }
                }
            }
        }

        editAboutBottomSheetDialog?.setOnDismissListener {
            bottomSheetEditAboutBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetEditAboutBinding.root)
            }
            saveAboutJob.cancel()
            editAboutBottomSheetDialog = null
        }

        editAboutBottomSheetDialog?.setContentView(bottomSheetEditAboutBinding.root)
        editAboutBottomSheetDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        editAboutBottomSheetDialog?.show()
    }

    private fun showEditBirthdateDialog(user: User) {
        if (editBirthdateBottomSheetDialog?.isShowing == true) {
            return
        }

        editBirthdateBottomSheetDialog = BottomSheetDialog(requireContext())
        editBirthdateBottomSheetDialog?.behavior?.isFitToContents = true
        editBirthdateBottomSheetDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        val bottomSheetEditBirthdateBinding =
            BottomSheetEditBirthdateBinding.inflate(layoutInflater)

        user.birthdate?.let { birthdate ->
            val sdf = SimpleDateFormat(getString(R.string.date_format), Locale.getDefault())
            val formattedDate = sdf.format(birthdate)
            bottomSheetEditBirthdateBinding.etBirthdate.setText(formattedDate)
            bottomSheetEditBirthdateBinding.etBirthdate.setSelection(formattedDate.length)
        }

        bottomSheetEditBirthdateBinding.tilBirthdate.setStartIconOnClickListener {
            if (datePickerDialog != null && datePickerDialog?.isAdded == true) {
                return@setStartIconOnClickListener
            }

            val calendar = Calendar.getInstance()
            val builder = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.date_picker_title_birthdate))
                .setSelection(calendar.timeInMillis)

            datePickerDialog = builder.build()
            datePickerDialog?.addOnPositiveButtonClickListener { selection ->
                selection?.let {
                    val selectedDate = Date(it)
                    val sdf = SimpleDateFormat(getString(R.string.date_format), Locale.getDefault())
                    val formattedDate = sdf.format(selectedDate)
                    bottomSheetEditBirthdateBinding.etBirthdate.setText(formattedDate)
                    bottomSheetEditBirthdateBinding.etBirthdate.setSelection(formattedDate.length)
                }
            }

            datePickerDialog?.addOnDismissListener {
                datePickerDialog = null
            }

            datePickerDialog?.show(parentFragmentManager, DATE_PICKER)
        }

        bottomSheetEditBirthdateBinding.mbBirthdateSave.setOnClickListener {
            val birthdateInput = bottomSheetEditBirthdateBinding.etBirthdate.text.toString()
            val validationError = validateBirthdate(requireContext(), birthdateInput)
            if (validationError != null) {
                triggerValidationFailureVibration(requireContext())
                bottomSheetEditBirthdateBinding.tilBirthdate.error = validationError
            } else {
                bottomSheetEditBirthdateBinding.tilBirthdate.error = null
                profileViewModel.saveBirthdate(stringToTimestamp(requireContext(), birthdateInput))
            }
        }

        bottomSheetEditBirthdateBinding.etBirthdate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bottomSheetEditBirthdateBinding.tilBirthdate.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val saveBirthdateJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                profileViewModel.profileUiState.collect { uiState ->
                    editBirthdateBottomSheetDialog?.setCancelable(!uiState.isSaveBirthdate)
                    bottomSheetEditBirthdateBinding.mbBirthdateSave.isEnabled =
                        !uiState.isSaveBirthdate

                    if (uiState.isSaveBirthdate) {
                        bottomSheetEditBirthdateBinding.mbBirthdateSave.text =
                            getString(R.string.prompt_empty_string)
                        bottomSheetEditBirthdateBinding.cpiBirthdateSave.visibility = View.VISIBLE
                    } else {
                        bottomSheetEditBirthdateBinding.mbBirthdateSave.text =
                            getString(R.string.action_save)
                        bottomSheetEditBirthdateBinding.cpiBirthdateSave.visibility = View.GONE
                    }
                }
            }
        }

        editBirthdateBottomSheetDialog?.setOnDismissListener {
            bottomSheetEditBirthdateBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetEditBirthdateBinding.root)
            }
            saveBirthdateJob.cancel()
            editBirthdateBottomSheetDialog = null
        }

        editBirthdateBottomSheetDialog?.setContentView(bottomSheetEditBirthdateBinding.root)
        editBirthdateBottomSheetDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        editBirthdateBottomSheetDialog?.show()
    }

    private fun showEditTwoFactorDialog(user: User, multiFactor: List<MultiFactorInfo>) {
        if (editTwoFactorBottomSheetDialog?.isShowing == true) {
            return
        }

        editTwoFactorBottomSheetDialog = BottomSheetDialog(requireContext())
        editTwoFactorBottomSheetDialog?.behavior?.isFitToContents = true
        editTwoFactorBottomSheetDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        val bottomSheetEditTwoFactorBinding =
            BottomSheetEditTwoFactorBinding.inflate(layoutInflater)

        val totpMultiFactorInfo =
            multiFactor.find { it is TotpMultiFactorInfo } as? TotpMultiFactorInfo
        val isTotpEnrolled = totpMultiFactorInfo != null

        bottomSheetEditTwoFactorBinding.msTotp.isChecked = isTotpEnrolled
        bottomSheetEditTwoFactorBinding.clTotp.setOnClickListener {
            triggerSingleVibration(requireContext())
            val currentChecked = bottomSheetEditTwoFactorBinding.msTotp.isChecked

            if (!currentChecked) {
                showReAuthToEnrollTotpDialog(user.email.toString())
            } else {
                totpMultiFactorInfo?.uid?.let { uid ->
                    showReAuthToUnEnrollTotpDialog(
                        user.email.toString(),
                        uid
                    )
                }
            }
        }

        editTwoFactorBottomSheetDialog?.setOnDismissListener {
            bottomSheetEditTwoFactorBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetEditTwoFactorBinding.root)
            }
            editTwoFactorBottomSheetDialog = null
        }

        editTwoFactorBottomSheetDialog?.setContentView(bottomSheetEditTwoFactorBinding.root)
        editTwoFactorBottomSheetDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        editTwoFactorBottomSheetDialog?.show()
    }

    private fun showReAuthToUnEnrollTotpDialog(email: String, uid: String) {
        if (reAuthToUnEnrollTotpDialog != null) return

        val dialogReAuthBinding = DialogReAuthBinding.inflate(layoutInflater)
        reAuthToUnEnrollTotpDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_title_re_auth))
            .setView(dialogReAuthBinding.root)
            .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.action_confirm), null)
            .create()

        reAuthToUnEnrollTotpDialog?.show()
        dialogReAuthBinding.etCurrentPassword.requestFocus()
        reAuthToUnEnrollTotpDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val currentPasswordInput = dialogReAuthBinding.etCurrentPassword.text.toString().trim()
                .replace("\\s+".toRegex(), "")
            var isValid = true

            if (currentPasswordInput.isEmpty()) {
                dialogReAuthBinding.tilCurrentPassword.error =
                    getString(R.string.error_password_empty)
                isValid = false
            }

            if (!isValid) {
                triggerValidationFailureVibration(requireContext())
            } else {
                profileViewModel.unEnrollTotp(email, currentPasswordInput, uid)
            }
        }

        val generateSecretJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                profileViewModel.profileUiState.collect { uiState ->
                    reAuthToUnEnrollTotpDialog?.setCancelable(!uiState.isUnEnrollTotp)
                    reAuthToUnEnrollTotpDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                        !uiState.isUnEnrollTotp
                    reAuthToUnEnrollTotpDialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled =
                        !uiState.isUnEnrollTotp

                    if (uiState.isUnEnrollTotp) {
                        dialogReAuthBinding.lpiReAuth.visibility = View.VISIBLE
                    } else {
                        dialogReAuthBinding.lpiReAuth.visibility = View.GONE
                    }
                }
            }
        }

        reAuthToUnEnrollTotpDialog?.setOnDismissListener {
            reAuthToUnEnrollTotpDialog = null
            generateSecretJob.cancel()
        }

    }

    private fun showReAuthToEnrollTotpDialog(email: String) {
        if (reAuthToEnrollTotpDialog != null) return

        val dialogReAuthBinding = DialogReAuthBinding.inflate(layoutInflater)
        reAuthToEnrollTotpDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_title_re_auth))
            .setView(dialogReAuthBinding.root)
            .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.action_confirm), null)
            .create()

        reAuthToEnrollTotpDialog?.show()
        dialogReAuthBinding.etCurrentPassword.requestFocus()
        reAuthToEnrollTotpDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val currentPasswordInput = dialogReAuthBinding.etCurrentPassword.text.toString().trim()
                .replace("\\s+".toRegex(), "")
            var isValid = true

            if (currentPasswordInput.isEmpty()) {
                dialogReAuthBinding.tilCurrentPassword.error =
                    getString(R.string.error_password_empty)
                isValid = false
            }

            if (!isValid) {
                triggerValidationFailureVibration(requireContext())
            } else {
                profileViewModel.generateTotpSecret(email, currentPasswordInput)
            }
        }

        val unEnrollTotpAppJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                profileViewModel.profileUiState.collect { uiState ->
                    reAuthToEnrollTotpDialog?.setCancelable(!uiState.isGenerateTotpSecret)
                    reAuthToEnrollTotpDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
                        !uiState.isGenerateTotpSecret
                    reAuthToEnrollTotpDialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled =
                        !uiState.isGenerateTotpSecret

                    if (uiState.isGenerateTotpSecret) {
                        dialogReAuthBinding.lpiReAuth.visibility = View.VISIBLE
                    } else {
                        dialogReAuthBinding.lpiReAuth.visibility = View.GONE
                    }
                }
            }
        }

        reAuthToEnrollTotpDialog?.setOnDismissListener {
            reAuthToEnrollTotpDialog = null
            unEnrollTotpAppJob.cancel()
        }

    }

    private fun showTotpActivateDialog(totpSecret: TotpSecret, qrCodeUri: String) {
        if (totpActivateBottomSheetDialog?.isShowing == true) {
            return
        }

        totpActivateBottomSheetDialog = BottomSheetDialog(requireContext())
        totpActivateBottomSheetDialog?.behavior?.isFitToContents = true
        totpActivateBottomSheetDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        val bottomSheetTotpActivateBinding = BottomSheetTotpActivateBinding.inflate(layoutInflater)
        val qrCodeBitmap = generateQrCode(qrCodeUri, 512)
        val imageLoader =
            (requireContext().applicationContext as KiparysApplication).imageLoader
        val request = ImageRequest.Builder(requireContext())
            .placeholder(R.drawable.outline_qr_code_2_24)
            .error(R.drawable.outline_qr_code_2_24)
            .data(qrCodeBitmap)
            .precision(Precision.EXACT)
            .target(bottomSheetTotpActivateBinding.sivQrCode)
            .build()
        imageLoader.enqueue(request)

        bottomSheetTotpActivateBinding.sivQrCode.setOnClickListener {
            profileViewModel.openInOtpApp(totpSecret, qrCodeUri)
        }
        bottomSheetTotpActivateBinding.mtvSecretCode.text = totpSecret.sharedSecretKey

        bottomSheetTotpActivateBinding.etVerificationCode.addTextChangedListener(object :
            TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bottomSheetTotpActivateBinding.tilVerificationCode.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        bottomSheetTotpActivateBinding.mbConfirm.setOnClickListener {
            val verificationCodeInput =
                bottomSheetTotpActivateBinding.etVerificationCode.text.toString().trim()
                    .replace("\\s+".toRegex(), "")
            var isValid = true

            if (verificationCodeInput.isEmpty()) {
                bottomSheetTotpActivateBinding.tilVerificationCode.error =
                    getString(R.string.error_empty_code)
                isValid = false
            }

            if (!isValid) {
                triggerValidationFailureVibration(requireContext())
            } else profileViewModel.enrollTotp(totpSecret, verificationCodeInput)
        }

        val enrollTotpJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                profileViewModel.profileUiState.collect { uiState ->
                    totpActivateBottomSheetDialog?.setCancelable(!uiState.isEnrollTotp)
                    bottomSheetTotpActivateBinding.mbConfirm.isEnabled = !uiState.isEnrollTotp

                    if (uiState.isEnrollTotp) {
                        bottomSheetTotpActivateBinding.mbConfirm.text =
                            getString(R.string.prompt_empty_string)
                        bottomSheetTotpActivateBinding.cpiConfirm.visibility = View.VISIBLE
                    } else {
                        bottomSheetTotpActivateBinding.mbConfirm.text =
                            getString(R.string.action_confirm)
                        bottomSheetTotpActivateBinding.cpiConfirm.visibility = View.GONE
                    }
                }
            }
        }

        totpActivateBottomSheetDialog?.setOnDismissListener {
            bottomSheetTotpActivateBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetTotpActivateBinding.root)
            }
            enrollTotpJob.cancel()
            totpActivateBottomSheetDialog = null
        }

        totpActivateBottomSheetDialog?.setContentView(bottomSheetTotpActivateBinding.root)
        totpActivateBottomSheetDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        totpActivateBottomSheetDialog?.show()
    }

    private fun showDeleteAccountDialog(user: User) {
        if (deleteAccountBottomSheetDialog?.isShowing == true) {
            return
        }

        deleteAccountBottomSheetDialog = BottomSheetDialog(requireContext())
        deleteAccountBottomSheetDialog?.behavior?.isFitToContents = true
        deleteAccountBottomSheetDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        val bottomSheetDeleteAccountBinding =
            BottomSheetDeleteAccountBinding.inflate(layoutInflater)

        bottomSheetDeleteAccountBinding.mbDeleteAccountConfirm.setOnClickListener {
            val currentPasswordInput =
                bottomSheetDeleteAccountBinding.etCurrentPassword.text.toString().trim()
                    .replace("\\s+".toRegex(), "")
            var isValid = true

            if (currentPasswordInput.isEmpty()) {
                bottomSheetDeleteAccountBinding.tilCurrentPassword.error =
                    getString(R.string.error_password_empty)
                isValid = false
            }

            if (!isValid) {
                triggerValidationFailureVibration(requireContext())
            } else profileViewModel.deleteAccount(user.email.toString(), currentPasswordInput)
        }

        bottomSheetDeleteAccountBinding.etCurrentPassword.addTextChangedListener(object :
            TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bottomSheetDeleteAccountBinding.tilCurrentPassword.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val deleteAccountJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                profileViewModel.profileUiState.collect { uiState ->
                    deleteAccountBottomSheetDialog?.setCancelable(!uiState.isDeleteAccount)
                    bottomSheetDeleteAccountBinding.mbDeleteAccountConfirm.isEnabled =
                        !uiState.isDeleteAccount

                    if (uiState.isDeleteAccount) {
                        bottomSheetDeleteAccountBinding.mbDeleteAccountConfirm.text =
                            getString(R.string.prompt_empty_string)
                        bottomSheetDeleteAccountBinding.cpiDeleteAccountConfirm.visibility =
                            View.VISIBLE
                    } else {
                        bottomSheetDeleteAccountBinding.mbDeleteAccountConfirm.text =
                            getString(R.string.action_confirm)
                        bottomSheetDeleteAccountBinding.cpiDeleteAccountConfirm.visibility =
                            View.GONE
                    }
                }
            }
        }

        deleteAccountBottomSheetDialog?.setOnDismissListener {
            bottomSheetDeleteAccountBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetDeleteAccountBinding.root)
            }
            deleteAccountJob.cancel()
            deleteAccountBottomSheetDialog = null
        }

        deleteAccountBottomSheetDialog?.setContentView(bottomSheetDeleteAccountBinding.root)
        deleteAccountBottomSheetDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        deleteAccountBottomSheetDialog?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ProfileFragment"
    }

}
