package com.example.kiparys.ui.projectdetails

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.CircleCropTransformation
import com.example.kiparys.KiparysApplication
import com.example.kiparys.R
import com.example.kiparys.data.model.User
import com.example.kiparys.databinding.DialogFragmentFullscreenSearchUsersBinding
import com.example.kiparys.ui.adapter.SearchMembersAdapter
import com.example.kiparys.util.ErrorUtil
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlin.getValue


class MemberSearchDialogFragment : DialogFragment() {

    private var _binding: DialogFragmentFullscreenSearchUsersBinding? = null
    private val dialogFragmentFullscreenSearchUsersBinding get() = _binding!!
    private val projectDetailsViewModel: ProjectDetailsViewModel by viewModels({ requireParentFragment().requireParentFragment() })
    private var exitConfirmationDialog: AlertDialog? = null
    private val searchMembersAdapter = SearchMembersAdapter(onUserClick = { user ->
        if (projectDetailsViewModel.isUserSelected(user)) {
            projectDetailsViewModel.setAssignedUser(null)
        } else {
            projectDetailsViewModel.setAssignedUser(user)
        }
    })

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFragmentFullscreenSearchUsersBinding.inflate(inflater, container, false)
        return dialogFragmentFullscreenSearchUsersBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialogFragmentFullscreenSearchUsersBinding.toolbar.title =
            getString(R.string.title_assign_member)

        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                dialogFragmentFullscreenSearchUsersBinding.rvResults.adapter = null
                super.onDestroy(owner)
            }
        })

        dialogFragmentFullscreenSearchUsersBinding.rvResults.apply {
            adapter = searchMembersAdapter
            layoutManager = LinearLayoutManager(context)
        }

        dialogFragmentFullscreenSearchUsersBinding.etSearch.addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                projectDetailsViewModel.searchMembersByEmail(
                    s?.toString()?.trim()?.lowercase().toString()
                )
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        projectDetailsViewModel.searchMembersByEmail("")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                projectDetailsViewModel.projectMainScreenUiState.collect { uiState ->
                    uiState.error?.let { error ->
                        Snackbar.make(
                            requireView(),
                            ErrorUtil.getErrorMessage(
                                requireContext(),
                                error
                            ),
                            Snackbar.LENGTH_LONG
                        ).show()
                        projectDetailsViewModel.errorMessageShown()
                    }

                    uiState.taskAssignedUser?.let { updateSelectedUserChips(it) }
                    searchMembersAdapter.updateSelectedUsers(uiState.taskAssignedUser)


                    if (uiState.searchMembersByEmailSuccess) {
                        dialogFragmentFullscreenSearchUsersBinding.mtvEmptyPlaceholder.visibility =
                            if (uiState.foundMembers.isEmpty()) View.VISIBLE else View.GONE
                        searchMembersAdapter.submitList(uiState.foundMembers)
                        projectDetailsViewModel.searchMembersByEmailMessageShown()
                    }

                    dialogFragmentFullscreenSearchUsersBinding.lpiSearch.visibility =
                        if (uiState.isSearchMembersByEmail) View.VISIBLE else View.GONE

                    dialogFragmentFullscreenSearchUsersBinding.chipGroupUsers.visibility =
                        if (uiState.taskAssignedUser != null) View.VISIBLE else View.GONE

                    dialogFragmentFullscreenSearchUsersBinding.toolbar.menu.findItem(R.id.action_save).isEnabled =
                        uiState.taskAssignedUser != null

                }
            }
        }

        dialogFragmentFullscreenSearchUsersBinding.toolbar.setNavigationOnClickListener {
            if (exitConfirmationDialog == null) {
                exitConfirmationDialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.dialog_title_exit_confirmation))
                    .setMessage(getString(R.string.dialog_message_exit_confirmation))
                    .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.action_ok) { dialog, _ ->
                        projectDetailsViewModel.setAssignedUser(null)
                        dialog.dismiss()
                        dismiss()
                    }
                    .setOnDismissListener {
                        exitConfirmationDialog = null
                    }
                    .create()

                exitConfirmationDialog?.show()
            }
        }

        dialogFragmentFullscreenSearchUsersBinding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save -> {
                    dismiss()
                    true
                }

                else -> false
            }
        }
    }

    private fun updateSelectedUserChips(selectedUser: User?) {
        val chipGroup = dialogFragmentFullscreenSearchUsersBinding.chipGroupUsers
        chipGroup.removeAllViews()

        if (selectedUser != null) {
            val chip = layoutInflater.inflate(R.layout.chip_user, chipGroup, false) as Chip
            chip.text = selectedUser.email
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener { projectDetailsViewModel.setAssignedUser(null) }

            val imageLoader =
                (requireContext().applicationContext as KiparysApplication).imageLoader
            val request = ImageRequest.Builder(requireContext())
                .data(selectedUser.profileImageUrl)
                .precision(Precision.EXACT)
                .transformations(CircleCropTransformation())
                .placeholder(R.drawable.baseline_circle_24)
                .error(R.drawable.baseline_circle_24)
                .target { drawable ->
                    chip.chipIcon = drawable
                }
                .build()

            imageLoader.enqueue(request)
            chipGroup.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (parentFragment as? ProjectChatFragment)?.memberSearchDialog = null
    }

    companion object {
        const val TAG = "MemberSearchDialogFragment"

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null) {
                val dialog = MemberSearchDialogFragment()
                dialog.show(fragmentManager, TAG)
            }
        }
    }
}
