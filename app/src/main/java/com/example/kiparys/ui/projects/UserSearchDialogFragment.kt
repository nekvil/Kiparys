package com.example.kiparys.ui.projects

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
import com.example.kiparys.ui.adapter.SearchUsersAdapter
import com.example.kiparys.util.ErrorUtil
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlin.getValue


class UserSearchDialogFragment : DialogFragment() {

    private var _binding: DialogFragmentFullscreenSearchUsersBinding? = null
    private val dialogFragmentFullscreenSearchUsersBinding get() = _binding!!
    private val projectsViewModel: ProjectsViewModel by viewModels({ requireParentFragment() })
    private var exitConfirmationDialog: AlertDialog? = null
    private val searchUsersAdapter = SearchUsersAdapter(onUserClick = { user ->
        projectsViewModel.toggleUserSelection(user)
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
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                dialogFragmentFullscreenSearchUsersBinding.rvResults.adapter = null
                super.onDestroy(owner)
            }
        })

        dialogFragmentFullscreenSearchUsersBinding.rvResults.apply {
            adapter = searchUsersAdapter
            layoutManager = LinearLayoutManager(context)
        }

        dialogFragmentFullscreenSearchUsersBinding.etSearch.addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.trim()?.lowercase()?.let { query ->
                    if (query.isEmpty()) {
                        searchUsersAdapter.submitList(emptyList())
                        dialogFragmentFullscreenSearchUsersBinding.mtvEmptyPlaceholder.visibility =
                            View.GONE
                    } else {
                        projectsViewModel.searchUsersByEmail(query)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                projectsViewModel.projectsUiState.collect { uiState ->
                    uiState.error?.let { error ->
                        Snackbar.make(
                            requireView(),
                            ErrorUtil.getErrorMessage(
                                requireContext(),
                                error
                            ),
                            Snackbar.LENGTH_LONG
                        ).show()
                        projectsViewModel.errorMessageShown()
                    }

                    updateSelectedUserChips(uiState.selectedUsers)
                    searchUsersAdapter.updateSelectedUsers(uiState.selectedUsers)

                    if (uiState.searchUsersByEmailSuccess) {
                        dialogFragmentFullscreenSearchUsersBinding.mtvEmptyPlaceholder.visibility =
                            if (uiState.foundUsers.isEmpty()) View.VISIBLE else View.GONE
                        searchUsersAdapter.submitList(uiState.foundUsers)
                        projectsViewModel.searchUsersByEmailMessageShown()
                    }

                    dialogFragmentFullscreenSearchUsersBinding.lpiSearch.visibility =
                        if (uiState.isSearchUsersByEmail) View.VISIBLE else View.GONE

                    dialogFragmentFullscreenSearchUsersBinding.chipGroupUsers.visibility =
                        if (uiState.selectedUsers.isNotEmpty()) View.VISIBLE else View.GONE

                    dialogFragmentFullscreenSearchUsersBinding.toolbar.menu.findItem(R.id.action_save).isEnabled =
                        uiState.selectedUsers.isNotEmpty()

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
                        projectsViewModel.clearSelectedUsers()
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
                    projectsViewModel.saveSelectedUsers()
                    dismiss()
                    true
                }

                else -> false
            }
        }
    }

    private fun updateSelectedUserChips(selectedUsers: List<User>) {
        val chipGroup = dialogFragmentFullscreenSearchUsersBinding.chipGroupUsers
        chipGroup.removeAllViews()

        for (user in selectedUsers) {
            val chip = layoutInflater.inflate(R.layout.chip_user, chipGroup, false) as Chip
            chip.text = user.email
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener { projectsViewModel.toggleUserSelection(user) }

            val imageLoader =
                (requireContext().applicationContext as KiparysApplication).imageLoader
            val request = ImageRequest.Builder(requireContext())
                .data(user.profileImageUrl)
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
        (parentFragment as? ProjectsFragment)?.userSearchDialog = null
    }

    companion object {
        const val TAG = "UserSearchDialogFragment"

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null) {
                val dialog = UserSearchDialogFragment()
                dialog.show(fragmentManager, TAG)
            }
        }
    }

}
