package com.example.kiparys.ui.projects

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.CircleCropTransformation
import com.example.kiparys.KiparysApplication
import com.example.kiparys.R
import com.example.kiparys.data.model.UserProject
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.ProjectRepository
import com.example.kiparys.data.repository.UserRepository
import com.example.kiparys.databinding.BottomSheetCreateProjectBinding
import com.example.kiparys.databinding.BottomSheetOptionsBinding
import com.example.kiparys.databinding.FragmentProjectsBinding
import com.example.kiparys.databinding.NavigationHeaderBinding
import com.example.kiparys.ui.MainActivity
import com.example.kiparys.ui.SearchBarUpdatable
import com.example.kiparys.ui.adapter.UserProjectOptionsAdapter
import com.example.kiparys.ui.adapter.UserProjectsAdapter
import com.example.kiparys.ui.main.MainViewModel
import com.example.kiparys.util.ErrorUtil
import com.example.kiparys.util.SystemUtil.isKeyboardVisible
import com.example.kiparys.util.SystemUtil.triggerValidationFailureVibration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.getValue


class ProjectsFragment : Fragment(), SearchBarUpdatable {

    private var _binding: FragmentProjectsBinding? = null
    private val fragmentProjectsBinding get() = _binding!!
    private val mainActivity get() = activity as? MainActivity
    private var snackBarJob: Job? = null
    private var createProjectBottomSheetDialog: BottomSheetDialog? = null
    private var userProjectOptionsDialog: BottomSheetDialog? = null
    private var leaveProjectDialog: AlertDialog? = null
    private var clearProjectDialog: AlertDialog? = null
    var userSearchDialog: UserSearchDialogFragment? = null
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var userProjectsAdapter: UserProjectsAdapter
    private val projectsViewModel: ProjectsViewModel by viewModels {
        ProjectsViewModelFactory(
            mainViewModel.userId,
            AuthRepository(),
            UserRepository(),
            ProjectRepository(),
            (requireActivity().application as KiparysApplication).messagingRepository
        )
    }
    private val imm by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -> }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectsBinding.inflate(inflater, container, false)
        return fragmentProjectsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                mainActivity?.activityMainBinding?.includeSearchViewContent?.searchView?.setupWithSearchBar(
                    fragmentProjectsBinding.searchBar
                )
            }

            override fun onPause(owner: LifecycleOwner) {
                mainActivity?.activityMainBinding?.includeSearchViewContent?.searchView?.setupWithSearchBar(
                    null
                )
            }

            override fun onDestroy(owner: LifecycleOwner) {
                fragmentProjectsBinding.rvProjects.adapter = null
                super.onDestroy(owner)
            }
        })

        fragmentProjectsBinding.searchBar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_profile) {
                mainActivity?.openProfileBottomSheet()
                true
            } else {
                false
            }
        }

        userProjectsAdapter = UserProjectsAdapter(
            mainViewModel.userId.value.toString(),
            onItemClickListener = { project ->
                mainViewModel.setProjectId(project.id)
                findNavController().navigate(R.id.action_to_projectDetailsFragment)
            },
            onItemLongClickListener = { project ->
                showUserProjectOptionsDialog(project)
            }
        ).apply {
            registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    fragmentProjectsBinding.rvProjects.layoutManager?.scrollToPosition(
                        userProjectsAdapter.itemCount - 1
                    )
                }

                override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                    super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                    fragmentProjectsBinding.rvProjects.layoutManager?.scrollToPosition(
                        userProjectsAdapter.itemCount - 1
                    )
                }
            })
            stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        fragmentProjectsBinding.rvProjects.apply {
            adapter = userProjectsAdapter
            layoutManager = LinearLayoutManager(context).apply {
                reverseLayout = true
                stackFromEnd = true
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val fab = mainActivity?.activityMainBinding?.extendedFab
                    if (dy > 0) {
                        fab?.hide()
                    } else if (dy < 0) {
                        fab?.show()
                    }
                }
            })
        }

        if (mainViewModel.userId.value != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                    }

                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                        snackBarJob = viewLifecycleOwner.lifecycleScope.launch {
                            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                                if (!mainViewModel.isNotificationPermissionDenied()) {
                                    delay(1000)
                                    mainActivity?.activityMainBinding?.extendedFab?.let { fab ->
                                        val snackBar = Snackbar.make(
                                            fragmentProjectsBinding.root,
                                            getString(R.string.notification_disabled_message),
                                            Snackbar.LENGTH_INDEFINITE
                                        ).setAction(getString(R.string.settings_button)) {
                                            val intent =
                                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                    putExtra(
                                                        Settings.EXTRA_APP_PACKAGE,
                                                        requireContext().packageName
                                                    )
                                                }
                                            startActivity(intent)
                                        }.addCallback(object : Snackbar.Callback() {
                                            override fun onDismissed(
                                                transientBottomBar: Snackbar?,
                                                event: Int
                                            ) {
                                                if (event == DISMISS_EVENT_SWIPE) {
                                                    mainViewModel.saveNotificationPermissionDenied(
                                                        true
                                                    )
                                                }
                                            }
                                        })

                                        mainActivity?.activityMainBinding?.extendedFab?.let { fab ->
                                            snackBar.setAnchorView(fab)
                                        }
                                        snackBar.show()
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.userData.collect { result ->
                    result?.onSuccess { user ->
                        val profileImageUrl = user.profileImageUrl
                        if (profileImageUrl != null) {
                            val imageLoader =
                                (requireContext().applicationContext as KiparysApplication).imageLoader
                            val request = ImageRequest.Builder(requireContext())
                                .placeholder(R.drawable.baseline_circle_24)
                                .error(R.drawable.baseline_circle_24)
                                .data(profileImageUrl)
                                .precision(Precision.EXACT)
                                .transformations(CircleCropTransformation())
                                .target { drawable ->
                                    fragmentProjectsBinding.searchBar.menu?.findItem(R.id.action_profile)?.icon =
                                        drawable
                                }
                                .build()
                            imageLoader.enqueue(request)
                        }

                        mainActivity?.activityMainBinding?.extendedFab?.setOnClickListener {
                            showCreateProjectDialog()
                        }

                        mainActivity?.activityMainBinding?.navigationRailView?.headerView?.let {
                            NavigationHeaderBinding.bind(it).fabHeaderLayout.setOnClickListener {
                                showCreateProjectDialog()
                            }
                        }

                    }?.onFailure { error ->
                        Log.e(TAG, "Failed to load profile image: ${error.message}")
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {

                launch {
                    projectsViewModel.userProjectsUiState.collect { uiState ->
                        fragmentProjectsBinding.lpiProjects.visibility =
                            if (uiState.isLoading) View.VISIBLE else View.GONE
                        userProjectsAdapter.submitList(uiState.userProjects)
                        if (!uiState.isLoading) {
                            fragmentProjectsBinding.nsvProjectsPreview.visibility =
                                if (uiState.userProjects.isNotEmpty()) View.GONE else View.VISIBLE
                            fragmentProjectsBinding.rvProjects.visibility =
                                if (uiState.userProjects.isNotEmpty()) View.VISIBLE else View.GONE

                            fragmentProjectsBinding.appBarLayout.liftOnScrollTargetViewId =
                                if (uiState.userProjects.isNotEmpty()) fragmentProjectsBinding.rvProjects.id
                                else fragmentProjectsBinding.nsvProjectsPreview.id
                        }
                    }
                }

                launch {
                    projectsViewModel.projectsUiState.collect { uiState ->

                        uiState.error?.let { error ->
                            createProjectBottomSheetDialog?.dismiss()
                            leaveProjectDialog?.dismiss()
                            val snackBar = Snackbar.make(
                                requireView(),
                                ErrorUtil.getErrorMessage(
                                    requireContext(),
                                    error
                                ),
                                Snackbar.LENGTH_LONG
                            )
                            mainActivity?.activityMainBinding?.extendedFab?.let { fab ->
                                snackBar.setAnchorView(fab)
                            }
                            snackBar.show()
                            projectsViewModel.errorMessageShown()
                        }

                        if (uiState.saveProjectSuccess) {
                            mainViewModel.setProjectId(uiState.saveProjectId)
                            createProjectBottomSheetDialog?.dismiss()
                            findNavController().navigate(R.id.action_to_projectDetailsFragment)
                            projectsViewModel.saveProjectMessageShown()
                        }

                        if (uiState.showLeaveProjectDialog) {
                            leaveProjectDialog = MaterialAlertDialogBuilder(requireContext())
                                .setTitle(
                                    getString(
                                        R.string.dialog_title_leave_project,
                                        uiState.selectedUserProject?.name
                                    )
                                )
                                .setMessage(
                                    if (!uiState.showTransferManagementMessage)
                                        getString(
                                            R.string.dialog_message_leave_project,
                                            uiState.selectedUserProject?.name
                                        )
                                    else
                                        getString(
                                            R.string.dialog_message_transfer_ownership,
                                            uiState.selectedUserProject?.name
                                        )
                                )
                                .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton(getString(R.string.action_leave)) { dialog, _ ->
                                    projectsViewModel.confirmLeaveProject()
                                    dialog.dismiss()
                                }
                                .setOnDismissListener {
                                    leaveProjectDialog = null
                                }
                                .create()
                            leaveProjectDialog?.show()
                            projectsViewModel.leaveProjectDialogShown()
                        }

                        if (uiState.deleteProjectSuccess) {
                            val snackBar = Snackbar.make(
                                requireView(),
                                getString(R.string.snack_bar_delete_project_success),
                                Snackbar.LENGTH_LONG
                            )
                            mainActivity?.activityMainBinding?.extendedFab?.let { fab ->
                                snackBar.setAnchorView(fab)
                            }
                            snackBar.show()
                            projectsViewModel.deleteProjectMessageShown()
                        }

                        if (uiState.showConfirmClearProjectDialog) {
                            clearProjectDialog = MaterialAlertDialogBuilder(requireContext())
                                .setTitle(getString(R.string.dialog_title_clear_project))
                                .setMessage(
                                    getString(
                                        R.string.dialog_message_clear_project,
                                        uiState.selectedUserProject?.name
                                    )
                                )
                                .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton(getString(R.string.action_clear)) { dialog, _ ->
                                    projectsViewModel.confirmClearProject(getString(R.string.message_clear_project))
                                    dialog.dismiss()
                                }
                                .setOnDismissListener {
                                    clearProjectDialog = null
                                }
                                .create()
                            clearProjectDialog?.show()
                            projectsViewModel.clearProjectDialogShown()
                        }

                        if (uiState.clearProjectSuccess) {
                            val snackBar = Snackbar.make(
                                requireView(),
                                getString(R.string.snack_bar_clear_project_history_success),
                                Snackbar.LENGTH_LONG
                            )
                            mainActivity?.activityMainBinding?.extendedFab?.let { fab ->
                                snackBar.setAnchorView(fab)
                            }
                            snackBar.show()
                            projectsViewModel.clearProjectMessageShown()
                        }
                    }
                }

            }
        }

    }

    private fun showCreateProjectDialog() {
        if (createProjectBottomSheetDialog?.isShowing == true) {
            return
        }

        createProjectBottomSheetDialog = BottomSheetDialog(requireContext())
        createProjectBottomSheetDialog?.behavior?.isFitToContents = true
        createProjectBottomSheetDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        val bottomSheetCreateProjectBinding =
            BottomSheetCreateProjectBinding.inflate(layoutInflater)

        bottomSheetCreateProjectBinding.mbAddTeamMember.setOnClickListener {
            bottomSheetCreateProjectBinding.root.clearFocus()
            if (isKeyboardVisible(requireActivity())) {
                imm.hideSoftInputFromWindow(bottomSheetCreateProjectBinding.root.windowToken, 0)
                bottomSheetCreateProjectBinding.root.postDelayed({
                    UserSearchDialogFragment.show(
                        childFragmentManager
                    )
                }, 300)
            } else {
                UserSearchDialogFragment.show(childFragmentManager)
            }
        }

        bottomSheetCreateProjectBinding.mbSaveProject.setOnClickListener {
            val projectNameInput =
                bottomSheetCreateProjectBinding.etProjectName.text.toString().trim()
                    .replace("\\s+".toRegex(), " ")
            val projectDescriptionInput =
                bottomSheetCreateProjectBinding.etProjectDescription.text.toString().trim()
                    .replace("\\s+".toRegex(), " ")
            if (projectNameInput.isEmpty()) {
                bottomSheetCreateProjectBinding.tilProjectName.error =
                    getString(R.string.error_project_name_empty)
                triggerValidationFailureVibration(requireContext())
            } else {
                projectsViewModel.saveProject(
                    name = projectNameInput,
                    description = if (projectDescriptionInput.isEmpty()) null else projectDescriptionInput,
                    messageToMembers = getString(
                        R.string.notification_message_to_new_member,
                        projectNameInput
                    ),
                    messageForMe = getString(R.string.message_create_new_project_to_me)
                )
            }
        }

        bottomSheetCreateProjectBinding.etProjectName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bottomSheetCreateProjectBinding.tilProjectName.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                projectsViewModel.projectsUiState.collect { uiState ->
                    createProjectBottomSheetDialog?.setCancelable(!uiState.isSaveProject)
                    bottomSheetCreateProjectBinding.mbSaveProject.isEnabled = !uiState.isSaveProject
                    bottomSheetCreateProjectBinding.mbAddTeamMember.isEnabled =
                        !uiState.isSaveProject

                    if (uiState.isSaveProject) {
                        bottomSheetCreateProjectBinding.mbSaveProject.text =
                            getString(R.string.prompt_empty_string)
                        bottomSheetCreateProjectBinding.cpiProjectSave.visibility = View.VISIBLE
                    } else {
                        bottomSheetCreateProjectBinding.mbSaveProject.text =
                            getString(R.string.action_save)
                        bottomSheetCreateProjectBinding.cpiProjectSave.visibility = View.GONE
                    }

                    bottomSheetCreateProjectBinding.chipGroupUsers.visibility =
                        if (uiState.selectedUsers.isNotEmpty()) View.VISIBLE else View.GONE

                    val chipGroup = bottomSheetCreateProjectBinding.chipGroupUsers
                    chipGroup.visibility = View.VISIBLE
                    chipGroup.removeAllViews()

                    for (user in uiState.selectedUsers) {
                        val chip =
                            layoutInflater.inflate(R.layout.chip_user, chipGroup, false) as Chip
                        chip.text = user.email
                        chip.isCloseIconVisible = true
                        chip.setOnCloseIconClickListener {
                            projectsViewModel.toggleUserSelection(
                                user
                            )
                        }

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
            }
        }

        createProjectBottomSheetDialog?.setOnDismissListener {
            bottomSheetCreateProjectBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetCreateProjectBinding.root)
            }
            projectsViewModel.clearSelectedUsers()
            createProjectBottomSheetDialog = null
        }

        createProjectBottomSheetDialog?.setContentView(bottomSheetCreateProjectBinding.root)
        createProjectBottomSheetDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        createProjectBottomSheetDialog?.show()
    }

    private fun showUserProjectOptionsDialog(project: UserProject) {
        if (userProjectOptionsDialog?.isShowing == true) {
            return
        }

        userProjectOptionsDialog = BottomSheetDialog(requireContext())
        userProjectOptionsDialog?.behavior?.isFitToContents = true
        userProjectOptionsDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        val bottomSheetSelectTwoFactorBinding = BottomSheetOptionsBinding.inflate(layoutInflater)

        val adapter = UserProjectOptionsAdapter(requireContext(), project) { selectedOption ->
            projectsViewModel.handleOptionSelection(selectedOption, project)
            userProjectOptionsDialog?.dismiss()
        }
        bottomSheetSelectTwoFactorBinding.rvOptions.adapter = adapter
        bottomSheetSelectTwoFactorBinding.rvOptions.layoutManager = LinearLayoutManager(context)

        userProjectOptionsDialog?.setOnDismissListener {
            bottomSheetSelectTwoFactorBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetSelectTwoFactorBinding.root)
            }
            userProjectOptionsDialog = null
        }

        userProjectOptionsDialog?.setContentView(bottomSheetSelectTwoFactorBinding.root)
        userProjectOptionsDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        userProjectOptionsDialog?.show()
    }

    override fun updateSearchBarText(text: String) {
        fragmentProjectsBinding.searchBar.setText(text)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snackBarJob?.cancel()
        _binding = null
    }

    companion object {
        private const val TAG = "ProjectsFragment"
    }
}
