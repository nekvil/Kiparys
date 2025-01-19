package com.example.kiparys.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.CircleCropTransformation
import com.example.kiparys.KiparysApplication
import com.example.kiparys.NavGraphAuthDirections
import com.example.kiparys.NavGraphDirections
import com.example.kiparys.R
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.UserRepository
import com.example.kiparys.databinding.ActivityMainBinding
import com.example.kiparys.databinding.BottomSheetUserProfileBinding
import com.example.kiparys.databinding.NavigationHeaderBinding
import com.example.kiparys.ui.main.MainViewModel
import com.example.kiparys.ui.main.MainViewModelFactory
import com.example.kiparys.worker.UpdateTokenWorker
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    lateinit var activityMainBinding: ActivityMainBinding
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var signOutDialog: AlertDialog? = null
    private var isUserAuthorizationChecked = false
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()
        enableEdgeToEdge()

        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        val dataStoreRepository = (applicationContext as KiparysApplication).dataStoreRepository
        val authRepository = AuthRepository()
        val userRepository = UserRepository()

        mainViewModel = ViewModelProvider(
            this,
            MainViewModelFactory(dataStoreRepository, authRepository, userRepository)
        )[MainViewModel::class.java]

        activityMainBinding.root.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (isUserAuthorizationChecked) {
                        activityMainBinding.root.viewTreeObserver.removeOnPreDrawListener(this)
                        initializeNavController()
                        handleAppLink()
                        handleDeepLink(intent)
                        return true
                    }
                    return false
                }
            }
        )

        subscribeToAuthState()
        val monthlyWorkRequest =
            PeriodicWorkRequestBuilder<UpdateTokenWorker>(730, TimeUnit.HOURS)
                .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "UpdateTokenWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            monthlyWorkRequest
        )
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun initializeNavController() {
        val navController = findNavController(R.id.nav_host_fragment)
        activityMainBinding.navigationRailView?.setupWithNavController(navController)
        activityMainBinding.bottomNavigationView?.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            Log.d(TAG, "Navigated to: ${destination.label} - ID: ${destination.id}")
            handleVisibility(destination.id, arguments)
        }
    }

    private fun handleVisibility(destinationId: Int, arguments: Bundle?) {
        val showFab = arguments?.getBoolean("showFab", false) == true
        val showSearchView = arguments?.getBoolean("showSearchView", false) == true
        val showNavigationRailView = arguments?.getBoolean("showNavigationRailView", false) == true
        val showBottomNavigationView =
            arguments?.getBoolean("showBottomNavigationView", false) == true

        activityMainBinding.includeSearchViewContent.searchView.visibility =
            if (showSearchView) View.VISIBLE else View.GONE
        activityMainBinding.navigationRailView?.visibility =
            if (showNavigationRailView) View.VISIBLE else View.GONE
        activityMainBinding.bottomNavigationView?.visibility =
            if (showBottomNavigationView) View.VISIBLE else View.GONE
        activityMainBinding.extendedFab?.visibility = if (showFab) View.VISIBLE else View.GONE
        activityMainBinding.navigationRailView?.headerView?.visibility =
            if (showFab) View.VISIBLE else View.GONE

        val headerView = activityMainBinding.navigationRailView?.headerView
        headerView?.let {
            val navigationHeaderBinding = NavigationHeaderBinding.bind(it)
            navigationHeaderBinding.fabHeaderLayout.apply {
                when (destinationId) {
                    R.id.projectsFragment -> {
                        setImageResource(R.drawable.outline_add_24)
                        contentDescription = getString(R.string.content_description_fab_projects)
                    }

                    R.id.assistantFragment -> {
                        setImageResource(R.drawable.outline_edit_24)
                        contentDescription = getString(R.string.content_description_fab_assistant)
                    }

                    R.id.meetingsFragment -> {
                        setImageResource(R.drawable.outline_group_add_24)
                        contentDescription = getString(R.string.content_description_fab_meetings)
                    }

                    else -> {
                        setOnClickListener(null)
                    }
                }
            }
        }

        activityMainBinding.extendedFab?.apply {
            when (destinationId) {
                R.id.projectsFragment -> {
                    setImageResource(R.drawable.outline_add_24)
                    contentDescription = getString(R.string.content_description_fab_projects)
                }

                R.id.assistantFragment -> {
                    setImageResource(R.drawable.outline_edit_24)
                    contentDescription = getString(R.string.content_description_fab_assistant)
                }

                R.id.meetingsFragment -> {
                    setImageResource(R.drawable.outline_group_add_24)
                    contentDescription = getString(R.string.content_description_fab_meetings)
                }

                else -> {
                    setOnClickListener(null)
                }
            }
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent == null) return
        var isIntentHandled = false
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.isUserAuthenticated.collect { isUserSignedIn ->
                    if (isUserSignedIn && !isIntentHandled) {
                        findNavController(R.id.nav_host_fragment).handleDeepLink(intent)
                        isIntentHandled = true
                        setIntent(Intent())
                    }
                }
            }
        }
    }

    private fun handleAppLink() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.isUserAuthenticated.collect { isUserSignedIn ->
                    val data: Uri? = intent?.data
                    data?.let { uri ->
                        val mode = uri.getQueryParameter("mode")
                        val navController = findNavController(R.id.nav_host_fragment)
                        if (isUserSignedIn) {
                            when (mode) {
                                "verifyAndChangeEmail" -> {
                                    val action =
                                        NavGraphDirections.actionNavGraphToProfileFragment(appLink = uri.toString())
                                    navController.navigate(action)
                                }

                                else -> {
                                    Log.e(TAG, "Unknown appLink mode for authenticated user: $mode")
                                }
                            }
                        } else {
                            when (mode) {
                                "verifyEmail", "resetPassword", "recoverEmail", "revertSecondFactorAddition" -> {
                                    val action =
                                        NavGraphAuthDirections.actionToAuthRegisterFragment(appLink = uri.toString())
                                    navController.navigate(action)
                                }

                                else -> {
                                    Log.e(TAG, "Unknown appLink mode: $mode")
                                }
                            }
                        }
                    }
                    intent?.data = null
                }
            }
        }
    }

    private fun subscribeToAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.isUserAuthenticated.collect { isUserSignedIn ->
                    val navController = findNavController(R.id.nav_host_fragment)
                    if (!isUserSignedIn) {
                        if (mainViewModel.getCurrentUser()) {
                            if (mainViewModel.isUserAuthenticatedByDataStore()) {
                                return@collect
                            }
                        } else {
                            if (navController.currentDestination?.parent?.id != R.id.nav_graph_auth) {
                                navController.navigate(R.id.action_nav_graph_to_nav_graph_auth)
                                bottomSheetDialog?.dismiss()
                            }
                        }
                    } else {
                        mainViewModel.refreshUserState()
                        uiSetup()
                    }
                    isUserAuthorizationChecked = true
                }
            }
        }
    }

    private fun uiSetup() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    mainViewModel.userOnlineState.collect { uiState -> }
                }
            }
        }

        val searchView = activityMainBinding.includeSearchViewContent.searchView
        searchView.hint = getString(R.string.prompt_search_hint)

        val editText = searchView.getEditText()
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSearchBar(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        editText.setOnEditorActionListener { _, _, _ ->
            updateSearchBar(editText.text.toString())
            searchView.hide()
            true
        }

        val navBar =
            activityMainBinding.navigationRailView ?: activityMainBinding.bottomNavigationView
        val userProjectsBadge = navBar?.getOrCreateBadge(R.id.nav_graph_projects)
        val userTasksBadge = navBar?.getOrCreateBadge(R.id.nav_graph_tasks)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userProjectsBadge?.isVisible = false
                userTasksBadge?.isVisible = false
                launch {
                    mainViewModel.userProjectsUnreadCount.collect { projectsUnreadCountResult ->
                        projectsUnreadCountResult?.onSuccess { projectsUnreadCount ->
                            userProjectsBadge?.let { badge ->
                                badge.isVisible = projectsUnreadCount > 0
                                if (projectsUnreadCount > 0) {
                                    badge.number = projectsUnreadCount
                                }
                            }
                        }?.onFailure {
                            userProjectsBadge?.isVisible = false
                        }
                    }
                }
                launch {
                    mainViewModel.userIncompleteTasksCount.collect { incompleteTasksCountResult ->
                        incompleteTasksCountResult?.onSuccess { incompleteTasksCount ->
                            userTasksBadge?.let { badge ->
                                badge.isVisible = incompleteTasksCount > 0
                                if (incompleteTasksCount > 0) {
                                    badge.number = incompleteTasksCount
                                }
                            }
                        }?.onFailure {
                            userTasksBadge?.isVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun updateSearchBar(text: String) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val updatableFragment =
            navHostFragment?.childFragmentManager?.fragments?.firstOrNull { it is SearchBarUpdatable } as? SearchBarUpdatable
        updatableFragment?.updateSearchBarText(text)
    }

    fun openProfileBottomSheet() {
        if (bottomSheetDialog?.isShowing == true) {
            return
        }

        bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetUserProfileBinding = BottomSheetUserProfileBinding.inflate(layoutInflater)

        val collectJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.userData.collect { result ->
                    result?.onSuccess { user ->

                        if (user.profileImageUrl != null) {
                            val imageLoader = (application as KiparysApplication).imageLoader
                            val request = ImageRequest.Builder(this@MainActivity)
                                .placeholder(R.drawable.baseline_circle_24)
                                .error(R.drawable.baseline_circle_24)
                                .data(user.profileImageUrl)
                                .precision(Precision.EXACT)
                                .transformations(CircleCropTransformation())
                                .target(bottomSheetUserProfileBinding.sivProfileImage)
                                .build()
                            imageLoader.enqueue(request)
                        }

                        val fullName =
                            if (!user.firstName.isNullOrBlank() || !user.lastName.isNullOrBlank()) {
                                "${user.firstName.orEmpty()} ${user.lastName.orEmpty()}".trim()
                            } else {
                                getString(R.string.prompt_unknown)
                            }

                        val email = if (!user.email.isNullOrBlank()) {
                            user.email
                        } else {
                            getString(R.string.prompt_unknown)
                        }

                        bottomSheetUserProfileBinding.mtvUserFullName.text = fullName
                        bottomSheetUserProfileBinding.mtvUserEmail.text = email
                    }?.onFailure { error ->
                        Log.e(TAG, "Failed to load profile image: ${error.message}")
                    }
                }
            }
        }

        with(bottomSheetUserProfileBinding) {
            clProfileOption.setOnClickListener {
                findNavController(R.id.nav_host_fragment).navigate(R.id.action_nav_graph_to_profile_fragment)
                bottomSheetDialog?.dismiss()
            }

            clSettingsOption.setOnClickListener {
                findNavController(R.id.nav_host_fragment).navigate(R.id.action_nav_graph_to_settings_fragment)
                bottomSheetDialog?.dismiss()
            }

            clExitOption.setOnClickListener {
                showSignOutDialog()
                bottomSheetDialog?.dismiss()
            }

            btnCopyEmail.setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("email", mtvUserEmail.text ?: "")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.toast_email_copied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        bottomSheetDialog?.behavior?.isFitToContents = true
        bottomSheetDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        bottomSheetDialog?.setOnDismissListener {
            collectJob.cancel()
            bottomSheetUserProfileBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetUserProfileBinding.root)
            }
            bottomSheetDialog = null
        }

        bottomSheetDialog?.setContentView(bottomSheetUserProfileBinding.root)
        bottomSheetDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog?.show()
    }

    private fun showSignOutDialog() {
        if (signOutDialog?.isShowing == true) {
            return
        }

        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(getString(R.string.dialog_title_sign_out))
        builder.setMessage(getString(R.string.dialog_message_sign_out))

        builder.setPositiveButton(getString(R.string.action_yes)) { dialog, _ ->
            mainViewModel.signOut()
            dialog.dismiss()
        }

        builder.setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
            dialog.dismiss()
        }

        signOutDialog = builder.create()

        signOutDialog?.setOnDismissListener {
            signOutDialog = null
        }

        signOutDialog?.show()
    }

    companion object {
        private const val TAG = "MainActivity"
    }

}
