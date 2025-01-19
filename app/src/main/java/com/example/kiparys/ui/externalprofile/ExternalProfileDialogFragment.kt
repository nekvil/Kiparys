package com.example.kiparys.ui.externalprofile

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.imageLoader
import coil.request.ImageRequest
import com.example.kiparys.KiparysApplication
import com.example.kiparys.R
import com.example.kiparys.databinding.DialogFragmentExternalProfileBinding
import com.example.kiparys.ui.projectdetails.ProjectChatFragment
import com.example.kiparys.ui.projectdetails.ProjectDetailsViewModel
import com.example.kiparys.util.StringUtil.getUserLastOnlineState
import com.example.kiparys.util.StringUtil.timestampToString
import kotlinx.coroutines.launch
import kotlin.getValue


class ExternalProfileDialogFragment : DialogFragment() {

    private var _binding: DialogFragmentExternalProfileBinding? = null
    private val dialogFragmentExternalProfileBinding get() = _binding!!
    private val projectDetailsViewModel: ProjectDetailsViewModel by viewModels({ requireParentFragment().requireParentFragment() })

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFragmentExternalProfileBinding.inflate(inflater, container, false)
        return dialogFragmentExternalProfileBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                projectDetailsViewModel.memberProfileData.collect { uiState ->
                    if (!uiState.isLoading) {
                        uiState.memberData?.let { memberData ->
                            val fullName = "${memberData.firstName} ${memberData.lastName ?: ""}"
                            dialogFragmentExternalProfileBinding.ctlUserFullName.title = fullName

                            val profileImageUrl = memberData.profileImageUrl
                            if (memberData.profileImageUrl != null) {
                                val imageLoader =
                                    (requireContext().applicationContext as KiparysApplication).imageLoader
                                val request = ImageRequest.Builder(requireContext())
                                    .data(profileImageUrl)
                                    .placeholder(R.color.md_theme_surfaceContainerLow)
                                    .target(dialogFragmentExternalProfileBinding.sivProfileImage)
                                    .build()
                                imageLoader.enqueue(request)
                            }

                            val isOnline = !memberData.connections.isNullOrEmpty()

                            if (isOnline) {
                                dialogFragmentExternalProfileBinding.mtvLastOnline.setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.md_theme_primary
                                    )
                                )
                            } else {
                                dialogFragmentExternalProfileBinding.mtvLastOnline.setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.md_theme_onSurface
                                    )
                                )
                            }

                            dialogFragmentExternalProfileBinding.mtvLastOnline.text =
                                if (isOnline) {
                                    getString(R.string.label_online)
                                } else {
                                    getUserLastOnlineState(requireContext(), memberData.lastOnline)
                                }

                            memberData.email?.let {
                                dialogFragmentExternalProfileBinding.mtvEmailInfo.text = it
                            }
                            dialogFragmentExternalProfileBinding.mtvPersonalInfoSectionTitle.visibility =
                                if (memberData.about != null || memberData.birthdate != null) View.VISIBLE else View.GONE

                            memberData.about?.let {
                                dialogFragmentExternalProfileBinding.mtvAboutInfo.text = it
                            }
                            dialogFragmentExternalProfileBinding.clAbout.visibility =
                                if (memberData.about != null) View.VISIBLE else View.GONE

                            memberData.birthdate?.let {
                                dialogFragmentExternalProfileBinding.mtvBirthdateInfo.text =
                                    timestampToString(requireContext(), it)
                            }
                            dialogFragmentExternalProfileBinding.clBirthdate.visibility =
                                if (memberData.birthdate != null) View.VISIBLE else View.GONE
                        }
                    }

                }

            }

        }

        dialogFragmentExternalProfileBinding.topAppBar.setNavigationOnClickListener {
            dialog?.dismiss()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (parentFragment as? ProjectChatFragment)?.userProfileDialog = null
    }

    companion object {
        const val TAG = "ExternalProfileDialogFragment"

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null) {
                val dialog = ExternalProfileDialogFragment()
                dialog.show(fragmentManager, TAG)
            }
        }
    }

}
